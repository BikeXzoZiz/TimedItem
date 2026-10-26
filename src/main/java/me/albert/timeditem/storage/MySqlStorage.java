package me.albert.timeditem.storage;

import com.zaxxer.hikari.HikariConfig;

import java.util.logging.Logger;

/**
 * MySQL backend (Settings.storage.type: mysql) through the MariaDB
 * driver, which speaks the MySQL protocol with an Apache 2.0 license.
 * Used by networks that share one database across servers.
 */
public final class MySqlStorage extends SqlStorage {

    public MySqlStorage(String host, int port, String database, String user, String password,
                        boolean useSsl, int poolSize, Logger logger, long cutoffMillis, int maxItems,
                        int auditHistory) {
        super(pool(host, port, database, user, password, useSsl, poolSize), logger, cutoffMillis,
                maxItems, auditHistory);
    }

    private static HikariConfig pool(String host, int port, String database, String user,
                                     String password, boolean useSsl, int poolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useSsl=" + useSsl + "&allowPublicKeyRetrieval=true");
        config.setUsername(user);
        config.setPassword(password == null ? "" : password);
        config.setMaximumPoolSize(Math.max(1, poolSize));
        config.setPoolName("TimedItem-MySQL");
        // wait briefly: a missing database server should fail fast into
        // the flat-file fallback, not stall the startup for minutes
        config.setInitializationFailTimeout(10_000L);
        return config;
    }

    @Override
    protected String auditIdColumn() {
        return "BIGINT PRIMARY KEY AUTO_INCREMENT";
    }

    @Override
    protected String upsertStatsSql() {
        return "INSERT INTO timeditem_stats (day, material, count) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE count = count + VALUES(count)";
    }

    @Override
    public String id() {
        return "mysql";
    }
}
