package me.albert.timeditem.storage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps the selected {@link Storage} with one background writer thread.
 * Fire-and-forget writes (recovery, audit, stats) are queued so expiry
 * can run on any region thread without touching disk or the network;
 * reads (startup load, command queries) block on the caller, which is
 * either the startup sequence or an async command task.
 *
 * Write-behind backends (flat files) additionally get a flush handed
 * into the writer queue at most once per second.
 */
public final class StorageWriter {

    /** Minimum gap between two deferred flushes of the backend. */
    private static final long FLUSH_DELAY_MILLIS = 1000;

    private final Storage storage;
    private final Logger logger;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "TimedItem-StorageWriter");
        thread.setDaemon(true);
        return thread;
    });
    /** Schedules the periodic {@link #flushNow()} that feeds {@link #writer}. */
    private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "TimedItem-StorageFlush");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean flushQueued = new AtomicBoolean();
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

    /**
     * Blocking per-player recovery query (network restore) - the caller
     * must run it on an async thread.
     */
    public List<RecoveryRecord> loadRecoveryFor(String playerUuid, int limit) {
        return storage.loadRecoveryFor(playerUuid, limit);
    }

    /** Blocking per-player recovery count - run it on an async thread. */
    public int countRecoveryFor(String playerUuid) {
        return storage.countRecoveryFor(playerUuid);
    }

    /** Fire-and-forget audit append. */
    public void appendAudit(AuditRecord record) {
        execute(() -> storage.appendAudit(record));
    }

    /** Fire-and-forget daily stats increment. */
    public void incrementExpired(String day, String material) {
        execute(() -> storage.incrementExpired(day, material));
    }

    /**
     * Fire-and-forget custom write-path task, run on the writer thread.
     * Used by RecoveryStore to keep the heavy NBT encode off expiry
     * threads; goes through the same logging and flush scheduling as
     * every other write.
     */
    public void runWrite(Runnable task) {
        execute(task);
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
        flusher.shutdownNow();
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
        try {
            writer.execute(() -> {
                try {
                    task.run();
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Storage write failed: " + t, t);
                }
            });
            scheduleFlush();
        } catch (RejectedExecutionException shuttingDown) {
            // close() ran between the flag check and the enqueue; the
            // write is lost with the rest of the shutdown work
        }
    }

    /**
     * Queues one {@code storage.flush()} into the writer queue at most
     * once per second: mutations mark backend state dirty, the flush
     * writes it out. The CAS guard keeps a whole burst of writes to a
     * single scheduled flush.
     */
    private void scheduleFlush() {
        if (closed || !flushQueued.compareAndSet(false, true)) {
            return;
        }
        try {
            flusher.schedule(this::flushNow, FLUSH_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException shuttingDown) {
            flushQueued.set(false);
        }
    }

    /** Runs on the flusher thread; hands the actual write to the writer queue. */
    private void flushNow() {
        flushQueued.set(false);
        if (closed) {
            return;
        }
        try {
            writer.execute(() -> {
                try {
                    storage.flush();
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Storage flush failed: " + t, t);
                }
            });
        } catch (RejectedExecutionException shuttingDown) {
            // writer already shut down; close() flushes via storage.close()
        }
    }
}
