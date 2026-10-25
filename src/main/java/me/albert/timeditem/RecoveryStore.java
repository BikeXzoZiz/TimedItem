package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Rolling log of expired items so admins can hand them back with
 * /ti restore. One entry per line in plugins/TimedItem/recovery.log
 * (line format in {@link RecoveryFormat}):
 *
 *   millis|playerName|worldName|format|base64(ItemStack)
 *
 * Entries live in memory (newest last). Every change schedules a file
 * rewrite on one background thread (snapshot first, then write to a temp
 * file and atomically move it), so expiry bursts never block the main
 * or a region thread; the size stays bounded through max_items and
 * keep_days. On load every entry is re-encoded in the current server's
 * format, so the file migrates itself after a Minecraft upgrade;
 * entries whose item cannot be deserialized at all are skipped with one
 * warning. All entry access is synchronized because on Folia expiry can
 * run concurrently on several region threads.
 */
public final class RecoveryStore {

    /** One recovered expiry. */
    public static final class Entry {
        private final long timeMillis;
        private final String player; // null for ground/frame expiry
        private final String world;
        private final String format; // ItemSerializer format tag of base64
        private final String base64;
        private final ItemStack item;

        Entry(long timeMillis, String player, String world, String format, String base64,
              ItemStack item) {
            this.timeMillis = timeMillis;
            this.player = player;
            this.world = world;
            this.format = format;
            this.base64 = base64;
            this.item = item;
        }

        public long getTimeMillis() {
            return timeMillis;
        }

        public String getPlayer() {
            return player;
        }

        public String getWorld() {
            return world;
        }

        public String getFormat() {
            return format;
        }

        public ItemStack getItem() {
            return item;
        }
    }

    private final TimedItemPlugin plugin;
    private final List<Entry> entries = new ArrayList<Entry>();
    private final ExecutorService saver = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "TimedItem-RecoverySaver");
        thread.setDaemon(true);
        return thread;
    });
    private boolean closed;

    public RecoveryStore(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads and prunes the log; safe to call when the file does not exist yet. */
    public synchronized void load() {
        entries.clear();
        if (!plugin.pluginConfig().isRecoveryEnabled()) {
            return;
        }
        Path file = file();
        if (Files.exists(file)) {
            try {
                long cutoff = System.currentTimeMillis()
                        - plugin.pluginConfig().getRecoveryKeepDays() * 86400000L;
                int unreadable = 0;
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    RecoveryFormat.Parts parts = RecoveryFormat.parse(line);
                    if (parts == null || parts.timeMillis < cutoff) {
                        continue;
                    }
                    ItemStack item = ItemSerializer.fromBase64(parts.base64);
                    if (item == null) {
                        unreadable++;
                        continue;
                    }
                    // Re-encode in this server's format so the file keeps
                    // working after a Minecraft upgrade (items written by an
                    // older server may not deserialize on the next one).
                    String base64 = parts.base64;
                    String format = parts.format;
                    ItemSerializer.Encoded reencoded = ItemSerializer.encode(item);
                    if (reencoded != null) {
                        base64 = reencoded.base64;
                        format = reencoded.format;
                    }
                    entries.add(new Entry(parts.timeMillis, parts.player, parts.world,
                            format, base64, item));
                }
                if (unreadable > 0) {
                    plugin.getLogger().warning("Skipped " + unreadable
                            + " recovery log entries whose items could not be read (items"
                            + " saved on another Minecraft version cannot always be restored).");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not read recovery log: " + e);
            }
        }
        trim();
        save();
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
        entries.add(new Entry(System.currentTimeMillis(),
                player == null ? null : player.getName(),
                world == null ? "" : world.getName(),
                encoded.format, encoded.base64, item.clone()));
        trim();
        save();
        DebugLog.verbose("Recorded expired " + item.getType() + " in the recovery log ("
                + entries.size() + " entries)");
    }

    /** Removes the entry (e.g. it was handed back via /ti restore). */
    public synchronized void remove(Entry entry) {
        entries.remove(entry);
        save();
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

    public synchronized int size() {
        return entries.size();
    }

    public synchronized int pageCount(int pageSize) {
        return Math.max(1, (entries.size() + pageSize - 1) / pageSize);
    }

    /** Stops the background writer after flushing pending saves. Call on disable. */
    public void close() {
        synchronized (this) {
            closed = true;
        }
        saver.shutdown();
        try {
            saver.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void trim() {
        int max = plugin.pluginConfig().getRecoveryMaxItems();
        while (entries.size() > max) {
            entries.remove(0); // oldest first
        }
    }

    /** Queues one full rewrite; the snapshot and target path are taken under the entry lock. */
    private void save() {
        if (closed) {
            return;
        }
        final List<Entry> snapshot = new ArrayList<Entry>(entries);
        final Path target = file();
        saver.execute(() -> writeAtomically(target, serialize(snapshot)));
    }

    private static String serialize(List<Entry> snapshot) {
        StringBuilder out = new StringBuilder();
        for (Entry entry : snapshot) {
            out.append(RecoveryFormat.format(entry.timeMillis, entry.player, entry.world,
                    entry.format, entry.base64)).append('\n');
        }
        return out.toString();
    }

    private void writeAtomically(Path file, String content) {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            // the throwable goes into the capture buffer for /ti debug export
            plugin.getLogger().log(Level.WARNING, "Could not write recovery log: " + e, e);
        }
    }

    /** Path of the recovery log; also read by the debug report export. */
    public Path file() {
        return plugin.getDataFolder().toPath().resolve("recovery.log");
    }
}
