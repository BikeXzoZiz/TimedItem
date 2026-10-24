package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.LocalDateTime;

/**
 * Deletes expired items and starts not-yet-started lifetimes.
 *
 * The removal step differs per context so the deleted stack can never
 * linger as a ghost item: clicks clear the clicked slot, interactions
 * clear the used hand, inventory scans null the slot, and damage rewrites
 * the armor array.
 */
public final class ExpiryListener implements Listener {

    /** PlayerInventory slot layout on 1.9+: 0-35 storage, 36-39 armor, 40 offhand. */
    private static final int ARMOR_START = 36;
    private static final int ARMOR_END = 39;

    private final TimedItemPlugin plugin;

    public ExpiryListener(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        boolean allowStart = plugin.pluginConfig().isStartDateCalculateOnClick();
        process(item, player, allowStart, () -> event.setCurrentItem(null), false);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        process(item, player, true, () -> clearHeldSlot(event), false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                scanInventory(player, true);
            }
        }, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack[] armor = inv.getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece == null || piece.getType() == Material.AIR) {
                continue;
            }
            final int index = i;
            if (process(piece, player, true, () -> armor[index] = null, true)) {
                changed = true;
            }
        }
        if (changed) {
            inv.setArmorContents(armor);
        }
    }

    /** Checks every slot of the player's inventory. Used on join and by the periodic task. */
    public void scanInventory(Player player, boolean allowStart) {
        PluginConfig config = plugin.pluginConfig();
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            final int slot = i;
            boolean armor = i >= ARMOR_START && i <= ARMOR_END;
            if (process(item, player, allowStart, () -> inv.setItem(slot, null), armor)
                    && inv.getItem(slot) != null) {
                inv.setItem(slot, inv.getItem(slot)); // write back in case the stack was a copy
            }
        }
    }

    /**
     * Handles one item: deletes it when expired, otherwise converts a
     * not-yet-started lifetime when allowed. Returns true when the item
     * state changed (deleted or converted).
     */
    private boolean process(ItemStack item, Player player, boolean allowStart, Runnable removal, boolean armor) {
        PluginConfig config = plugin.pluginConfig();
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type == TimeLore.Type.EXPIRE) {
            if (TimeLore.isExpired(item, config, LocalDateTime.now())) {
                removeExpired(item, player, removal, armor, config);
                return true;
            }
            return false;
        }
        if (type == TimeLore.Type.LIFETIME && allowStart) {
            return TimeLore.startLifetime(item, config);
        }
        return false;
    }

    private void removeExpired(ItemStack item, Player player, Runnable removal, boolean armor, PluginConfig config) {
        if (config.isSoundEnabled()) {
            player.playSound(player.getLocation(), config.getSoundName(),
                    config.getSoundVolume(), config.getSoundPitch());
        }
        removal.run();
        if (armor && config.isResetPlayerHealthIfArmor()) {
            HealthCompat.resetMaxHealth(player);
        }
        player.sendMessage(config.getPrefix() + config.getExpire());
    }

    private void clearHeldSlot(PlayerInteractEvent event) {
        PlayerInventory inv = event.getPlayer().getInventory();
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            inv.setItemInOffHand(null);
        } else {
            inv.setItemInMainHand(null);
        }
    }
}
