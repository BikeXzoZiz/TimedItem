package me.albert.timeditem.storage;

import me.albert.timeditem.RecoveryFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatFileStorageTest {

    private static final Logger LOG = Logger.getLogger("test");

    @TempDir
    Path folder;

    private FlatFileStorage storage() {
        return new FlatFileStorage(folder, LOG, 7 * 86400000L, 100, 1000);
    }

    /** Recent timestamps - keep_days pruning would drop anything older. */
    private static long now() {
        return System.currentTimeMillis();
    }

    @Test
    void recoveryRoundTrip() {
        FlatFileStorage storage = storage();
        RecoveryRecord in = new RecoveryRecord("id-1", now(), "uuid-1", "Steve", "world",
                "nbt", "QUJD");
        storage.insertRecovery(in);
        List<RecoveryRecord> loaded = storage.loadRecovery();
        assertEquals(1, loaded.size());
        assertEquals("id-1", loaded.get(0).getId());
        assertEquals("uuid-1", loaded.get(0).getPlayerUuid());
        assertEquals("Steve", loaded.get(0).getPlayerName());

        storage.deleteRecovery("id-1");
        assertTrue(storage.loadRecovery().isEmpty());
    }

    @Test
    void insertIsAnUpsertById() {
        FlatFileStorage storage = storage();
        long t = now();
        storage.insertRecovery(new RecoveryRecord("id-1", t, null, "Steve", "world",
                "nbt", "AAAA"));
        storage.insertRecovery(new RecoveryRecord("id-1", t + 1000L, null, "Steve", "world",
                "nbt", "BBBB"));
        List<RecoveryRecord> loaded = storage.loadRecovery();
        assertEquals(1, loaded.size());
        assertEquals(t + 1000L, loaded.get(0).getTimeMillis());
        assertEquals("BBBB", loaded.get(0).getBase64());
    }

    @Test
    void maxItemsPrunesOldest() {
        FlatFileStorage storage = new FlatFileStorage(folder, LOG, 7 * 86400000L, 2, 1000);
        for (int i = 0; i < 5; i++) {
            storage.insertRecovery(new RecoveryRecord("id-" + i, now() + i * 1000L, null, "Steve",
                    "world", "nbt", "QUJD"));
        }
        List<RecoveryRecord> loaded = storage.loadRecovery();
        assertEquals(2, loaded.size());
        assertEquals("id-3", loaded.get(0).getId());
        assertEquals("id-4", loaded.get(1).getId());
    }

    @Test
    void keepsLoadingOldFormatLines() throws Exception {
        FlatFileStorage storage = storage();
        // v2 line (no id/uuid) and v1 line (no format tag), recent enough
        // to survive keep_days pruning
        long t = now();
        Files.write(folder.resolve("recovery.log"),
                java.util.Arrays.asList(
                        t + "|Steve|world|nbt|QUJD",
                        (t + 1000) + "||world|QUJD"),
                java.nio.charset.StandardCharsets.UTF_8);
        List<RecoveryRecord> loaded = storage.loadRecovery();
        assertEquals(2, loaded.size());
        assertTrue(loaded.get(0).getId().startsWith("gen-"));
        assertEquals("Steve", loaded.get(0).getPlayerName());
        assertEquals(RecoveryFormat.FMT_UNKNOWN, loaded.get(1).getFormat());
    }

    @Test
    void auditRoundTripWithEscaping() {
        FlatFileStorage storage = storage();
        storage.appendAudit(new AuditRecord(1000L, "uuid-1", "Steve", "set", "Bob",
                "item-uid", "mmoitems:SWORD", "1d|h\\x|p"));
        storage.appendAudit(new AuditRecord(2000L, null, "console", AuditRecord.RESTORE,
                "Steve", null, "DIAMOND", null));

        FlatFileStorage reopened = storage();
        List<AuditRecord> page = reopened.auditPage(10, 0);
        assertEquals(2, page.size());
        AuditRecord newest = page.get(0);
        assertEquals("console", newest.getActorName());
        assertNull(newest.getActorUuid());
        assertNull(newest.getDetails());
        AuditRecord oldest = page.get(1);
        assertEquals("1d|h\\x|p", oldest.getDetails());
        assertEquals("item-uid", oldest.getItemUid());
    }

    @Test
    void statsIncrementAndReload() {
        FlatFileStorage storage = storage();
        storage.incrementExpired("2026-10-26", "DIAMOND");
        storage.incrementExpired("2026-10-26", "DIAMOND");
        storage.incrementExpired("2026-10-25", "ELYTRA");
        storage.incrementExpired("2026-10-20", "ELYTRA");

        Map<String, Map<String, Long>> since25 = storage.expiredStats("2026-10-25");
        assertEquals(2, since25.size());
        assertEquals(2L, since25.get("2026-10-26").get("DIAMOND"));

        FlatFileStorage reopened = storage();
        assertEquals(2L, reopened.expiredStats("1970-01-01").get("2026-10-26").get("DIAMOND"));
    }
}
