package me.albert.timeditem.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Shared JDBC implementation for SQLite and MySQL/MariaDB. Owns the
 * Hikari pool, the schema (recovery / audit / stats tables) and every
 * statement; subclasses only supply the pool configuration and the two
 * SQL fragments where the dialects differ (DDL identity columns and the
 * stats upsert).
 *
 * All methods block; callers are the startup sequence or the single
 * background writer thread, so statement use stays serialized and the
 * pool mainly serves command-time queries (/ti stats, /ti audit).
 */
public abstract class SqlStorage implements Storage {

    protected final Logger logger;
    private final HikariDataSource dataSource;
    private final long cutoffMillis;
    private final int maxItems;
    private final int auditHistory;

    protected SqlStorage(HikariConfig poolConfig, Logger logger, long cutoffMillis, int maxItems,
                         int auditHistory) {
        this.logger = logger;
        this.cutoffMillis = cutoffMillis;
        this.maxItems = maxItems;
        this.auditHistory = Math.max(100, auditHistory);
        this.dataSource = new HikariDataSource(poolConfig);
        createSchema();
    }

    /** Identity column for the audit table: SQLite vs MySQL syntax. */
    protected abstract String auditIdColumn();

    /** Dialect upsert for the daily stats counter. */
    protected abstract String upsertStatsSql();

    @Override
    public List<RecoveryRecord> loadRecovery() {
        try (Connection connection = dataSource.getConnection()) {
            createTables(connection);
            try (PreparedStatement prune = connection.prepareStatement(
                    "DELETE FROM timeditem_recovery WHERE time_millis < ?")) {
                prune.setLong(1, System.currentTimeMillis() - cutoffMillis);
                prune.executeUpdate();
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM timeditem_recovery WHERE id NOT IN ("
                        + "SELECT id FROM timeditem_recovery ORDER BY time_millis DESC LIMIT "
                        + maxItems + ")");
            }
            List<RecoveryRecord> out = new ArrayList<RecoveryRecord>();
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id, time_millis, player_uuid, player_name, world, format, data"
                            + " FROM timeditem_recovery")) {
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        out.add(new RecoveryRecord(result.getString(1), result.getLong(2),
                                result.getString(3), result.getString(4), result.getString(5),
                                result.getString(6), result.getString(7)));
                    }
                }
            }
            return out;
        } catch (SQLException e) {
            logger.warning("Recovery table read failed: " + e.getMessage());
            return new ArrayList<RecoveryRecord>();
        }
    }

    @Override
    public void insertRecovery(RecoveryRecord record) {
        try (Connection connection = dataSource.getConnection()) {
            deleteRecovery(connection, record.getId());
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO timeditem_recovery (id, time_millis, player_uuid, player_name,"
                            + " world, format, data) VALUES (?,?,?,?,?,?,?)")) {
                insert.setString(1, record.getId());
                insert.setLong(2, record.getTimeMillis());
                insert.setString(3, record.getPlayerUuid());
                insert.setString(4, record.getPlayerName());
                insert.setString(5, record.getWorld());
                insert.setString(6, record.getFormat());
                insert.setString(7, record.getBase64());
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            logger.warning("Recovery insert failed: " + e.getMessage());
        }
    }

    @Override
    public void deleteRecovery(String id) {
        try (Connection connection = dataSource.getConnection()) {
            deleteRecovery(connection, id);
        } catch (SQLException e) {
            logger.warning("Recovery delete failed: " + e.getMessage());
        }
    }

    private void deleteRecovery(Connection connection, String id) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM timeditem_recovery WHERE id = ?")) {
            delete.setString(1, id);
            delete.executeUpdate();
        }
    }

    @Override
    public void appendAudit(AuditRecord record) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO timeditem_audit (time_millis, actor_uuid, actor_name, action,"
                            + " target, item_uid, item_id, details) VALUES (?,?,?,?,?,?,?,?)")) {
                insert.setLong(1, record.getTimeMillis());
                insert.setString(2, record.getActorUuid());
                insert.setString(3, record.getActorName());
                insert.setString(4, record.getAction());
                insert.setString(5, record.getTarget());
                insert.setString(6, record.getItemUid());
                insert.setString(7, record.getItemId());
                insert.setString(8, record.getDetails());
                insert.executeUpdate();
            }
            try (Statement trim = connection.createStatement()) {
                trim.executeUpdate("DELETE FROM timeditem_audit WHERE id NOT IN ("
                        + "SELECT id FROM timeditem_audit ORDER BY id DESC LIMIT "
                        + auditHistory + ")");
            }
        } catch (SQLException e) {
            logger.warning("Audit insert failed: " + e.getMessage());
        }
    }

    @Override
    public List<AuditRecord> auditPage(int pageSize, int pageIndex) {
        List<AuditRecord> out = new ArrayList<AuditRecord>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT time_millis, actor_uuid, actor_name, action, target, item_uid,"
                             + " item_id, details FROM timeditem_audit"
                             + " ORDER BY id DESC LIMIT ? OFFSET ?")) {
            select.setInt(1, pageSize);
            select.setInt(2, Math.max(0, pageIndex) * pageSize);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    out.add(new AuditRecord(result.getLong(1), result.getString(2),
                            result.getString(3), result.getString(4), result.getString(5),
                            result.getString(6), result.getString(7), result.getString(8)));
                }
            }
        } catch (SQLException e) {
            logger.warning("Audit query failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public void incrementExpired(String day, String material) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement upsert = connection.prepareStatement(upsertStatsSql())) {
            upsert.setString(1, day);
            upsert.setString(2, material);
            upsert.setLong(3, 1L);
            upsert.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Stats increment failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Map<String, Long>> expiredStats(String sinceDay) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<String, Map<String, Long>>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(
                     "SELECT day, material, count FROM timeditem_stats WHERE day >= ? ORDER BY day")) {
            select.setString(1, sinceDay);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    String day = result.getString(1);
                    Map<String, Long> perDay = out.get(day);
                    if (perDay == null) {
                        perDay = new LinkedHashMap<String, Long>();
                        out.put(day, perDay);
                    }
                    perDay.put(result.getString(2), result.getLong(3));
                }
            }
        } catch (SQLException e) {
            logger.warning("Stats query failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private void createSchema() {
        try (Connection connection = dataSource.getConnection()) {
            createTables(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create TimedItem schema: " + e.getMessage(), e);
        }
    }

    private void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS timeditem_recovery ("
                    + "id VARCHAR(64) PRIMARY KEY, time_millis BIGINT NOT NULL,"
                    + " player_uuid VARCHAR(64), player_name VARCHAR(64), world VARCHAR(64),"
                    + " format VARCHAR(8), data MEDIUMTEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS timeditem_audit ("
                    + "id " + auditIdColumn() + ", time_millis BIGINT NOT NULL,"
                    + " actor_uuid VARCHAR(64), actor_name VARCHAR(64), action VARCHAR(32),"
                    + " target VARCHAR(64), item_uid VARCHAR(64), item_id VARCHAR(255),"
                    + " details VARCHAR(255))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS timeditem_stats ("
                    + "day VARCHAR(10) NOT NULL, material VARCHAR(64) NOT NULL,"
                    + " count BIGINT NOT NULL, PRIMARY KEY (day, material))");
        }
    }
}
