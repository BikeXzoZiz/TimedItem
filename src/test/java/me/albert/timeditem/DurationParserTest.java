package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DurationParserTest {

    private static Map<String, Long> units() {
        Map<String, Long> map = new HashMap<String, Long>();
        map.put("s", 1L);
        map.put("m", 60L);
        map.put("h", 3600L);
        map.put("d", 86400L);
        return map;
    }

    @Test
    void parsesSingleUnit() {
        assertEquals(90L, DurationParser.parse("90s", units()));
        assertEquals(7200L, DurationParser.parse("2h", units()));
    }

    @Test
    void parsesCombinedUnits() {
        assertEquals(129600L, DurationParser.parse("1d12h", units()));
        assertEquals(131400L, DurationParser.parse("1d12h30m", units()));
        assertEquals(1810L, DurationParser.parse("30m10s", units()));
        assertEquals(86400L * 2 + 5L, DurationParser.parse("2d5s", units()));
    }

    @Test
    void parsesLegacyTwoTokenFormWithSpaces() {
        assertEquals(120L, DurationParser.parse("120 s", units()));
        assertEquals(120L, DurationParser.parse(" 120 s ", units()));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals(1L, DurationParser.parse("1S", units()));
        assertEquals(60L, DurationParser.parse("1M", units()));
    }

    @Test
    void rejectsGarbage() {
        assertEquals(-1L, DurationParser.parse(null, units()));
        assertEquals(-1L, DurationParser.parse("", units()));
        assertEquals(-1L, DurationParser.parse("   ", units()));
        assertEquals(-1L, DurationParser.parse("abc", units()));
        assertEquals(-1L, DurationParser.parse("5", units()));       // no unit
        assertEquals(-1L, DurationParser.parse("1x", units()));      // unknown unit
        assertEquals(-1L, DurationParser.parse("s", units()));       // no number
        assertEquals(-1L, DurationParser.parse("1d 2x", units()));   // one bad token fails all
        assertEquals(-1L, DurationParser.parse("h30m", units()));    // unit before any number
    }

    @Test
    void rejectsZeroAndNegatives() {
        assertEquals(-1L, DurationParser.parse("0s", units()));
        assertEquals(-1L, DurationParser.parse("0m0s", units()));
        assertFalse(DurationParser.parse("-5s", units()) > 0); // '-' is not a digit -> unknown unit "-s"
    }

    @Test
    void rejectsDurationsBeyondFiveYears() {
        assertEquals(-1L, DurationParser.parse("99999999d", units()));
        // exactly at the cap (5 years = 157680000 seconds = 1825 days) is allowed
        assertTrue(DurationParser.parse("157680000s", units()) == 157680000L);
        assertEquals(157680000L, DurationParser.parse("1825d", units()));
        assertEquals(-1L, DurationParser.parse("1826d", units()));
    }

    @Test
    void rejectsOverflowingNumbers() {
        assertEquals(-1L, DurationParser.parse("99999999999999999999s", units()));
    }
}
