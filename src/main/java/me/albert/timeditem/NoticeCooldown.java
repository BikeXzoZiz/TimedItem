package me.albert.timeditem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimum ms between two renewal or grace messages of one player, so a
 * spammed right-click fires the notice once. Shared by
 * {@link RenewalHandler} and {@link GraceHandler}.
 */
final class NoticeCooldown {

    private static final long COOLDOWN_MILLIS = 250L;

    private final Map<UUID, Long> last = new ConcurrentHashMap<UUID, Long>();

    /** True (and arms the cooldown) when enough time has passed. */
    boolean tryAcquire(UUID id) {
        long now = System.currentTimeMillis();
        Long lastAt = last.get(id);
        if (lastAt != null && now - lastAt < COOLDOWN_MILLIS) {
            return false;
        }
        last.put(id, now);
        return true;
    }

    void clear(UUID id) {
        last.remove(id);
    }
}
