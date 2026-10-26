package me.albert.timeditem.storage;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.RecoveryFormat;
import me.albert.timeditem.TimedItemPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates the Storage backend selected by Settings.storage.type and
 * migrates the flat recovery.log into a new database on first use.
 * A backend that cannot connect (no server, bad credentials) never
 * kills the plugin: the factory logs loudly and falls back to the
 * flat files, exactly like 2.6 ran.
 */
public final class StorageFactory {

    private StorageFactory() {
    }

    public static Storage create(TimedItemPlugin plugin) {
        PluginConfig config = plugin.pluginConfig();
        Logger logger = plugin.getLogger();
        String type = config.getStorageType();
        if ("flat".equals(type) || type == null || type.isEmpty()) {
            return flat(plugin, logger);
        }
        try {
            Storage storage = createBackend(plugin, type);
            migrateFlatLogInto(plugin, storage);
            logger.info("Storage backend: " + type + ".");
            return storage;
        } catch (Throwable t) {
            logger.warning("Could not use storage type '" + type + "' (" + t.getMessage()
                    + ") - falling back to flat files. Fix Settings.storage.* and restart.");
            return flat(plugin, logger);
        }
    }

    private static Storage createBackend(TimedItemPlugin plugin, String type) {
        PluginConfig config = plugin.pluginConfig();
        Logger logger = plugin.getLogger();
        long cutoffMillis = config.getRecoveryKeepDays() * 86400000L;
        if ("sqlite".equals(type)) {
            Path file = plugin.getDataFolder().toPath().resolve(
                    config.getSqliteFile() == null ? "database.db" : config.getSqliteFile());
            return new SqliteStorage(file, logger, cutoffMillis,
                    config.getRecoveryMaxItems(), config.getAuditHistory());
        }
        if ("mysql".equals(type)) {
            return new MySqlStorage(config.getMysqlHost(), config.getMysqlPort(),
                    config.getMysqlDatabase(), config.getMysqlUser(), config.getMysqlPassword(),
                    config.isMysqlUseSsl(), config.getMysqlPoolSize(), logger, cutoffMillis,
                    config.getRecoveryMaxItems(), config.getAuditHistory());
        }
        if ("mongodb".equals(type)) {
            return new MongoStorage(config.getMongodbUri(), config.getMongodbDatabase(), logger,
                    cutoffMillis, config.getRecoveryMaxItems(), config.getAuditHistory());
        }
        throw new IllegalArgumentException("unknown storage type");
    }

    private static Storage flat(TimedItemPlugin plugin, Logger logger) {
        PluginConfig config = plugin.pluginConfig();
        return new FlatFileStorage(plugin.getDataFolder().toPath(), logger,
                config.getRecoveryKeepDays() * 86400000L,
                config.getRecoveryMaxItems(), config.getAuditHistory());
    }

    /**
     * One-time import: when a database backend starts and the old
     * recovery.log still exists, its entries are inserted into the
     * backend and the file is renamed to recovery.log.imported so the
     * data is never lost and never imported twice.
     */
    private static void migrateFlatLogInto(TimedItemPlugin plugin, Storage storage) {
        Path file = plugin.getDataFolder().toPath().resolve("recovery.log");
        if (!Files.exists(file)) {
            return;
        }
        int imported = 0;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                RecoveryFormat.Parsed parsed = RecoveryFormat.parse(line);
                if (parsed == null) {
                    continue;
                }
                storage.insertRecovery(parsed.record);
                imported++;
            }
            Files.move(file, file.resolveSibling("recovery.log.imported"),
                    StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Imported " + imported + " recovery log entries into "
                    + storage.id() + " (recovery.log -> recovery.log.imported).");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not import recovery.log into "
                    + storage.id() + ": " + e.getMessage());
        }
    }
}
