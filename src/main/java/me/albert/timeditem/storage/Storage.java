package me.albert.timeditem.storage;

import java.util.List;
import java.util.Map;

/**
 * Persistence backend for TimedItem's own data: the recovery log, the
 * audit trail and the daily expiry statistics. One implementation per
 * storage type (flat file, SQLite, MySQL/MariaDB, MongoDB); selected
 * once on startup through StorageFactory.
 *
 * Every method may block - callers either run at startup or on the
 * background writer thread (StorageWriter), never on a main or region
 * thread. Implementations must be safe when the writer thread and a
 * command-time query race; synchronize internally where needed.
 */
public interface Storage extends AutoCloseable {

    /** Config identifier: "flat", "sqlite", "mysql" or "mongodb". */
    String id();

    /**
     * Reads every recovery entry, already pruned by keep_days and
     * max_items. Called once at startup.
     */
    List<RecoveryRecord> loadRecovery();

    /** Inserts (or replaces, by id) one recovery entry. */
    void insertRecovery(RecoveryRecord record);

    /** Deletes one recovery entry (it was restored to a player). */
    void deleteRecovery(String id);

    /**
     * Newest-first recovery entries of one player uuid, up to limit
     * (3.1+, network restore across servers). Blocking - run on an async
     * thread. Flat storage filters its local list; SQL/Mongo query the
     * shared database.
     */
    List<RecoveryRecord> loadRecoveryFor(String playerUuid, int limit);

    /** How many recovery entries one player uuid has (restore reminder). */
    int countRecoveryFor(String playerUuid);

    /** Appends one audit entry. */
    void appendAudit(AuditRecord record);

    /**
     * One page of audit entries, newest first. Kept bounded by the
     * backend (audit_history setting).
     */
    List<AuditRecord> auditPage(int pageSize, int pageIndex);

    /** Counts how many expired items of each material happened per day. */
    void incrementExpired(String day, String material);

    /**
     * Expired counts per day since (and including) the given
     * yyyy-MM-dd: day -> material -> count.
     */
    Map<String, Map<String, Long>> expiredStats(String sinceDay);

    /**
     * Writes deferred changes to durable storage. Flat file storage runs
     * write-behind (mutations mark files dirty, this rewrites them) and
     * is flushed at most once per second by StorageWriter plus once on
     * close; SQL and MongoDB apply every write immediately, so this is a
     * no-op for them. Called on the background writer thread.
     */
    default void flush() {
    }

    /** Releases connections and threads; called on disable. */
    void close();
}
