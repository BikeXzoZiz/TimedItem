package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Rolling log of expired items so admins can hand them back with
 * /ti restore. One entry per line in plugins/TimedItem/recovery.log:
 *
 *   millis|playerName|worldName|base64(ItemStack)
 *
 * Entries live in memory (newest last) and the file is rewritten on
 * every change; the size stays bounded through max_items and keep_days.
 * Entries whose item cannot be deserialized are dropped at load time.
 */
public final class RecoveryStore {

    /** One recovered expiry. */
    public static final class Entry {
        private final long timeMillis;
        private final String player; // null for ground/frame expiry
        private final String world;
        private final String base64;
        private final ItemStack item;

        Entry(long timeMillis, String player, String world, String base64, ItemStack item) {
            this.timeMillis = timeMillis;
            this.player = player;
            this.world = world;
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

        public ItemStack getItem() {
            return item;
        }
    }

    private final TimedItemPlugin plugin;
    private final List<Entry> entries = new ArrayList<Entry>();

    public RecoveryStore(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads and prunes the log; safe to call when the file does not exist yet. */
    public void load() {
        entries.clear();
        if (!plugin.pluginConfig().isRecoveryEnabled()) {
            return;
        }
        Path file = file();
        if (Files.exists(file)) {
            try {
                long cutoff = System.currentTimeMillis()
                        - plugin.pluginConfig().getRecoveryKeepDays() * 86400000L;
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    Entry entry = parse(line);
                    if (entry != null && entry.timeMillis >= cutoff) {
                        entries.add(entry);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not read recovery log: " + e);
            }
        }
        trim();
        save();
    }

    /** Records one expired item; the caller runs on the main thread. */
    public void record(ItemStack item, Player player, World world) {
        if (!plugin.pluginConfig().isRecoveryEnabled() || item == null
                || item.getType() == Material.AIR) {
            return;
        }
        String base64 = ItemSerializer.toBase64(item);
        if (base64 == null) {
            return;
        }
        entries.add(new Entry(System.currentTimeMillis(),
                player == null ? null : player.getName(),
                world == null ? "" : world.getName(),
                base64, item.clone()));
        trim();
        save();
    }

    /** Removes the entry (e.g. it was handed back via /ti restore). */
    public void remove(Entry entry) {
        entries.remove(entry);
        save();
    }

    /** Newest first slice of the log for one GUI page. */
    public List<Entry> page(int pageSize, int pageIndex) {
        List<Entry> out = new ArrayList<Entry>();
        int start = entries.size() - 1 - pageIndex * pageSize;
        for (int i = start; i >= 0 && out.size() < pageSize; i--) {
            out.add(entries.get(i));
        }
        return out;
    }

    public int size() {
        return entries.size();
    }

    public int pageCount(int pageSize) {
        return Math.max(1, (entries.size() + pageSize - 1) / pageSize);
    }

    private void trim() {
        int max = plugin.pluginConfig().getRecoveryMaxItems();
        while (entries.size() > max) {
            entries.remove(0); // oldest first
        }
    }

    private Entry parse(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) {
            return null;
        }
        long millis;
        try {
            millis = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        ItemStack item = ItemSerializer.fromBase64(parts[3]);
        if (item == null) {
            return null;
        }
        return new Entry(millis,
                parts[1].isEmpty() ? null : parts[1],
                parts[2],
                parts[3], item);
    }

    private void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            StringBuilder out = new StringBuilder();
            for (Entry entry : entries) {
                out.append(entry.timeMillis).append('|')
                        .append(entry.player == null ? "" : entry.player).append('|')
                        .append(entry.world == null ? "" : entry.world).append('|')
                        .append(entry.base64).append('\n');
            }
            Files.write(file, out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not write recovery log: " + e);
        }
    }

    private Path file() {
        return plugin.getDataFolder().toPath().resolve("recovery.log");
    }
}
