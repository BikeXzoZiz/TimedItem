package me.albert.timeditem.api;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Public facade for other plugins to manage timed items. All methods
 * share the exact semantics of the plugin's own commands, including the
 * PDC storage on 1.14+ servers and the lore fallback.
 *
 * Thread safety: call from the main thread only (Bukkit item access).
 */
public final class TimedItemAPI {

    private static volatile TimedItemPlugin plugin;

    private TimedItemAPI() {
    }

    /** Called by TimedItemPlugin on enable - not part of the public API. */
    public static void init(TimedItemPlugin instance) {
        plugin = instance;
    }

    private static PluginConfig config() {
        TimedItemPlugin pl = plugin;
        if (pl == null || !pl.isEnabled()) {
            throw new IllegalStateException("TimedItem is not enabled");
        }
        return pl.pluginConfig();
    }

    /** Returns true when the item carries any timer (expiry, lifetime or paused). */
    public static boolean isTimed(ItemStack item) {
        return TimeLore.analyze(item, config()) != null;
    }

    /**
     * Returns "expire" (counting down to a date), "lifetime" (starts on
     * first use), "paused" (frozen), or null when the item has no timer.
     */
    public static String getType(ItemStack item) {
        TimeLore.TimerInfo info = TimeLore.analyze(item, config());
        return info == null ? null : info.getType().name().toLowerCase(Locale.ROOT);
    }

    /**
     * Remaining seconds: for expired-type items the time until expiry,
     * for lifetime/paused items the stored duration. Null when untimed
     * or the timer data is unreadable.
     */
    public static Long getRemainingSeconds(ItemStack item) {
        TimeLore.TimerInfo info = TimeLore.analyze(item, config());
        if (info == null) {
            return null;
        }
        if (info.getType() == TimeLore.Type.EXPIRE) {
            if (info.getExpiry() == null) {
                return null;
            }
            long remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
            return Math.max(0L, remaining);
        }
        long seconds = info.getSeconds();
        return seconds >= 0 ? seconds : null;
    }

    /**
     * Stamps a lifetime on the item (starts counting on first use, per config).
     *
     * @throws IllegalArgumentException when seconds is not positive or the
     *         item's material is blacklisted (Settings.blacklist.materials)
     */
    public static void setLifetime(ItemStack item, long seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("seconds must be positive");
        }
        if (TimeLore.isBlacklisted(item, config())) {
            throw new IllegalArgumentException("material is blacklisted: " + item.getType());
        }
        TimeLore.applyLifetime(item, config(), seconds);
    }

    /**
     * Stamps a concrete expiry date {seconds} from now, replacing any timer.
     *
     * @throws IllegalArgumentException when seconds is not positive or the
     *         item's material is blacklisted (Settings.blacklist.materials)
     */
    public static void setExpiryFromNow(ItemStack item, long seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("seconds must be positive");
        }
        if (TimeLore.isBlacklisted(item, config())) {
            throw new IllegalArgumentException("material is blacklisted: " + item.getType());
        }
        TimeLore.applyExpiry(item, config(), seconds);
    }

    /** Freezes a counting item; returns false when it is not counting down. */
    public static boolean pause(ItemStack item) {
        TimeLore.TimerInfo info = TimeLore.analyze(item, config());
        if (info == null || info.getType() != TimeLore.Type.EXPIRE || info.getExpiry() == null) {
            return false;
        }
        long remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
        if (remaining <= 0) {
            return false;
        }
        TimeLore.applyPaused(item, config(), remaining);
        return true;
    }

    /** Resumes a paused item; returns false when it is not paused. */
    public static boolean resume(ItemStack item) {
        TimeLore.TimerInfo info = TimeLore.analyze(item, config());
        if (info == null || info.getType() != TimeLore.Type.PAUSED || info.getSeconds() <= 0) {
            return false;
        }
        TimeLore.applyExpiry(item, config(), info.getSeconds());
        return true;
    }

    /** Removes any timer from the item. */
    public static void clear(ItemStack item) {
        TimeLore.removeTimer(item, config());
    }
}
