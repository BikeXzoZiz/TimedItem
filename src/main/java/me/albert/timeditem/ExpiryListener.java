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

    /** Minimum ms between two renewal or grace messages of one player. */
    private static final long MESSAGE_COOLDOWN_MILLIS = 250L;

    private final TimedItemPlugin plugin;
    /** Player UUID -> timer lines already warned about (one warning per item). */
    private final Map<UUID, Set<String>> warned = new ConcurrentHashMap<UUID, Set<String>>();
    /** Player UUID -> last grace/renewal message, so one right-click fires it once. */
    private final Map<UUID, Long> lastNotice = new ConcurrentHashMap<UUID, Long>();

    public ExpiryListener(TimedItemPlugin plugin) {
        this.plugin = plugin;
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
        if (handleRenewal(event)) {
            return;
        }
        if (handleGrace(event)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        process(item, player, true, () -> clearHeldSlot(event), false);
    }

    /**
     * Renewal token (2.8+): right-click with the token in one hand and a
     * timed item in the other extends the timer by the configured time
     * and consumes one token.
     */
    private boolean handleRenewal(PlayerInteractEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.isRenewalEnabled()) {
            return false;
        }
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        ItemStack main = inv.getItemInMainHand();
        ItemStack off = inv.getItemInOffHand();
        ItemStack token;
        ItemStack timed;
        if (isToken(main, config) && isTimed(off, config)) {
            token = main;
            timed = off;
        } else if (isToken(off, config) && isTimed(main, config)) {
            token = off;
            timed = main;
        } else {
            return false;
        }
        if (!noticeCooldownOver(player)) {
            event.setCancelled(true);
            return true;
        }
        long delta = config.getRenewalAddSeconds();
        if (!addTime(timed, delta, config)) {
            return false;
        }
        consume(token, main, off, inv);
        long remaining = remainingSeconds(timed, config);
        player.sendMessage(config.getPrefix() + config.getRenewalSuccess()
                .replace("{time}", TimeLore.formatDuration(remaining, config)));
        player.playSound(player.getLocation(), config.getWarningSound(),
                config.getWarningVolume(), config.getWarningPitch());
        plugin.storage().appendAudit(new me.albert.timeditem.storage.AuditRecord(
                System.currentTimeMillis(), player.getUniqueId().toString(), player.getName(),
                me.albert.timeditem.storage.AuditRecord.ADD, player.getName(),
                PdcCompat.readUid(timed),
                plugin.hooks().itemId(timed) == null ? timed.getType().name()
                        : plugin.hooks().itemId(timed),
                "renewal token +" + TimeLore.formatDuration(delta, config)));
        event.setCancelled(true);
        return true;
    }

    /**
     * Grace period (2.8+): in the final seconds the item can no longer
     * be used, but it still only disappears when the timer hits zero.
     */
    private boolean handleGrace(PlayerInteractEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.isGraceEnabled()) {
            return false;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null || info.getType() != TimeLore.Type.EXPIRE || info.getExpiry() == null) {
            return false;
        }
        long remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
        if (remaining <= 0 || remaining > config.getGraceSeconds()) {
            return false;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (noticeCooldownOver(player)) {
            player.sendMessage(config.getPrefix() + config.getGraceMessage()
                    .replace("{time}", TimeLore.formatDuration(remaining, config)));
            player.playSound(player.getLocation(), config.getWarningSound(),
                    config.getWarningVolume(), config.getWarningPitch());
        }
        return true;
    }

    private boolean isTimed(ItemStack item, PluginConfig config) {
        return item != null && item.getType() != Material.AIR
                && TimeLore.analyze(item, config) != null;
    }

    /** True when the stack matches the configured renewal token. */
    private boolean isToken(ItemStack item, PluginConfig config) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        String custom = config.getRenewalTokenCustomId();
        if (!custom.isEmpty()) {
            String id = plugin.hooks() == null ? null : plugin.hooks().itemId(item);
            return id != null && id.equalsIgnoreCase(custom) && tokenNameMatches(item, config);
        }
        if (config.getRenewalTokenMaterial() == null
                || item.getType() != config.getRenewalTokenMaterial()) {
            return false;
        }
        return tokenNameMatches(item, config);
    }

    private boolean tokenNameMatches(ItemStack item, PluginConfig config) {
        String required = config.getRenewalTokenName();
        if (required.isEmpty()) {
            return true;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String name = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        return name != null && name.toLowerCase(java.util.Locale.ROOT).equals(required);
    }

    /** Adds time to any timer kind, mirroring /ti add's behaviour. */
    private boolean addTime(ItemStack item, long delta, PluginConfig config) {
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type == TimeLore.Type.EXPIRE) {
            LocalDateTime expiry = TimeLore.expiryOf(item, config);
            if (expiry == null) {
                return false;
            }
            long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
            TimeLore.applyExpiry(item, config, Math.max(0, remaining) + delta);
            return true;
        }
        if (type == TimeLore.Type.LIFETIME) {
            long seconds = TimeLore.lifetimeSeconds(item, config);
            if (seconds <= 0) {
                return false;
            }
            TimeLore.applyLifetime(item, config, seconds + delta);
            return true;
        }
        if (type == TimeLore.Type.PAUSED) {
            long seconds = TimeLore.pausedSeconds(item, config);
            if (seconds <= 0) {
                return false;
            }
            TimeLore.applyPaused(item, config, seconds + delta);
            return true;
        }
        return false;
    }

    private long remainingSeconds(ItemStack item, PluginConfig config) {
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null) {
            return 0;
        }
        if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            return Math.max(0, Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds());
        }
        if (info.getType() == TimeLore.Type.PAUSED) {
            return Math.max(0, info.getSeconds());
        }
        if (info.getType() == TimeLore.Type.LIFETIME) {
            return Math.max(0, info.getSeconds());
        }
        return 0;
    }

    /** Removes one token from the hand that holds it (identity compare). */
    private void consume(ItemStack token, ItemStack main, ItemStack off, PlayerInventory inv) {
        if (token == main) {
            if (token.getAmount() <= 1) {
                inv.setItemInMainHand(null);
            } else {
                token.setAmount(token.getAmount() - 1);
                inv.setItemInMainHand(token);
            }
        } else {
            if (token.getAmount() <= 1) {
                inv.setItemInOffHand(null);
            } else {
                token.setAmount(token.getAmount() - 1);
                inv.setItemInOffHand(token);
            }
        }
    }

    private boolean noticeCooldownOver(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastNotice.get(player.getUniqueId());
        if (last != null && now - last < MESSAGE_COOLDOWN_MILLIS) {
            return false;
        }
        lastNotice.put(player.getUniqueId(), now);
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SchedulerCompat.runAtPlayerLater(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            scanInventory(player, true);
            // Restore reminder for admins: items of this player sit in the
            // recovery log and can be handed back with /ti restore.
            if (player.hasPermission("timeditem.admin")) {
                int waiting = plugin.recoveryStore().countForPlayer(
                        player.getUniqueId().toString());
                if (waiting > 0) {
                    player.sendMessage(plugin.pluginConfig().getPrefix()
                            + plugin.pluginConfig().getRestoreReminder()
                            .replace("{count}", String.valueOf(waiting)));
                }
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
