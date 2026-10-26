package me.albert.timeditem.storage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps the selected {@link Storage} with one background writer thread.
 * Fire-and-forget writes (recovery, audit, stats) are queued so expiry
 * can run on any region thread without touching disk or the network;
 * reads (startup load, command queries) block on the caller, which is
 * either the startup sequence or an async command task.
 */
public final class StorageWriter {

    private final Storage storage;
    private final Logger logger;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "TimedItem-StorageWriter");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean closed;

    public StorageWriter(Storage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    /** The backend behind this writer ("flat", "sqlite", "mysql", "mongodb"). */
    public String backendId() {
        return storage.id();
    }

    /** Blocking: called once at startup. */
    public List<RecoveryRecord> loadRecovery() {
        return storage.loadRecovery();
    }

    /** Fire-and-forget upsert of one recovery entry. */
    public void insertRecovery(RecoveryRecord record) {
        execute(() -> storage.insertRecovery(record));
    }

    /** Fire-and-forget delete of one restored recovery entry. */
    public void deleteRecovery(String id) {
        execute(() -> storage.deleteRecovery(id));
    }

    /** Fire-and-forget audit append. */
    public void appendAudit(AuditRecord record) {
        execute(() -> storage.appendAudit(record));
    }

    /** Fire-and-forget daily stats increment. */
    public void incrementExpired(String day, String material) {
        execute(() -> storage.incrementExpired(day, material));
    }

    /** Blocking query for command output - run it on an async thread. */
    public List<AuditRecord> auditPage(int pageSize, int pageIndex) {
        return storage.auditPage(pageSize, pageIndex);
    }

    /** Blocking query for command output - run it on an async thread. */
    public Map<String, Map<String, Long>> expiredStats(String sinceDay) {
        return storage.expiredStats(sinceDay);
    }

    public void close() {
        closed = true;
        writer.shutdown();
        try {
            writer.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        storage.close();
    }

    private void execute(Runnable task) {
        if (closed) {
            return;
        }
        writer.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Storage write failed: " + t, t);
            }
        });
    }
}
