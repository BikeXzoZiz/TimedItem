package me.albert.timeditem;

import me.albert.timeditem.storage.RecoveryRecord;
import me.albert.timeditem.storage.StorageWriter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rolling log of expired items so admins can hand them back with
 * /ti restore. The in-memory list feeds the restore GUI; persistence
 * goes through the {@link StorageWriter} (flat file, SQLite, MySQL or
 * MongoDB), so every write is fire-and-forget and safe to call from any
 * region thread. Entries are identified by a random id and carry the
 * player UUID when the expiry had a player.
 *
 * On load every entry is re-encoded in the current server's format, so
 * storage self-migrates after a Minecraft upgrade; entries whose item
 * cannot be deserialized at all are skipped with one warning. All list
 * access is synchronized because expiry can run concurrently on several
 * region threads (Folia).
 */
public final class RecoveryStore {

    /** One recovered expiry. */
    public static final class Entry {
        private final RecoveryRecord record;
        private final ItemStack item;

        Entry(RecoveryRecord record, ItemStack item) {
            this.record = record;
            this.item = item;
        }

        public String getId() {
            return record.getId();
        }

        /** Player UUID, or null for ground/frame expiry and old entries. */
        public String getPlayerUuid() {
            return record.getPlayerUuid();
        }

        public long getTimeMillis() {
            return record.getTimeMillis();
        }

        public String getPlayer() {
            return record.getPlayerName();
        }

        public String getWorld() {
            return record.getWorld();
        }

        public String getFormat() {
            return record.getFormat();
        }

        public ItemStack getItem() {
            return item;
        }
    }

    private final TimedItemPlugin plugin;
    private final StorageWriter writer;
    private final List<Entry> entries = new ArrayList<Entry>();

    public RecoveryStore(TimedItemPlugin plugin, StorageWriter writer) {
        this.plugin = plugin;
        this.writer = writer;
    }

    /**
     * Loads and prunes the log through the storage backend; safe to call
     * again (e.g. /ti reload keeps the backend, this refills memory).
     */
    public synchronized void load() {
        entries.clear();
        if (!plugin.pluginConfig().isRecoveryEnabled()) {
            return;
        }
        int unreadable = 0;
        for (RecoveryRecord record : writer.loadRecovery()) {
            ItemStack item = ItemSerializer.fromBase64(record.getBase64());
            if (item == null) {
                unreadable++;
                continue;
            }
            // Re-encode in this server's format so storage keeps working
            // after a Minecraft upgrade (items saved by an older server
            // may not deserialize on the next one).
            ItemSerializer.Encoded reencoded = ItemSerializer.encode(item);
            if (reencoded != null && (!reencoded.base64.equals(record.getBase64())
                    || !reencoded.format.equals(record.getFormat()))) {
                writer.insertRecovery(record.reencoded(reencoded.format, reencoded.base64));
            }
            entries.add(new Entry(record, item));
        }
        if (unreadable > 0) {
            plugin.getLogger().warning("Skipped " + unreadable
                    + " recovery entries whose items could not be read (items"
                    + " saved on another Minecraft version cannot always be restored).");
        }
    }

    /** Records one expired item; safe to call from any thread. */
    public synchronized void record(ItemStack item, Player player, World world) {
        if (!plugin.pluginConfig().isRecoveryEnabled() || item == null
                || item.getType() == Material.AIR) {
            return;
        }
        ItemSerializer.Encoded encoded = ItemSerializer.encode(item);
        if (encoded == null) {
            return;
        }
        RecoveryRecord record = new RecoveryRecord(UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                player == null ? null : player.getUniqueId().toString(),
                player == null ? null : player.getName(),
                world == null ? "" : world.getName(),
                encoded.format, encoded.base64);
        entries.add(new Entry(record, item.clone()));
        trim();
        writer.insertRecovery(record);
        DebugLog.verbose("Recorded expired " + item.getType() + " in recovery storage ("
                + entries.size() + " entries)");
    }

    /** Removes the entry (e.g. it was handed back via /ti restore). */
    public synchronized void remove(Entry entry) {
        entries.remove(entry);
        writer.deleteRecovery(entry.getId());
    }

    /** Newest first slice of the log for one GUI page. */
    public synchronized List<Entry> page(int pageSize, int pageIndex) {
        List<Entry> out = new ArrayList<Entry>();
        int start = entries.size() - 1 - pageIndex * pageSize;
        for (int i = start; i >= 0 && out.size() < pageSize; i--) {
            out.add(entries.get(i));
        }
        return out;
    }

    /** Newest first entries for one player UUID (restore reminders, /ti restore <player>). */
    public synchronized int countForPlayer(String playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        int count = 0;
        for (Entry entry : entries) {
            if (playerUuid.equals(entry.getPlayerUuid())) {
                count++;
            }
        }
        return count;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized int pageCount(int pageSize) {
        return Math.max(1, (entries.size() + pageSize - 1) / pageSize);
    }

    private void trim() {
        int max = plugin.pluginConfig().getRecoveryMaxItems();
        while (entries.size() > max) {
            entries.remove(0); // oldest first
        }
    }
}
