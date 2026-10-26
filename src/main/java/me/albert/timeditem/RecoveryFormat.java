package me.albert.timeditem;

import me.albert.timeditem.storage.RecoveryRecord;

import java.util.UUID;

/**
 * Line format of recovery.log, kept free of Bukkit types so it can be
 * unit tested without a server.
 *
 * v3 (2.7+): id|playerUuid|playerName|worldName|format|millis|base64
 * v2:        millis|playerName|worldName|format|base64
 * v1:        millis|playerName|worldName|base64
 *
 * v3 prepends the entry id and the player UUID (empty for ground expiry
 * or old entries) so entries are addressable in SQL/Mongo and restorable
 * per player. Detection is by the first field: v1/v2 start with the
 * epoch millis (all digits), v3 starts with a UUID, which never parses
 * as a long.
 *
 * format is the tag produced by {@link ItemSerializer}: "nbt"
 * (ItemStack.serializeAsBytes, 1.15+) or "bos" (Bukkit object streams,
 * 1.13/1.14). v1 lines have no format tag; the item decoders try both
 * encodings, so those still load.
 */
public final class RecoveryFormat {

    public static final String FMT_NBT = "nbt";
    public static final String FMT_LEGACY = "bos";
    public static final String FMT_UNKNOWN = "";

    /** One parsed log line without the ItemStack (its decode needs a server). */
    public static final class Parsed {
        public final RecoveryRecord record;
        /** True for v1/v2 lines, which received a generated "gen-" id. */
        public final boolean legacy;

        Parsed(RecoveryRecord record, boolean legacy) {
            this.record = record;
            this.legacy = legacy;
        }
    }

    private RecoveryFormat() {
    }

    /** Splits one log line, or returns null when the line is not a record. */
    public static Parsed parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", 8);
        if (parts.length < 4) {
            return null;
        }
        long millis;
        try {
            millis = Long.parseLong(parts[0]);
        } catch (NumberFormatException v3) {
            return parseV3(parts);
        }
        // v1/v2 had no id - generate one so entries stay addressable
        String player = playerOrNull(parts[1]);
        String world = parts[2];
        if (parts.length == 4) {
            return parsed(generatedId(), millis, null, player, world, FMT_UNKNOWN, parts[3]);
        }
        if (parts.length == 5 && normalizeFormat(parts[3]) != FMT_UNKNOWN) {
            return parsed(generatedId(), millis, null, player, world,
                    normalizeFormat(parts[3]), parts[4]);
        }
        return parsed(generatedId(), millis, null, player, world, FMT_UNKNOWN, parts[parts.length - 1]);
    }

    private static Parsed parseV3(String[] parts) {
        if (parts.length != 7) {
            return null;
        }
        long millis;
        try {
            millis = Long.parseLong(parts[5]);
        } catch (NumberFormatException e) {
            return null;
        }
        return parsed(parts[0], millis, playerOrNull(parts[1]), playerOrNull(parts[2]),
                parts[3], normalizeFormat(parts[4]), parts[6]);
    }

    /** Builds one v3 log line. */
    public static String format(RecoveryRecord record) {
        return record.getId()
                + "|" + orEmpty(record.getPlayerUuid())
                + "|" + orEmpty(record.getPlayerName())
                + "|" + orEmpty(record.getWorld())
                + "|" + (record.getFormat() == null ? FMT_UNKNOWN : record.getFormat())
                + "|" + record.getTimeMillis()
                + "|" + record.getBase64();
    }

    private static Parsed parsed(String id, long millis, String uuid, String player, String world,
                                 String format, String base64) {
        return new Parsed(new RecoveryRecord(id, millis, uuid, player, world, format, base64),
                id.startsWith("gen-"));
    }

    private static String generatedId() {
        return "gen-" + UUID.randomUUID();
    }

    private static String playerOrNull(String field) {
        return field == null || field.isEmpty() ? null : field;
    }

    private static String orEmpty(String field) {
        return field == null ? "" : field;
    }

    private static String normalizeFormat(String field) {
        if (FMT_NBT.equals(field) || FMT_LEGACY.equals(field)) {
            return field;
        }
        return FMT_UNKNOWN;
    }
}
