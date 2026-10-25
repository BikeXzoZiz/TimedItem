package me.albert.timeditem;

import me.albert.timeditem.api.ItemExpireEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Deletes expired items and starts not-yet-started lifetimes.
 *
 * The removal step differs per context so the deleted stack can never
 * linger as a ghost item: clicks clear the clicked slot, interactions
 * clear the used hand, inventory scans null the slot, and damage rewrites
 * the armor array. Items on the ground are removed when picked up.
 */
public final class ExpiryListener implements Listener {

    /** PlayerInventory slot layout on 1.9+: 0-35 storage, 36-39 armor, 40 offhand. */
    private static final int ARMOR_START = 36;
    private static final int ARMOR_END = 39;

    private final TimedItemPlugin plugin;
    /** Player UUID -> timer lines already warned about (one warning per item). */
    private final Map<UUID, Set<String>> warned = new HashMap<UUID, Set<String>>();

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
        boolean allowStart = plugin.pluginConfig().isStartDateCalculateOnClick();
        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() != Material.AIR) {
            process(item, player, allowStart, () -> event.setCurrentItem(null), false);
        }
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            // The item leaving the hotbar lands in the clicked slot only
            // after the event applies, so re-check the inventory next tick.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    scanInventory(player, allowStart);
                }
            });
        }
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        warned.remove(event.getPlayer().getUniqueId());
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

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        PluginConfig config = plugin.pluginConfig();
        TimeLore.TimerInfo info = TimeLore.analyze(stack, config);
        if (info == null || info.getType() != TimeLore.Type.EXPIRE
                || info.getExpiry() == null || !info.getExpiry().isBefore(LocalDateTime.now())) {
            return;
        }
        event.setCancelled(true);
        expireNow(stack, player, () -> event.getItem().remove(), false, config);
    }

    /** Checks every slot of the player's inventory. Used on join and by the periodic task. */
    public void scanInventory(Player player, boolean allowStart) {
        PluginConfig config = plugin.pluginConfig();
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            TimeLore.TimerInfo info = TimeLore.analyze(item, config);
            if (info == null) {
                continue;
            }
            final int slot = i;
            boolean armor = i >= ARMOR_START && i <= ARMOR_END;
            boolean changed = handleInfo(info, item, player, allowStart,
                    () -> inv.setItem(slot, null), armor, config, now);
            if (changed && inv.getItem(slot) != null) {
                inv.setItem(slot, inv.getItem(slot)); // write back in case the stack was a copy
            }
            warnIfNeeded(player, info, config, now);
        }
    }

    /** Message + sound when an item crosses one of its warning stages. */
    private void warnIfNeeded(Player player, TimeLore.TimerInfo info, PluginConfig config, LocalDateTime now) {
        if (!config.isWarningEnabled() || info.getType() != TimeLore.Type.EXPIRE || info.getExpiry() == null) {
            return;
        }
        long remaining = Duration.between(now, info.getExpiry()).getSeconds();
        if (remaining <= 0) {
            return;
        }
        Set<String> playerWarned = warned.get(player.getUniqueId());
        if (playerWarned == null) {
            playerWarned = new HashSet<String>();
            warned.put(player.getUniqueId(), playerWarned);
        }
        int stage = Warnings.nextStage(remaining, config.getWarningStages(), playerWarned, info.getLine());
        if (stage < 0) {
            return; // not inside any stage window, or already warned about it
        }
        playerWarned.add(Warnings.key(info.getLine(), stage));
        player.sendMessage(config.getPrefix()
                + config.getExpireSoon().replace("{time}", TimeLore.formatDuration(remaining, config)));
        player.playSound(player.getLocation(), config.getWarningSound(),
                config.getWarningVolume(), config.getWarningPitch());
    }

    /**
     * Handles one pre-analyzed item: deletes it when expired, otherwise
     * converts a not-yet-started lifetime when allowed. Returns true when
     * the item state changed (deleted or converted).
     */
    private boolean handleInfo(TimeLore.TimerInfo info, ItemStack item, Player player, boolean allowStart,
                               Runnable removal, boolean armor, PluginConfig config, LocalDateTime now) {
        if (info.getType() == TimeLore.Type.EXPIRE) {
            if (info.getExpiry() != null && info.getExpiry().isBefore(now)) {
                return expireNow(item, player, removal, armor, config);
            }
            return false;
        }
        if (info.getType() == TimeLore.Type.LIFETIME && allowStart) {
            return TimeLore.startLifetime(item, config, player);
        }
        return false;
    }

    /** Analyzes and handles one item (single-meta-read entry point for events). */
    private boolean process(ItemStack item, Player player, boolean allowStart, Runnable removal, boolean armor) {
        PluginConfig config = plugin.pluginConfig();
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null) {
            return false;
        }
        return handleInfo(info, item, player, allowStart, removal, armor, config, LocalDateTime.now());
    }

    /**
     * Shared expiry routine for every player-bound context. Fires the
     * cancellable {@link ItemExpireEvent} first; when cancelled nothing
     * happens and false is returned. Records the item in the recovery
     * log before removal.
     */
    public boolean expireNow(ItemStack item, Player player, Runnable removal, boolean armor,
                             PluginConfig config) {
        ItemExpireEvent event = new ItemExpireEvent(item, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        plugin.recoveryStore().record(item, player, player == null ? null : player.getWorld());
        removal.run();
        if (player == null) {
            return true;
        }
        if (config.isSoundEnabled()) {
            player.playSound(player.getLocation(), config.getSoundName(),
                    config.getSoundVolume(), config.getSoundPitch());
        }
        if (config.isParticleEnabled()) {
            ParticleCompat.spawn(player, config.getParticleName(), config.getParticleCount());
        }
        if (armor && config.isResetPlayerHealthIfArmor()) {
            HealthCompat.resetMaxHealth(player);
        }
        player.sendMessage(config.getPrefix() + config.getExpire());
        for (String command : config.getExpiryCommands()) {
            String cmd = command.replace("{player}", player.getName());
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            if (!cmd.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
        return true;
    }

    /** Nearest player within the radius of the location, or null. */
    public static Player nearestPlayer(Location center, double radius) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player player : center.getWorld().getPlayers()) {
            if (!player.getWorld().equals(center.getWorld())) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(center);
            if (distance <= radius * radius && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
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
