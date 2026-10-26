package me.albert.timeditem.storage;

import com.zaxxer.hikari.HikariConfig;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * SQLite backend (Settings.storage.type: sqlite). Single-connection
 * pool - SQLite does not benefit from more and file locking punishes
 * concurrent writers.
 */
public final class SqliteStorage extends SqlStorage {

    public SqliteStorage(Path databaseFile, Logger logger, long cutoffMillis, int maxItems,
                         int auditHistory) {
        super(pool(databaseFile), logger, cutoffMillis, maxItems, auditHistory);
    }

    private static HikariConfig pool(Path databaseFile) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setPoolName("TimedItem-SQLite");
        return config;
    }

    @Override
    protected String auditIdColumn() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    protected String upsertStatsSql() {
        return "INSERT INTO timeditem_stats (day, material, count) VALUES (?,?,?) "
                + "ON CONFLICT(day, material) DO UPDATE SET count = count + excluded.count";
    }

    @Override
    public String id() {
        return "sqlite";
    }
}
