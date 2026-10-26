package me.albert.timeditem.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the SQL storage path against a real embedded SQLite file - the
 * same code path MySQL uses (SqlStorage), minus the network. Dialect
 * differences (audit id column, stats upsert) are covered because they
 * live in the subclass under test here for SQLite.
 */
class SqliteStorageTest {

    private static final Logger LOG = Logger.getLogger("test");

    @TempDir
    Path folder;

    @Test
    void recoveryRoundTripAndPrune() {
        try (SqliteStorage storage = new SqliteStorage(folder.resolve("db.sqlite"), LOG,
                7 * 86400000L, 100, 1000)) {
            storage.insertRecovery(new RecoveryRecord("id-1", System.currentTimeMillis(),
                    "uuid-1", "Steve", "world", "nbt", "QUJD"));
            storage.insertRecovery(new RecoveryRecord("id-2", 1L, null, null, "world",
                    "nbt", "QUJD")); // older than keep_days -> pruned on load
            List<RecoveryRecord> loaded = storage.loadRecovery();
            assertEquals(1, loaded.size());
            assertEquals("id-1", loaded.get(0).getId());
            assertEquals("uuid-1", loaded.get(0).getPlayerUuid());
        }
        // data survives a reopen
        try (SqliteStorage storage = new SqliteStorage(folder.resolve("db.sqlite"), LOG,
                7 * 86400000L, 100, 1000)) {
            List<RecoveryRecord> loaded = storage.loadRecovery();
            assertEquals(1, loaded.size());
            storage.deleteRecovery("id-1");
            assertTrue(storage.loadRecovery().isEmpty());
        }
    }

    @Test
    void auditPagesNewestFirst() {
        try (SqliteStorage storage = new SqliteStorage(folder.resolve("audit.sqlite"), LOG,
                7 * 86400000L, 100, 1000)) {
            for (int i = 1; i <= 5; i++) {
                storage.appendAudit(new AuditRecord(i * 1000L, "uuid", "Steve",
                        AuditRecord.SET, "Bob", null, "DIAMOND", String.valueOf(i)));
            }
            List<AuditRecord> page = storage.auditPage(2, 0);
            assertEquals(2, page.size());
            assertEquals("5", page.get(0).getDetails());
            assertEquals("4", page.get(1).getDetails());
            List<AuditRecord> last = storage.auditPage(2, 2);
            assertEquals(1, last.size());
            assertEquals("1", last.get(0).getDetails());
        }
    }

    @Test
    void statsUpsertAccumulates() {
        try (SqliteStorage storage = new SqliteStorage(folder.resolve("stats.sqlite"), LOG,
                7 * 86400000L, 100, 1000)) {
            storage.incrementExpired("2026-10-26", "DIAMOND");
            storage.incrementExpired("2026-10-26", "DIAMOND");
            storage.incrementExpired("2026-10-26", "ELYTRA");
            Map<String, Map<String, Long>> stats = storage.expiredStats("2026-10-01");
            assertEquals(2L, stats.get("2026-10-26").get("DIAMOND"));
            assertEquals(1L, stats.get("2026-10-26").get("ELYTRA"));
        }
    }

    @Test
    void auditDetailsSurviveSpecialCharacters() {
        try (SqliteStorage storage = new SqliteStorage(folder.resolve("special.sqlite"), LOG,
                7 * 86400000L, 100, 1000)) {
            storage.appendAudit(new AuditRecord(1000L, null, "console", AuditRecord.GIVE,
                    "Steve|O'", null, "item\\x", "pipe|and\\slash"));
            AuditRecord record = storage.auditPage(1, 0).get(0);
            assertNull(record.getActorUuid());
            assertEquals("Steve|O'", record.getTarget());
            assertEquals("item\\x", record.getItemId());
            assertEquals("pipe|and\\slash", record.getDetails());
        }
    }

    @Test
    void auditTrimIsAmortizedButCorrect() {
        int trimEvery = SqlStorage.AUDIT_TRIM_EVERY;
        try (SqliteStorage storage = new SqliteStorage(folder.resolve("trim.sqlite"), LOG,
                7 * 86400000L, 100, 100)) {
            // insert past several trim points
            int first = 100 + trimEvery * 4;
            for (int i = 1; i <= first; i++) {
                storage.appendAudit(new AuditRecord(i * 1000L, "uuid", "Steve",
                        AuditRecord.SET, "Bob", null, "DIAMOND", String.valueOf(i)));
            }
            // the table may exceed the cap by up to one trim interval
            List<AuditRecord> all = storage.auditPage(10000, 0);
            assertTrue(all.size() > 100 && all.size() <= 100 + trimEvery,
                    "expected cap..cap+trimEvery rows, got " + all.size());
            assertEquals(String.valueOf(first), all.get(0).getDetails());

            // inserting up to the next trim point prunes to exactly the cap
            int toNextTrim = trimEvery - (first % trimEvery);
            for (int i = first + 1; i <= first + toNextTrim; i++) {
                storage.appendAudit(new AuditRecord(i * 1000L, "uuid", "Steve",
                        AuditRecord.SET, "Bob", null, "DIAMOND", String.valueOf(i)));
            }
            List<AuditRecord> trimmed = storage.auditPage(10000, 0);
            assertEquals(100, trimmed.size());
            assertEquals(String.valueOf(first + toNextTrim), trimmed.get(0).getDetails());
            assertEquals(String.valueOf(first + toNextTrim - 99),
                    trimmed.get(trimmed.size() - 1).getDetails());
        }
    }
}
