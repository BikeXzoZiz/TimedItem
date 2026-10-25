package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;

/**
 * Periodically scans every online player's inventory for expired items,
 * so items are removed even if the player never clicks, uses, or takes
 * damage while holding them. Optionally (Settings.expire_check.
 * scan_world_items) also scans ground items, item frames and armor
 * stands in every loaded world. All expiry runs go through the shared
 * {@link ExpiryService}.
 */
public final class ExpiryCheckTask implements Runnable {

    private final TimedItemPlugin plugin;
    private final ExpiryListener listener;

    public ExpiryCheckTask(TimedItemPlugin plugin, ExpiryListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public void run() {
        PluginConfig config = plugin.pluginConfig();
        if (SchedulerCompat.isFolia()) {
            // Touching inventories is only legal on the owning region, so
            // enqueue one entity-scheduler run per player. Ground items are
            // handled by FoliaGroundExpiry (per-entity tracking) instead.
            for (Player player : Bukkit.getOnlinePlayers()) {
                SchedulerCompat.runAtPlayer(plugin, player, () -> {
                    if (player.isOnline()
                            && !(config.isIgnoreCreative()
                                    && player.getGameMode() == GameMode.CREATIVE)) {
                        listener.scanInventory(player, false);
                    }
                });
            }
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (config.isIgnoreCreative() && player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }
            listener.scanInventory(player, false);
        }
        if (config.isScanWorldItems()) {
            scanWorlds(config);
        }
    }

    private void scanWorlds(PluginConfig config) {
        LocalDateTime now = LocalDateTime.now();
        for (World world : Bukkit.getWorlds()) {
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                ItemStack stack = entity.getItemStack();
                if (isExpiredGroundItem(stack, config, now)) {
                    plugin.expiryService().expire(stack, null, entity.getLocation(),
                            entity.getWorld(), entity::remove, false, config);
                }
            }
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack stack = frame.getItem();
                if (stack.getType() == Material.AIR) {
                    continue;
                }
                if (isExpiredGroundItem(stack, config, now)) {
                    plugin.expiryService().expire(stack, null, frame.getLocation(),
                            frame.getWorld(),
                            () -> frame.setItem(new ItemStack(Material.AIR)), false, config);
                }
            }
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                EntityEquipment equipment = stand.getEquipment();
                if (equipment == null) {
                    continue;
                }
                ItemStack[] armor = equipment.getArmorContents();
                boolean changed = false;
                for (int i = 0; i < armor.length; i++) {
                    ItemStack piece = armor[i];
                    if (piece == null || piece.getType() == Material.AIR
                            || !isExpiredGroundItem(piece, config, now)) {
                        continue;
                    }
                    final int index = i;
                    if (plugin.expiryService().expire(piece, null, stand.getLocation(),
                            stand.getWorld(), () -> armor[index] = null, false, config)) {
                        changed = true;
                    }
                }
                if (changed) {
                    equipment.setArmorContents(armor);
                }
            }
        }
    }

    private boolean isExpiredGroundItem(ItemStack stack, PluginConfig config, LocalDateTime now) {
        TimeLore.TimerInfo info = TimeLore.analyze(stack, config);
        return info != null && info.getType() == TimeLore.Type.EXPIRE
                && info.getExpiry() != null && info.getExpiry().isBefore(now);
    }
}
