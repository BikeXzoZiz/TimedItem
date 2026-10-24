package me.albert.timeditem;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and writes the TimedItem lore tags. The lore format is unchanged
 * from 1.x so items already stamped by the old plugin keep working:
 *
 *   expiry line   : {DateFormat.expires_at}§s{yyyy-MM-dd HH:mm:ss}
 *   lifetime line : {DateFormat.useful_life}§s{seconds}§s{unit label}
 *
 * 1.x wrote the lifetime line as {amount}§s{single unit}; those items are
 * still parsed. 2.0 writes {total seconds}§s{combined label such as 1d12h}.
 */
public final class TimeLore {

    public static final String DELIMITER = "§s";
    public static final DateTimeFormatter LORE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum Type {
        /** Carries a concrete expiry date. */
        EXPIRE,
        /** Carries a lifetime that starts counting on first use. */
        LIFETIME
    }

    private TimeLore() {
    }

    /** Returns the timer type tagged on this item, or null when it is not timed. */
    public static Type typeOf(ItemStack item, PluginConfig config) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }
        for (String line : lore) {
            if (line.startsWith(config.getExpiresAtPrefix())) {
                return Type.EXPIRE;
            }
            if (line.startsWith(config.getUsefulLifePrefix())) {
                return Type.LIFETIME;
            }
        }
        return null;
    }

    /** Returns the absolute expiry instant, or null when the tag is absent or unparsable. */
    public static LocalDateTime expiryOf(ItemStack item, PluginConfig config) {
        String line = findLine(item, config.getExpiresAtPrefix());
        if (line == null) {
            return null;
        }
        String[] parts = line.split(DELIMITER);
        if (parts.length < 2) {
            return null;
        }
        try {
            return LocalDateTime.parse(parts[1], LORE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /** Returns the lifetime in seconds, or -1 when the tag is absent or unparsable. */
    public static long lifetimeSeconds(ItemStack item, PluginConfig config) {
        String line = findLine(item, config.getUsefulLifePrefix());
        if (line == null) {
            return -1;
        }
        String[] parts = line.split(DELIMITER);
        if (parts.length < 2) {
            return -1;
        }
        long amount = parseLong(parts[1], -1);
        String label = parts.length >= 3 ? parts[2] : "";
        Map<String, Long> units = config.unitSeconds();
        long parsed = DurationParser.parse(label, units);
        if (parsed > 0) {
            return parsed; // 2.0 combined label such as "1d12h"
        }
        Long multiplier = units.get(label.toLowerCase(Locale.ROOT));
        if (multiplier != null && amount >= 0) {
            return amount * multiplier; // 1.x single-unit label such as "m"
        }
        return amount >= 0 ? amount : -1; // 2.0 fallback: the amount field holds seconds
    }

    /** Returns true when the item carries an expiry date that has already passed. */
    public static boolean isExpired(ItemStack item, PluginConfig config, LocalDateTime now) {
        LocalDateTime expiry = expiryOf(item, config);
        return expiry != null && expiry.isBefore(now);
    }

    /** Stamps a fresh lifetime on the item, replacing any previous timer. */
    public static void applyLifetime(ItemStack item, PluginConfig config, long seconds) {
        String label = formatDuration(seconds, config);
        String line = config.getUsefulLifePrefix() + DELIMITER + seconds + DELIMITER + label;
        setTimerLine(item, config, line);
    }

    /**
     * Converts a lifetime into a concrete expiry date starting now.
     * Returns false when the item carries no parsable lifetime.
     */
    public static boolean startLifetime(ItemStack item, PluginConfig config) {
        long seconds = lifetimeSeconds(item, config);
        if (seconds <= 0) {
            return false;
        }
        String line = config.getExpiresAtPrefix()
                + DELIMITER + LORE_FORMAT.format(LocalDateTime.now().plusSeconds(seconds));
        setTimerLine(item, config, line);
        return true;
    }

    /** Removes any timer line from the item. */
    public static void removeTimer(ItemStack item, PluginConfig config) {
        setTimerLine(item, config, null);
    }

    /** Human readable duration using the configured unit labels, e.g. "1d12h30m". */
    public static String formatDuration(long seconds, PluginConfig config) {
        long days = seconds / 86400;
        long rem = seconds % 86400;
        long hours = rem / 3600;
        rem %= 3600;
        long minutes = rem / 60;
        long secs = rem % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(config.getUnitD());
        }
        if (hours > 0) {
            sb.append(hours).append(config.getUnitH());
        }
        if (minutes > 0) {
            sb.append(minutes).append(config.getUnitM());
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append(config.getUnitS());
        }
        return sb.toString();
    }

    private static String findLine(ItemStack item, String prefix) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }
        for (String line : lore) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        return null;
    }

    /** Rewrites the lore keeping everything except timer lines, then appends the given line (if any). */
    private static void setTimerLine(ItemStack item, PluginConfig config, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> kept = new ArrayList<String>();
        if (meta.hasLore() && meta.getLore() != null) {
            String expirePrefix = config.getExpiresAtPrefix();
            String lifePrefix = config.getUsefulLifePrefix();
            for (String entry : meta.getLore()) {
                if (!entry.startsWith(expirePrefix) && !entry.startsWith(lifePrefix)) {
                    kept.add(entry);
                }
            }
        }
        if (line != null) {
            kept.add(line);
        }
        meta.setLore(kept);
        item.setItemMeta(meta);
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
