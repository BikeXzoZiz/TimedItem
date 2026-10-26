package me.albert.timeditem;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Container features (3.0+):
 *
 * - Container scanning, "open" mode: when a player opens a chest, barrel,
 *   shulker box, furnace and so on, the contents are checked for expired
 *   items (cheap - only containers someone actually opens). The periodic
 *   variant lives in {@link ContainerScanTask}.
 * - Anti-hide: items close to expiry can no longer be moved INTO a
 *   container, so players cannot park expiring items in a chest that the
 *   scanner never sees. Applies to deposits (cursor place / shift move).
 */
public final class ContainerGuard implements Listener {

    private final TimedItemPlugin plugin;

    public ContainerGuard(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    // InventoryView changed from an abstract class to an interface in 1.21,
    // so code compiled against the older API crashes with
    // IncompatibleClassChangeError on direct view calls. These reflective
    // accessors keep the compiled bytecode free of either layout.
    private static final Method TOP_INVENTORY = viewAccessor("getTopInventory");
    private static final Method BOTTOM_INVENTORY = viewAccessor("getBottomInventory");

    private static Method viewAccessor(String name) {
        try {
            return Class.forName("org.bukkit.inventory.InventoryView").getMethod(name);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Inventory viewInventory(Object view, Method accessor) {
        if (accessor == null) {
            return null;
        }
        try {
            return (Inventory) accessor.invoke(view);
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    /** True when the open-mode container scan should run for this event. */
    private boolean openScanEnabled(PluginConfig config) {
        String mode = config.getContainerScanMode();
        return config.isContainerScanEnabled()
                && (mode.equals("open") || mode.equals("both"));
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!openScanEnabled(config)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (config.isIgnoreCreative() && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Inventory top = viewInventory(event.getView(), TOP_INVENTORY);
        if (top == null || !(top.getHolder() instanceof org.bukkit.block.Container)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int slot = 0; slot < top.getSize(); slot++) {
            ItemStack item = top.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            TimeLore.TimerInfo info = TimeLore.analyze(item, config);
            if (info == null || info.getType() != TimeLore.Type.EXPIRE
                    || info.getExpiry() == null || !info.getExpiry().isBefore(now)) {
                // the item itself may not be due, but its shulker contents can be
                plugin.expiryService().expireInsideShulker(item, player,
                        player.getLocation(), player.getWorld(), config);
                continue;
            }
            final int index = slot;
            plugin.expiryService().expire(item, player, player.getLocation(),
                    player.getWorld(), () -> top.setItem(index, null), false, config);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.isAntiHideEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (config.isIgnoreCreative() && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Object view = event.getView();
        Inventory top = viewInventory(view, TOP_INVENTORY);
        if (top == null || !(top.getHolder() instanceof org.bukkit.block.Container)) {
            return; // no real container open
        }
        long threshold = config.getAntiHideBelowSeconds();
        // depositing with the cursor
        ItemStack cursor = event.getCursor();
        if (isBelowThreshold(cursor, config, threshold)) {
            InventoryAction action = event.getAction();
            if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE
                    || action == InventoryAction.SWAP_WITH_CURSOR) {
                block(player, config, cursor);
                event.setCancelled(true);
                return;
            }
        }
        // shift-clicking from the player's own inventory into the container
        Inventory bottom = viewInventory(view, BOTTOM_INVENTORY);
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && bottom != null && event.getClickedInventory() == bottom
                && isBelowThreshold(event.getCurrentItem(), config, threshold)) {
            block(player, config, event.getCurrentItem());
            event.setCancelled(true);
        }
    }

    private void block(Player player, PluginConfig config, ItemStack item) {
        long remaining = remainingOf(item, config);
        player.sendMessage(config.getPrefix() + config.getAntiHideBlocked()
                .replace("{time}", TimeLore.formatDuration(Math.max(0L, remaining), config)));
        player.playSound(player.getLocation(), config.getWarningSound(),
                config.getWarningVolume(), config.getWarningPitch());
    }

    /** True when the stack carries an EXPIRE timer with less than the threshold left. */
    private boolean isBelowThreshold(ItemStack item, PluginConfig config, long threshold) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        // a shulker whose contents hold an about-to-expire item counts too,
        // otherwise the anti-hide block is bypassed by parking items inside one
        if (Shulkers.containsBelowThreshold(item, config, threshold)) {
            return true;
        }
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null || info.getType() != TimeLore.Type.EXPIRE || info.getExpiry() == null) {
            return false;
        }
        long remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
        return remaining <= threshold;
    }

    private long remainingOf(ItemStack item, PluginConfig config) {
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null) {
            return 0;
        }
        if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            return Math.max(0L, Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds());
        }
        return Math.max(0L, info.getSeconds());
    }
}
