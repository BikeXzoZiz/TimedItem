package me.albert.timeditem;

/**
 * Line format of recovery.log, kept free of Bukkit types so it can be
 * unit tested without a server:
 *
 *   millis|playerName|worldName|format|base64(ItemStack)
 *
 * format is the tag produced by {@link ItemSerializer}: "nbt"
 * (ItemStack.serializeAsBytes, 1.15+) or "bos" (Bukkit object streams,
 * 1.13/1.14). Lines written before the tag existed have four fields and
 * parse with an unknown format; the item decoders try both encodings,
 * so those still load.
 */
public final class RecoveryFormat {

    public static final String FMT_NBT = "nbt";
    public static final String FMT_LEGACY = "bos";
    public static final String FMT_UNKNOWN = "";

    /** One parsed log line without the ItemStack (its decode needs a server). */
    public static final class Parts {
        public final long timeMillis;
        public final String player; // null = ground expiry
        public final String world;
        public final String format; // FMT_NBT, FMT_LEGACY or FMT_UNKNOWN
        public final String base64;

        Parts(long timeMillis, String player, String world, String format, String base64) {
            this.timeMillis = timeMillis;
            this.player = player;
            this.world = world;
            this.format = format;
            this.base64 = base64;
        }
    }

    private RecoveryFormat() {
    }

    /** Splits one log line, or returns null when the line is not a record. */
    public static Parts parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", 5);
        if (parts.length < 4) {
            return null;
        }
        long millis;
        try {
            millis = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (parts.length == 4) {
            return new Parts(millis, playerOrNull(parts[1]), parts[2], FMT_UNKNOWN, parts[3]);
        }
        return new Parts(millis, playerOrNull(parts[1]), parts[2], normalizeFormat(parts[3]), parts[4]);
    }

    /** Builds one log line. */
    public static String format(long timeMillis, String player, String world, String format,
                                String base64) {
        return timeMillis + "|"
                + (player == null ? "" : player) + "|"
                + (world == null ? "" : world) + "|"
                + (format == null ? FMT_UNKNOWN : format) + "|"
                + base64;
    }

    private static String playerOrNull(String field) {
        return field.isEmpty() ? null : field;
    }

    private static String normalizeFormat(String field) {
        if (FMT_NBT.equals(field) || FMT_LEGACY.equals(field)) {
            return field;
        }
        return FMT_UNKNOWN;
    }
}
