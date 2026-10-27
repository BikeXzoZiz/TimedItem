package me.albert.timeditem;

import org.bukkit.GameMode;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<UUID, Set<String>> warned = new ConcurrentHashMap<UUID, Set<String>>();
    private final NoticeCooldown noticeCooldown = new NoticeCooldown();
    private final RenewalHandler renewal;
    private final GraceHandler grace;

    public ExpiryListener(TimedItemPlugin plugin) {
        this.plugin = plugin;
        this.renewal = new RenewalHandler(plugin, noticeCooldown);
        this.grace = new GraceHandler(plugin, noticeCooldown);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (isIgnored(player)) {
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
            SchedulerCompat.runAtPlayer(plugin, player, () -> {
                if (player.isOnline()) {
                    scanInventory(player, allowStart);
                }
            });
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isIgnored(player)) {
            return;
        }
        if (renewal.handle(event)) {
            return;
        }
        if (grace.handle(event)) {
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
        SchedulerCompat.runAtPlayerLater(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            scanInventory(player, true);
            // Restore reminder: local mode counts the in-memory log;
            // network mode counts the shared database (async) and also
            // covers items that expired on other servers.
            if (plugin.isNetworkRestoreActive()) {
                final java.util.UUID uuid = player.getUniqueId();
                SchedulerCompat.runAsync(plugin, () -> {
                    int waiting = plugin.storage().countRecoveryFor(uuid.toString());
                    if (waiting > 0) {
                        sendRestoreReminder(player, waiting);
                    }
                });
            } else if (player.hasPermission("timeditem.admin")) {
                int waiting = plugin.recoveryStore().countForPlayer(
                        player.getUniqueId().toString());
                if (waiting > 0) {
                    sendRestoreReminder(player, waiting);
                }
            }
        }, 1L);
    }

    private void sendRestoreReminder(Player player, int waiting) {
        SchedulerCompat.runAtPlayer(plugin, player, () -> {
            if (player.isOnline()) {
                player.sendMessage(plugin.pluginConfig().getPrefix()
                        + plugin.pluginConfig().getRestoreReminder()
                        .replace("{count}", String.valueOf(waiting)));
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        warned.remove(uuid);
        noticeCooldown.clear(uuid);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (isIgnored(player)) {
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
        if (isIgnored(player)) {
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
            recordTracked(player, i, item, info, config);
            VisualStages.apply(plugin, item, info, config);
            final int slot = i;
            boolean armor = i >= ARMOR_START && i <= ARMOR_END;
            handleInfo(info, item, player, allowStart,
                    () -> inv.setItem(slot, null), armor, config, now);
            if (inv.getItem(slot) != null) {
                // write back unconditionally: on platforms that hand out
                // copies, the visual stage and lifetime mutations only
                // persist through this call
                inv.setItem(slot, item);
            }
            // timed items parked inside a shulker box in the player's inventory
            plugin.expiryService().expireInsideShulker(item, player,
                    player.getLocation(), player.getWorld(), config);
            warnIfNeeded(player, info, config, now);
        }
    }

    /** Feeds the /ti admin overview (one entry per item per scan cycle). */
    private void recordTracked(Player player, int slot, ItemStack item,
                               TimeLore.TimerInfo info, PluginConfig config) {
        String custom = plugin.hooks() == null ? null : plugin.hooks().itemId(item);
        String material = item.getType().name();
        long remaining;
        if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            remaining = Math.max(0L,
                    Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds());
        } else {
            remaining = Math.max(0L, info.getSeconds());
        }
        plugin.trackedItems().record(player, slot, material,
                custom == null ? material : custom,
                info.getType().name().toLowerCase(java.util.Locale.ROOT), remaining);
    }

    /**
     * Counts the seconds-based timers down (2.9+): PLAYTIME items lose
     * <interval> seconds per scan while the owner is online, HELD items
     * only while in a hand or worn. Called once per periodic scan (never
     * from click paths), so an item is decremented exactly once per
     * interval; expiry goes through the shared pipeline.
     */
    public void tickCountdownTimers(Player player, PluginConfig config) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        int heldSlot = inv.getHeldItemSlot();
        long delta = config.getExpireCheckSeconds();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            TimeLore.TimerInfo info = TimeLore.analyze(item, config);
            if (info == null) {
                continue;
            }
            if (info.getType() == TimeLore.Type.HELD) {
                // main hand (the selected hotbar slot), offhand (40) and armor (36-39)
                if (!isHeldSlot(i, heldSlot)) {
                    continue;
                }
            } else if (info.getType() != TimeLore.Type.PLAYTIME) {
                continue;
            }
            long remaining = info.getSeconds();
            if (remaining <= 0) {
                continue;
            }
            final int slot = i;
            boolean armor = i >= ARMOR_START && i <= ARMOR_END;
            if (remaining - delta <= 0) {
                expireNow(item, player, () -> inv.setItem(slot, null), armor, config);
            } else {
                if (info.getType() == TimeLore.Type.PLAYTIME) {
                    TimeLore.applyPlaytime(item, config, remaining - delta);
                } else {
                    TimeLore.applyHeld(item, config, remaining - delta);
                }
                if (inv.getItem(slot) != null) {
                    inv.setItem(slot, item); // write back in case the stack was a copy
                }
            }
        }
    }

    /** True when the inventory slot is a hand or armor slot (worn/wielded positions). */
    private boolean isHeldSlot(int slot, int heldSlot) {
        return slot == heldSlot || slot == 40 || (slot >= ARMOR_START && slot <= ARMOR_END);
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
     * Expiry entry point for every player-bound context, delegating to
     * the shared {@link ExpiryService} pipeline (also used for ground
     * expiry, so both behave identically).
     */
    public boolean expireNow(ItemStack item, Player player, Runnable removal, boolean armor,
                             PluginConfig config) {
        return plugin.expiryService().expire(item, player,
                player == null ? null : player.getLocation(),
                player == null ? null : player.getWorld(), removal, armor, config);
    }

    /** True when this player is skipped entirely (creative exemption). */
    private boolean isIgnored(Player player) {
        return plugin.pluginConfig().isIgnoreCreative()
                && player.getGameMode() == GameMode.CREATIVE;
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
