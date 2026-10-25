package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the pure lore analysis of TimeLore (no Bukkit classes involved).
 * Prefixes here are plain strings; in-game they carry color codes first,
 * which does not change the parsing.
 */
class TimeLoreTest {

    private static final String EXPIRE_PREFIX = "Expires: ";
    private static final String LIFE_PREFIX = "Lifetime: ";
    private static final String PAUSED_PREFIX = "Paused: ";

    private static Map<String, Long> units() {
        Map<String, Long> map = new HashMap<String, Long>();
        map.put("s", 1L);
        map.put("m", 60L);
        map.put("h", 3600L);
        map.put("d", 86400L);
        return map;
    }

    private static TimeLore.TimerInfo analyze(String... lines) {
        List<String> lore = Arrays.asList(lines);
        return TimeLore.analyzeLore(lore, EXPIRE_PREFIX, LIFE_PREFIX, PAUSED_PREFIX, units());
    }

    @Test
    void classifiesExpireLine() {
        TimeLore.TimerInfo info = analyze("Expires: §s2026-10-25 12:00:00");
        assertNotNull(info);
        assertEquals(TimeLore.Type.EXPIRE, info.getType());
        assertEquals(LocalDateTime.of(2026, 10, 25, 12, 0, 0), info.getExpiry());
        assertEquals(-1L, info.getSeconds());
    }

    @Test
    void classifiesLifetimeLineWithCombinedLabel() {
        TimeLore.TimerInfo info = analyze("Lifetime: §s129600§s1d12h");
        assertNotNull(info);
        assertEquals(TimeLore.Type.LIFETIME, info.getType());
        assertEquals(129600L, info.getSeconds());
    }

    @Test
    void parsesLegacySingleUnitLifetime() {
        TimeLore.TimerInfo info = analyze("Lifetime: §s30§sm");
        assertNotNull(info);
        assertEquals(1800L, info.getSeconds());

        assertEquals(432000L, analyze("Lifetime: §s5§sd").getSeconds());
    }

    @Test
    void fallsBackToAmountFieldAsSeconds() {
        TimeLore.TimerInfo info = analyze("Lifetime: §s600§s???");
        assertNotNull(info);
        assertEquals(600L, info.getSeconds());
    }

    @Test
    void classifiesPausedLine() {
        TimeLore.TimerInfo info = analyze("Paused: §s600§s10m");
        assertNotNull(info);
        assertEquals(TimeLore.Type.PAUSED, info.getType());
        assertEquals(600L, info.getSeconds());
    }

    @Test
    void malformedExpireLineIsIgnored() {
        assertNull(analyze("Expires: §snot-a-date"));
        assertNull(analyze("Expires: "));
    }

    @Test
    void malformedExpireLineDoesNotHideLaterLines() {
        TimeLore.TimerInfo info = analyze("Expires: §snot-a-date", "Lifetime: §s600§s10m");
        assertNotNull(info);
        assertEquals(TimeLore.Type.LIFETIME, info.getType());
        assertEquals(600L, info.getSeconds());
    }

    @Test
    void untimedLoreReturnsNull() {
        assertNull(analyze());
        assertNull(analyze("Sharpness V", "Some other lore"));
        assertNull(analyze(""));
    }

    @Test
    void firstMatchingLineWins() {
        TimeLore.TimerInfo info = analyze(
                "Expires: §s2026-01-01 00:00:00",
                "Lifetime: §s600§s10m");
        assertEquals(TimeLore.Type.EXPIRE, info.getType());
    }

    @Test
    void loreAfterOtherLinesIsStillFound() {
        TimeLore.TimerInfo info = analyze("Sharpness V", "", "Paused: §s99§s1m39s");
        assertNotNull(info);
        assertEquals(TimeLore.Type.PAUSED, info.getType());
        assertEquals(99L, info.getSeconds());
    }

    @Test
    void formatDurationUsesConfiguredUnits() {
        assertEquals("1d12h", TimeLore.formatDuration(129600L, "d", "h", "m", "s"));
        assertEquals("30m", TimeLore.formatDuration(1800L, "d", "h", "m", "s"));
        assertEquals("5s", TimeLore.formatDuration(5L, "d", "h", "m", "s"));
        assertEquals("0s", TimeLore.formatDuration(0L, "d", "h", "m", "s"));
        assertEquals("1d1s", TimeLore.formatDuration(86401L, "d", "h", "m", "s"));
    }
}
