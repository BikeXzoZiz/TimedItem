package me.albert.timeditem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecoveryFormatTest {

    @Test
    void parsesCurrentFormatLine() {
        RecoveryFormat.Parts parts = RecoveryFormat.parse(
                "1730000000000|Steve|world|" + ItemSerializer.FORMAT_NBT + "|QUJD");
        assertEquals(1730000000000L, parts.timeMillis);
        assertEquals("Steve", parts.player);
        assertEquals("world", parts.world);
        assertEquals(ItemSerializer.FORMAT_NBT, parts.format);
        assertEquals("QUJD", parts.base64);
    }

    @Test
    void parsesLegacyFourFieldLineWithUnknownFormat() {
        RecoveryFormat.Parts parts = RecoveryFormat.parse("1730000000000||world_nether|QUJD");
        assertEquals(1730000000000L, parts.timeMillis);
        assertNull(parts.player);
        assertEquals("world_nether", parts.world);
        assertEquals(RecoveryFormat.FMT_UNKNOWN, parts.format);
        assertEquals("QUJD", parts.base64);
    }

    @Test
    void emptyPlayerMeansGroundExpiry() {
        RecoveryFormat.Parts parts = RecoveryFormat.parse(
                "1||w|" + ItemSerializer.FORMAT_LEGACY + "|QUJD");
        assertNull(parts.player);
        assertEquals(RecoveryFormat.FMT_LEGACY, parts.format);
    }

    @Test
    void unknownFormatTagNormalizesToUnknown() {
        RecoveryFormat.Parts parts = RecoveryFormat.parse("1|Steve|world|future|QUJD");
        assertEquals(RecoveryFormat.FMT_UNKNOWN, parts.format);
    }

    @Test
    void rejectsGarbageLines() {
        assertNull(RecoveryFormat.parse(null));
        assertNull(RecoveryFormat.parse(""));
        assertNull(RecoveryFormat.parse("not a record"));
        assertNull(RecoveryFormat.parse("abc|Steve|world|nbt|QUJD"));
        assertNull(RecoveryFormat.parse("1|Steve"));
    }

    @Test
    void formatThenParseRoundTrips() {
        String line = RecoveryFormat.format(1730000000000L, null, "world", ItemSerializer.FORMAT_NBT,
                "QUJD");
        RecoveryFormat.Parts parts = RecoveryFormat.parse(line);
        assertEquals(1730000000000L, parts.timeMillis);
        assertNull(parts.player);
        assertEquals("world", parts.world);
        assertEquals(ItemSerializer.FORMAT_NBT, parts.format);
        assertEquals("QUJD", parts.base64);
    }

    @Test
    void formatTreatsNullWorldAndFormatAsEmpty() {
        String line = RecoveryFormat.format(5L, "Steve", null, null, "QUJD");
        RecoveryFormat.Parts parts = RecoveryFormat.parse(line);
        assertEquals("", parts.world);
        assertEquals(RecoveryFormat.FMT_UNKNOWN, parts.format);
    }
}
