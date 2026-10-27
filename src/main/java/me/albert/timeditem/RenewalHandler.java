package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Renewal token (2.8+): right-click with the token in one hand and a
 * timed item in the other extends the timer by the configured time
 * and consumes one token.
 */
final class RenewalHandler {

    private final TimedItemPlugin plugin;
    private final NoticeCooldown notices;

    RenewalHandler(TimedItemPlugin plugin, NoticeCooldown notices) {
        this.plugin = plugin;
        this.notices = notices;
    }

    /** Handles the event when a renewal applies; true = consumed. */
    boolean handle(PlayerInteractEvent event) {
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
        if (!notices.tryAcquire(player.getUniqueId())) {
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
        if (type == TimeLore.Type.PLAYTIME) {
            long seconds = TimeLore.playtimeSeconds(item, config);
            if (seconds <= 0) {
                return false;
            }
            TimeLore.applyPlaytime(item, config, seconds + delta);
            return true;
        }
        if (type == TimeLore.Type.HELD) {
            long seconds = TimeLore.heldSeconds(item, config);
            if (seconds <= 0) {
                return false;
            }
            TimeLore.applyHeld(item, config, seconds + delta);
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
}
