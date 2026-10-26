package me.albert.timeditem;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the snapshot logic of TrackedItems without a server: the Player
 * handle is a dynamic proxy (record() only reads name and uuid), so the
 * filter/cycle semantics can be pinned on a plain JVM.
 */
class TrackedItemsTest {

    private static Player fakePlayer(final String name, final UUID uuid) {
        return (Player) Proxy.newProxyInstance(TrackedItemsTest.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("getName".equals(methodName)) {
                        return name;
                    }
                    if ("getUniqueId".equals(methodName)) {
                        return uuid;
                    }
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) {
                        return false;
                    }
                    if (type == int.class) {
                        return 0;
                    }
                    return null;
                });
    }

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void recordsAndSnapshotsEverything() {
        TrackedItems tracked = new TrackedItems();
        Player alice = fakePlayer("Alice", UUID_A);
        Player bob = fakePlayer("Bob", UUID_B);
        tracked.record(alice, 3, "DIAMOND_SWORD", "mmoitems:SWORD:CUTLASS", "EXPIRE", 120L);
        tracked.record(bob, 8, "GOLDEN_APPLE", null, "LIFETIME", 3600L);

        List<TrackedItems.Tracked> all = tracked.snapshot("");
        assertEquals(2, all.size());
        assertEquals(2, tracked.count(""));

        TrackedItems.Tracked first = all.get(0);
        assertEquals("Alice", first.ownerName);
        assertEquals(UUID_A.toString(), first.ownerUuid);
        assertEquals(3, first.slot);
        assertEquals("DIAMOND_SWORD", first.material);
        assertEquals("mmoitems:SWORD:CUTLASS", first.itemId);
        assertEquals("EXPIRE", first.typeName);
        assertEquals(120L, first.remainingSeconds);
    }

    @Test
    void beginCycleClearsTheSnapshot() {
        TrackedItems tracked = new TrackedItems();
        tracked.record(fakePlayer("Alice", UUID_A), 0, "STONE", null, "EXPIRE", 5L);
        assertEquals(1, tracked.count(""));
        tracked.beginCycle();
        assertEquals(0, tracked.count(""));
        assertTrue(tracked.snapshot("").isEmpty());
    }

    @Test
    void filterMatchesOwnerMaterialItemIdAndType() {
        TrackedItems tracked = new TrackedItems();
        tracked.record(fakePlayer("Alice", UUID_A), 0, "DIAMOND_SWORD", "mmoitems:SWORD:CUTLASS", "EXPIRE", 10L);
        tracked.record(fakePlayer("Bob", UUID_B), 1, "GOLDEN_APPLE", null, "LIFETIME", 20L);

        assertEquals(1, tracked.count("alice"));
        assertEquals(1, tracked.count("golden_apple"));
        assertEquals(1, tracked.count("mmoitems"));
        assertEquals(1, tracked.count("lifetime"));
        assertEquals(0, tracked.count("netherite"));
        // filter is case-insensitive
        assertEquals(1, tracked.count("ALICE"));
    }

    @Test
    void allReturnsACopyThatSurvivesCycles() {
        TrackedItems tracked = new TrackedItems();
        tracked.record(fakePlayer("Alice", UUID_A), 0, "STONE", null, "EXPIRE", 5L);
        List<TrackedItems.Tracked> copy = tracked.all();
        assertEquals(1, copy.size());
        tracked.beginCycle();
        // the debug report keeps its own copy, not a live view
        assertEquals(1, copy.size());
    }
}
