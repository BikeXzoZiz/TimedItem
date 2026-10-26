package me.albert.timeditem.storage;

/**
 * One expired item as the storage backends see it: plain data, no
 * Bukkit types, so backends can be unit tested without a server. The
 * item itself travels as the base64 produced by ItemSerializer; the
 * decoding back into an ItemStack happens in RecoveryStore.
 */
public final class RecoveryRecord {

    private final String id;
    private final long timeMillis;
    private final String playerUuid; // null = unknown (old log lines) or ground expiry
    private final String playerName; // null = ground expiry
    private final String world;
    private final String format; // ItemSerializer format tag
    private final String base64;

    public RecoveryRecord(String id, long timeMillis, String playerUuid, String playerName,
                          String world, String format, String base64) {
        this.id = id;
        this.timeMillis = timeMillis;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.world = world;
        this.format = format;
        this.base64 = base64;
    }

    public String getId() {
        return id;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getWorld() {
        return world;
    }

    public String getFormat() {
        return format;
    }

    public String getBase64() {
        return base64;
    }

    /** Same record under a different id (used when migrating old log lines). */
    public RecoveryRecord withId(String newId) {
        return new RecoveryRecord(newId, timeMillis, playerUuid, playerName, world, format, base64);
    }

    /** Same record re-encoded for the running server version. */
    public RecoveryRecord reencoded(String newFormat, String newBase64) {
        return new RecoveryRecord(id, timeMillis, playerUuid, playerName, world, newFormat, newBase64);
    }
}
