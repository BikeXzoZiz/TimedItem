package me.albert.timeditem;

import java.util.List;
import java.util.Set;

/**
 * Pure warning-stage logic: given the remaining seconds, the configured
 * thresholds (highest first) and the keys already warned for the player,
 * returns the stage that should fire now. Only the highest due stage
 * fires per check, so an item created below several thresholds warns
 * once per scan instead of spamming every stage at once.
 */
public final class Warnings {

    private Warnings() {
    }

    /**
     * Returns the index into {@code stagesDesc} of the highest
     * un-warned stage whose threshold covers {@code remaining}, or -1
     * when nothing is due. The caller adds the key via {@link #key}
     * after firing.
     */
    public static int nextStage(long remaining, List<Integer> stagesDesc,
                                Set<String> warnedKeys, String line) {
        if (remaining <= 0) {
            return -1;
        }
        for (int i = 0; i < stagesDesc.size(); i++) {
            if (remaining <= stagesDesc.get(i) && !warnedKeys.contains(key(line, i))) {
                return i;
            }
        }
        return -1;
    }

    /** Dedup key for one item line at one stage. */
    public static String key(String line, int stageIndex) {
        return line + "|w" + stageIndex;
    }
}
