package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the parsing of Settings.visual_stages: bad entries are skipped,
 * color codes are translated, glow only comes from a real boolean, and
 * the result is ordered from the highest threshold down (index 0 = least
 * severe stage).
 */
class VisualStagesTest {

    private static Map<Object, Object> stage(Object at, Object color, Object glow) {
        Map<Object, Object> map = new HashMap<Object, Object>();
        if (at != null) {
            map.put("at_seconds", at);
        }
        if (color != null) {
            map.put("name_color", color);
        }
        if (glow != null) {
            map.put("glow", glow);
        }
        return map;
    }

    @Test
    void emptyAndNullInputGiveNoStages() {
        assertTrue(VisualStages.parse(null).isEmpty());
        assertTrue(VisualStages.parse(java.util.Collections.<Map<?, ?>>emptyList()).isEmpty());
    }

    @Test
    void skipsEntriesWithoutPositiveAtSeconds() {
        List<VisualStages.Stage> stages = VisualStages.parse(Arrays.<Map<?, ?>>asList(
                stage(null, "&c", null),          // no threshold
                stage(0, "&c", null),             // zero
                stage(-5, "&c", null),            // negative
                stage("soon", "&c", null),        // not a number
                stage(60, "&c", null)));          // the one valid row
        assertEquals(1, stages.size());
        assertEquals(60, stages.get(0).atSeconds);
    }

    @Test
    void skipsNullEntries() {
        List<VisualStages.Stage> stages = VisualStages.parse(Arrays.<Map<?, ?>>asList(
                null, stage(30, null, null)));
        assertEquals(1, stages.size());
    }

    @Test
    void translatesColorCodes() {
        VisualStages.Stage stage = VisualStages.parse(
                java.util.Collections.singletonList(stage(30, "&c&l", null))).get(0);
        assertEquals("\u00A7c\u00A7l", stage.nameColor);
    }

    @Test
    void missingColorMeansKeepName() {
        VisualStages.Stage stage = VisualStages.parse(
                java.util.Collections.singletonList(stage(30, null, null))).get(0);
        assertEquals("", stage.nameColor);
    }

    @Test
    void glowOnlyFromRealBoolean() {
        List<VisualStages.Stage> stages = VisualStages.parse(Arrays.<Map<?, ?>>asList(
                stage(30, null, true),
                stage(20, null, "yes"),   // not a Boolean -> no glow
                stage(10, null, false)));
        assertTrue(stages.get(0).glow);
        assertFalse(stages.get(1).glow);
        assertFalse(stages.get(2).glow);
    }

    @Test
    void sortsFromHighestThresholdDown() {
        List<VisualStages.Stage> stages = VisualStages.parse(Arrays.<Map<?, ?>>asList(
                stage(10, "&c", null),
                stage(600, "&e", null),
                stage(60, "&6", null)));
        assertEquals(3, stages.size());
        // index 0 is the least severe stage = the highest threshold
        assertEquals(600, stages.get(0).atSeconds);
        assertEquals(60, stages.get(1).atSeconds);
        assertEquals(10, stages.get(2).atSeconds);
    }
}
