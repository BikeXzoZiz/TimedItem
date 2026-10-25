package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningsTest {

    private static final List<Integer> STAGES = Arrays.asList(600, 60, 10);

    private static Set<String> keys() {
        return new HashSet<String>();
    }

    @Test
    void outsideAllStagesIsNotDue() {
        Set<String> warned = keys();
        assertEquals(-1, Warnings.nextStage(1200, STAGES, warned, "line"));
        assertTrue(warned.isEmpty());
    }

    @Test
    void firesStagesInOrderAsCountdownProgresses() {
        Set<String> warned = keys();
        assertEquals(0, Warnings.nextStage(500, STAGES, warned, "line"));
        warned.add(Warnings.key("line", 0));
        assertEquals(-1, Warnings.nextStage(500, STAGES, warned, "line"));
        assertEquals(1, Warnings.nextStage(50, STAGES, warned, "line"));
        warned.add(Warnings.key("line", 1));
        assertEquals(-1, Warnings.nextStage(50, STAGES, warned, "line"));
        assertEquals(2, Warnings.nextStage(5, STAGES, warned, "line"));
    }

    @Test
    void warnedStagesDoNotFireAgain() {
        Set<String> warned = keys();
        int stage = Warnings.nextStage(500, STAGES, warned, "line");
        warned.add(Warnings.key("line", stage));
        assertEquals(-1, Warnings.nextStage(500, STAGES, warned, "line"));
    }

    @Test
    void itemBelowSeveralStagesWarnsOnePerCheck() {
        Set<String> warned = keys();
        assertEquals(0, Warnings.nextStage(8, STAGES, warned, "line"));
        warned.add(Warnings.key("line", 0));
        assertEquals(1, Warnings.nextStage(8, STAGES, warned, "line"));
        warned.add(Warnings.key("line", 1));
        assertEquals(2, Warnings.nextStage(8, STAGES, warned, "line"));
        warned.add(Warnings.key("line", 2));
        assertEquals(-1, Warnings.nextStage(8, STAGES, warned, "line"));
    }

    @Test
    void boundaryRemainingEqualsThresholdFires() {
        assertEquals(0, Warnings.nextStage(600, STAGES, keys(), "line"));
    }

    @Test
    void expiredOrZeroRemainingNeverFires() {
        assertEquals(-1, Warnings.nextStage(0, STAGES, keys(), "line"));
        assertEquals(-1, Warnings.nextStage(-5, STAGES, keys(), "line"));
    }

    @Test
    void keysDifferPerStageAndLine() {
        assertFalse(Warnings.key("a", 0).equals(Warnings.key("a", 1)));
        assertFalse(Warnings.key("a", 0).equals(Warnings.key("b", 0)));
        assertEquals(Warnings.key("a", 0), Warnings.key("a", 0));
    }
}
