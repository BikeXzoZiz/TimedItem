package me.albert.timeditem;

import me.albert.timeditem.api.ItemTimerStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and writes the TimedItem tags. Since 2.2 the authoritative
 * storage on 1.14+ servers is the item's PersistentDataContainer (via
 * {@link PdcCompat}, epoch millis so clock/timezone shifts cannot
 * corrupt timers); the lore line stays in sync as the visible display
 * and remains the storage on 1.13 and for legacy items:
 *
 *   expiry line   : {DateFormat.expires_at}§s{yyyy-MM-dd HH:mm:ss}
 *   lifetime line : {DateFormat.useful_life}§s{seconds}§s{unit label}
 *   paused line   : {DateFormat.paused}§s{seconds}§s{unit label}
 *
 * 1.x wrote the lifetime line as {amount}§s{single unit}; those items
 * are still parsed. 2.x writes {total seconds}§s{combined label 1d12h}.
 */
public final class TimeLore {

    public static final String DELIMITER = "§s";
    public static final DateTimeFormatter LORE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum Type {
        /** Carries a concrete expiry date. */
        EXPIRE,
        /** Carries a lifetime that starts counting on first use. */
        LIFETIME,
        /** Carries a frozen remaining time; counts down only after resuming. */
        PAUSED,
        /** Counts down only while the owner is online (2.9+); seconds-based. */
        PLAYTIME,
        /** Counts down only while held in a hand or worn (2.9+); seconds-based. */
        HELD
    }

    /** Immutable snapshot of an item's timer, read in a single pass. */
    public static final class TimerInfo {
        private final Type type;
        private final LocalDateTime expiry; // EXPIRE only, else null
        private final long seconds;         // LIFETIME/PAUSED, else -1
        private final String line;          // display lore line

        TimerInfo(Type type, LocalDateTime expiry, long seconds, String line) {
            this.type = type;
            this.expiry = expiry;
            this.seconds = seconds;
            this.line = line;
        }

        public Type getType() {
            return type;
        }

        public LocalDateTime getExpiry() {
            return expiry;
        }

        public long getSeconds() {
            return seconds;
        }

        public String getLine() {
            return line;
        }
    }

    private TimeLore() {
    }

    /** Reads the timer of an item in one pass: PDC first, lore fallback. */
    public static TimerInfo analyze(ItemStack item, PluginConfig config) {
        if (item == null) {
            return null;
        }
        String[] pdc = PdcCompat.read(item);
        if (pdc != null) {
            Type type = parseType(pdc[0]);
            long value = parseLong(pdc[1], -1);
            if (type != null && value >= 0) {
                return infoFromValue(type, value, config);
            }
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            return null;
        }
        return analyzeLore(meta.getLore(),
                config.getExpiresAtPrefix(), config.getUsefulLifePrefix(), config.getPausedPrefix(),
                config.getPlaytimePrefix(), config.getHeldPrefix(),
                config.unitSeconds());
    }

    /** Pure lore-based analysis (no Bukkit access; used by tests too). */
    public static TimerInfo analyzeLore(List<String> lore, String expirePrefix, String lifePrefix,
                                        String pausedPrefix, String playtimePrefix,
                                        String heldPrefix, Map<String, Long> units) {
        for (String line : lore) {
            if (!line.startsWith(expirePrefix) && !line.startsWith(lifePrefix)
                    && !line.startsWith(pausedPrefix) && !line.startsWith(playtimePrefix)
                    && !line.startsWith(heldPrefix)) {
                continue;
            }
            String[] parts = line.split(DELIMITER);
            if (line.startsWith(expirePrefix)) {
                if (parts.length >= 2) {
                    try {
                        return new TimerInfo(Type.EXPIRE, LocalDateTime.parse(parts[1], LORE_FORMAT), -1L, line);
                    } catch (DateTimeParseException ignored) {
                        continue; // malformed expire line - try the remaining lines
                    }
                }
            } else if (line.startsWith(lifePrefix)) {
                return new TimerInfo(Type.LIFETIME, null, secondsFromParts(parts, units), line);
            } else if (line.startsWith(playtimePrefix)) {
                return new TimerInfo(Type.PLAYTIME, null, secondsFromParts(parts, units), line);
            } else if (line.startsWith(heldPrefix)) {
                return new TimerInfo(Type.HELD, null, secondsFromParts(parts, units), line);
            } else {
                return new TimerInfo(Type.PAUSED, null, secondsFromParts(parts, units), line);
            }
        }
        return null;
    }

    /** Public helpers kept for API compatibility - each delegates to {@link #analyze}. */

    public static Type typeOf(ItemStack item, PluginConfig config) {
        TimerInfo info = analyze(item, config);
        return info == null ? null : info.getType();
    }

    public static LocalDateTime expiryOf(ItemStack item, PluginConfig config) {
        TimerInfo info = analyze(item, config);
        return info == null || info.getType() != Type.EXPIRE ? null : info.getExpiry();
    }

    public static long lifetimeSeconds(ItemStack item, PluginConfig config) {
        return secondsOf(item, config, Type.LIFETIME);
    }

    public static long pausedSeconds(ItemStack item, PluginConfig config) {
        return secondsOf(item, config, Type.PAUSED);
    }

    public static long playtimeSeconds(ItemStack item, PluginConfig config) {
        return secondsOf(item, config, Type.PLAYTIME);
    }

    public static long heldSeconds(ItemStack item, PluginConfig config) {
        return secondsOf(item, config, Type.HELD);
    }

    public static boolean isExpired(ItemStack item, PluginConfig config, LocalDateTime now) {
        TimerInfo info = analyze(item, config);
        return info != null && info.getType() == Type.EXPIRE
                && info.getExpiry() != null && info.getExpiry().isBefore(now);
    }

    /** Returns the first lore line starting with the given prefix, or null. */
    public static String findLine(ItemStack item, String prefix) {
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

    /** Mutators - every write updates both the PDC (when available) and the lore display. */

    /** Stamps a fresh lifetime on the item, replacing any previous timer. */
    public static void applyLifetime(ItemStack item, PluginConfig config, long seconds) {
        write(item, config, Type.LIFETIME, seconds);
    }

    /** Writes a concrete expiry date {seconds} from now, replacing any previous timer. */
    public static void applyExpiry(ItemStack item, PluginConfig config, long seconds) {
        write(item, config, Type.EXPIRE, System.currentTimeMillis() + seconds * 1000L);
    }

    /** Freezes the given remaining time on the item, replacing any previous timer. */
    public static void applyPaused(ItemStack item, PluginConfig config, long seconds) {
        write(item, config, Type.PAUSED, seconds);
    }

    /** Stamps a playtime countdown (ticks only while the owner is online), replacing any previous timer. */
    public static void applyPlaytime(ItemStack item, PluginConfig config, long seconds) {
        write(item, config, Type.PLAYTIME, seconds);
    }

    /** Stamps a held-time countdown (ticks only while held or worn), replacing any previous timer. */
    public static void applyHeld(ItemStack item, PluginConfig config, long seconds) {
        write(item, config, Type.HELD, seconds);
    }

    /**
     * Converts a lifetime into a concrete expiry date starting now and
     * fires {@link ItemTimerStartEvent}. Returns false when the item
     * carries no parsable lifetime.
     */
    public static boolean startLifetime(ItemStack item, PluginConfig config) {
        return startLifetime(item, config, null);
    }

    public static boolean startLifetime(ItemStack item, PluginConfig config, Player player) {
        TimerInfo info = analyze(item, config);
        if (info == null || info.getType() != Type.LIFETIME || info.getSeconds() <= 0) {
            return false;
        }
        write(item, config, Type.EXPIRE, System.currentTimeMillis() + info.getSeconds() * 1000L);
        Bukkit.getPluginManager().callEvent(new ItemTimerStartEvent(item, player));
        return true;
    }

    /** Removes any timer from the item. */
    public static void removeTimer(ItemStack item, PluginConfig config) {
        write(item, config, null, -1L);
    }

    /** True when the item's material is on the blacklist (Settings.blacklist.materials). */
    public static boolean isBlacklisted(ItemStack item, PluginConfig config) {
        return item != null && config.blacklistedMaterials().contains(item.getType());
    }

    /** Human readable duration using the configured unit labels, e.g. "1d12h30m". */
    public static String formatDuration(long seconds, PluginConfig config) {
        return formatDuration(seconds, config.getUnitD(), config.getUnitH(), config.getUnitM(), config.getUnitS());
    }

    /** Pure duration formatter (no Bukkit access; used by tests too). */
    public static String formatDuration(long seconds, String unitD, String unitH, String unitM, String unitS) {
        long days = seconds / 86400;
        long rem = seconds % 86400;
        long hours = rem / 3600;
        rem %= 3600;
        long minutes = rem / 60;
        long secs = rem % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(unitD);
        }
        if (hours > 0) {
            sb.append(hours).append(unitH);
        }
        if (minutes > 0) {
            sb.append(minutes).append(unitM);
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append(unitS);
        }
        return sb.toString();
    }

    private static TimerInfo infoFromValue(Type type, long value, PluginConfig config) {
        if (type == Type.EXPIRE) {
            LocalDateTime expiry = LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
            String line = config.getExpiresAtPrefix() + DELIMITER + LORE_FORMAT.format(expiry);
            return new TimerInfo(Type.EXPIRE, expiry, -1L, line);
        }
        String prefix = prefixOf(type, config);
        String line = prefix + DELIMITER + value + DELIMITER + formatDuration(value, config);
        return new TimerInfo(type, null, value, line);
    }

    /** The lore prefix of a seconds-based timer type. */
    private static String prefixOf(Type type, PluginConfig config) {
        switch (type) {
            case LIFETIME:
                return config.getUsefulLifePrefix();
            case PAUSED:
                return config.getPausedPrefix();
            case PLAYTIME:
                return config.getPlaytimePrefix();
            case HELD:
                return config.getHeldPrefix();
            default:
                return config.getExpiresAtPrefix();
        }
    }

    private static long secondsOf(ItemStack item, PluginConfig config, Type expected) {
        TimerInfo info = analyze(item, config);
        if (info == null || info.getType() != expected) {
            return -1L;
        }
        return info.getSeconds();
    }

    private static long secondsFromParts(String[] parts, Map<String, Long> units) {
        if (parts.length < 2) {
            return -1L;
        }
        long amount = parseLong(parts[1], -1L);
        String label = parts.length >= 3 ? parts[2] : "";
        long parsed = DurationParser.parse(label, units);
        if (parsed > 0) {
            return parsed; // 2.x combined label such as "1d12h"
        }
        Long multiplier = units.get(label.toLowerCase(Locale.ROOT));
        if (multiplier != null && amount >= 0) {
            return amount * multiplier; // 1.x single-unit label such as "m"
        }
        return amount >= 0 ? amount : -1L; // 2.x fallback: the amount field holds seconds
    }

    private static void write(ItemStack item, PluginConfig config, Type type, long value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> kept = new ArrayList<String>();
        if (meta.hasLore() && meta.getLore() != null) {
            for (String entry : meta.getLore()) {
                if (isTimerLine(entry, config)) {
                    continue;
                }
                kept.add(entry);
            }
        }
        if (type != null) {
            kept.add(buildLine(type, value, config));
        }
        meta.setLore(kept);
        item.setItemMeta(meta);
        if (type == null) {
            PdcCompat.clear(item);
        } else {
            PdcCompat.write(item, type.name().toLowerCase(Locale.ROOT), value);
        }
    }

    /** True when the lore line belongs to any TimedItem timer type. */
    private static boolean isTimerLine(String line, PluginConfig config) {
        return line.startsWith(config.getExpiresAtPrefix())
                || line.startsWith(config.getUsefulLifePrefix())
                || line.startsWith(config.getPausedPrefix())
                || line.startsWith(config.getPlaytimePrefix())
                || line.startsWith(config.getHeldPrefix());
    }

    private static String buildLine(Type type, long value, PluginConfig config) {
        if (type == Type.EXPIRE) {
            LocalDateTime expiry = LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
            return config.getExpiresAtPrefix() + DELIMITER + LORE_FORMAT.format(expiry);
        }
        String prefix = prefixOf(type, config);
        return prefix + DELIMITER + value + DELIMITER + formatDuration(value, config);
    }

    private static Type parseType(String name) {
        if ("expire".equals(name)) {
            return Type.EXPIRE;
        }
        if ("lifetime".equals(name)) {
            return Type.LIFETIME;
        }
        if ("paused".equals(name)) {
            return Type.PAUSED;
        }
        if ("playtime".equals(name)) {
            return Type.PLAYTIME;
        }
        if ("held".equals(name)) {
            return Type.HELD;
        }
        return null;
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
