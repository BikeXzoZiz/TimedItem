package me.albert.timeditem;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * In-memory snapshot of every timed item seen by the last periodic scan
 * (online players' inventories). Fills the /ti admin overview GUI; it is
 * view data only - the scan clears and repopulates it each cycle, so an
 * entry disappearing just means the item moved or expired. All access is
 * synchronized because on Folia the per-player region tasks record
 * concurrently.
 */
public final class TrackedItems {

    /** One tracked timed item (plain data for the GUI). */
    public static final class Tracked {
        public final String ownerName;
        public final String ownerUuid;
        public final int slot;
        public final String material;
        public final String itemId; // plugin:id or material name
        public final String typeName;
        public final long remainingSeconds;

        Tracked(String ownerName, String ownerUuid, int slot, String material,
                String itemId, String typeName, long remainingSeconds) {
            this.ownerName = ownerName;
            this.ownerUuid = ownerUuid;
            this.slot = slot;
            this.material = material;
            this.itemId = itemId;
            this.typeName = typeName;
            this.remainingSeconds = remainingSeconds;
        }

        public String matches() {
            return (ownerName + " " + material + " " + itemId + " " + typeName)
                    .toLowerCase(Locale.ROOT);
        }
    }

    private final List<Tracked> items = new ArrayList<Tracked>();

    /** Clears the snapshot; called once at the start of each scan cycle. */
    public synchronized void beginCycle() {
        items.clear();
    }

    /** Records one timed item seen during the scan. */
    public synchronized void record(Player player, int slot, String material,
                                    String itemId, String typeName, long remainingSeconds) {
        items.add(new Tracked(player.getName(), player.getUniqueId().toString(), slot,
                material, itemId, typeName, remainingSeconds));
    }

    /** Snapshot filtered by a query substring (empty = everything), newest irrelevant order. */
    public synchronized List<Tracked> snapshot(String filter) {
        List<Tracked> out = new ArrayList<Tracked>();
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        for (Tracked tracked : items) {
            if (needle.isEmpty() || tracked.matches().contains(needle)) {
                out.add(tracked);
            }
        }
        return out;
    }

    public synchronized int count(String filter) {
        return snapshot(filter).size();
    }

    /** Unmodifiable full snapshot (debug report). */
    public synchronized List<Tracked> all() {
        return Collections.unmodifiableList(new ArrayList<Tracked>(items));
    }
}
