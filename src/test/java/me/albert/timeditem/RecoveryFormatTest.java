package me.albert.timeditem;

import me.albert.timeditem.storage.RecoveryRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryFormatTest {

    @Test
    void parsesV3LineWithIdAndUuid() {
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse(
                "abc-123|uuid-1|Steve|world|" + ItemSerializer.FORMAT_NBT + "|1730000000000|QUJD");
        assertFalse(parsed.legacy);
        RecoveryRecord record = parsed.record;
        assertEquals("abc-123", record.getId());
        assertEquals("uuid-1", record.getPlayerUuid());
        assertEquals("Steve", record.getPlayerName());
        assertEquals("world", record.getWorld());
        assertEquals(ItemSerializer.FORMAT_NBT, record.getFormat());
        assertEquals(1730000000000L, record.getTimeMillis());
        assertEquals("QUJD", record.getBase64());
    }

    @Test
    void v3EmptyUuidMeansGroundExpiry() {
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse(
                "abc-123|||world_nether|QUJD|1730000000000|QUJD");
        assertNull(parsed.record.getPlayerUuid());
        assertNull(parsed.record.getPlayerName());
    }

    @Test
    void parsesV2LineWithGeneratedId() {
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse(
                "1730000000000|Steve|world|" + ItemSerializer.FORMAT_NBT + "|QUJD");
        assertTrue(parsed.legacy);
        assertEquals("Steve", parsed.record.getPlayerName());
        assertEquals(1730000000000L, parsed.record.getTimeMillis());
        assertEquals(ItemSerializer.FORMAT_NBT, parsed.record.getFormat());
        assertEquals("QUJD", parsed.record.getBase64());
        assertTrue(parsed.record.getId().startsWith("gen-"));
        assertNull(parsed.record.getPlayerUuid());
    }

    @Test
    void parsesLegacyFourFieldLineWithUnknownFormat() {
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse("1730000000000||world_nether|QUJD");
        assertTrue(parsed.legacy);
        assertNull(parsed.record.getPlayerName());
        assertEquals("world_nether", parsed.record.getWorld());
        assertEquals(RecoveryFormat.FMT_UNKNOWN, parsed.record.getFormat());
        assertEquals("QUJD", parsed.record.getBase64());
    }

    @Test
    void emptyPlayerMeansGroundExpiry() {
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse(
                "1||w|" + ItemSerializer.FORMAT_LEGACY + "|QUJD");
        assertNull(parsed.record.getPlayerName());
        assertEquals(RecoveryFormat.FMT_LEGACY, parsed.record.getFormat());
    }

    @Test
    void unknownFormatTagNormalizesToUnknown() {
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse("1|Steve|world|future|QUJD");
        assertEquals(RecoveryFormat.FMT_UNKNOWN, parsed.record.getFormat());
    }

    @Test
    void rejectsGarbageLines() {
        assertNull(RecoveryFormat.parse(null));
        assertNull(RecoveryFormat.parse(""));
        assertNull(RecoveryFormat.parse("not a record"));
        assertNull(RecoveryFormat.parse("abc|Steve|world|nbt|QUJD")); // v3 shape with 4 fields
        assertNull(RecoveryFormat.parse("1|Steve"));
    }

    @Test
    void formatThenParseRoundTripsV3() {
        RecoveryRecord record = new RecoveryRecord("id-1", 1730000000000L, "uuid-1", null,
                "world", ItemSerializer.FORMAT_NBT, "QUJD");
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse(RecoveryFormat.format(record));
        assertFalse(parsed.legacy);
        assertEquals("id-1", parsed.record.getId());
        assertEquals("uuid-1", parsed.record.getPlayerUuid());
        assertNull(parsed.record.getPlayerName());
        assertEquals(1730000000000L, parsed.record.getTimeMillis());
        assertEquals("world", parsed.record.getWorld());
        assertEquals(ItemSerializer.FORMAT_NBT, parsed.record.getFormat());
        assertEquals("QUJD", parsed.record.getBase64());
    }

    @Test
    void formatTreatsNullWorldAndFormatAsEmpty() {
        RecoveryRecord record = new RecoveryRecord("id-1", 5L, null, "Steve", null, null, "QUJD");
        RecoveryFormat.Parsed parsed = RecoveryFormat.parse(RecoveryFormat.format(record));
        assertEquals("", parsed.record.getWorld());
        assertEquals(RecoveryFormat.FMT_UNKNOWN, parsed.record.getFormat());
    }
}
