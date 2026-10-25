package me.albert.timeditem;

import me.albert.timeditem.api.ItemExpireEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
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
 * stands in every loaded world.
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE) {
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
                if (isExpiredGroundItem(stack, config, now)
                        && expireGround(entity, stack, config)) {
                    entity.remove();
                }
            }
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack stack = frame.getItem();
                if (stack.getType() == Material.AIR) {
                    continue;
                }
                if (isExpiredGroundItem(stack, config, now)
                        && expireGround(frame, stack, config)) {
                    frame.setItem(new ItemStack(Material.AIR));
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
                            || !isExpiredGroundItem(piece, config, now)
                            || !expireGround(stand, piece, config)) {
                        continue;
                    }
                    armor[i] = null;
                    changed = true;
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

    /** Fires the cancellable event and removes the item, then plays effects for nearby players. */
    private boolean expireGround(Entity entity, ItemStack stack, PluginConfig config) {
        ItemExpireEvent event = new ItemExpireEvent(stack, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        plugin.recoveryStore().record(stack, null, entity.getWorld());
        Location center = entity.getLocation();
        Player nearby = ExpiryListener.nearestPlayer(center, 20.0);
        // remove first so the sound/particle reads as the item vanishing
        if (config.isSoundEnabled() && nearby != null) {
            nearby.playSound(nearby.getLocation(), config.getSoundName(),
                    config.getSoundVolume(), config.getSoundPitch());
        }
        if (config.isParticleEnabled() && nearby != null) {
            ParticleCompat.spawnAt(nearby, center, config.getParticleName(), config.getParticleCount());
        }
        for (String command : config.getExpiryCommands()) {
            if (command.contains("{player}")) {
                continue; // ground expiry has no player to substitute
            }
            String cmd = command.startsWith("/") ? command.substring(1) : command;
            if (!cmd.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
        return true;
    }
}
