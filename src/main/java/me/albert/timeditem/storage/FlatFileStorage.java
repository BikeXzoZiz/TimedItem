package me.albert.timeditem.storage;

import me.albert.timeditem.RecoveryFormat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default backend: plain files in plugins/TimedItem/.
 *
 * recovery.log keeps the line format documented in {@link me.albert.timeditem.RecoveryFormat}
 * (v3 lines carry the entry id and the player UUID; v2/v1 lines still load).
 * audit.log stores one line per audit entry, newest last, trimmed to the
 * audit_history cap. stats.yml maps day -> material -> count and is only
 * rewritten when a day's numbers change.
 *
 * All state is kept in memory and rewritten atomically on change; every
 * method is synchronized because the writer thread and command-time
 * queries (e.g. /ti stats) can race.
 */
public final class FlatFileStorage implements Storage {

    private final Path dataFolder;
    private final Logger logger;
    private final long cutoffMillis;
    private final int maxItems;
    private final int auditHistory;
    private final List<RecoveryRecord> records = new ArrayList<RecoveryRecord>();
    private final List<AuditRecord> audit = new ArrayList<AuditRecord>();
    /** day -> (material -> count); insertion order = chronological. */
    private final Map<String, Map<String, Long>> stats = new LinkedHashMap<String, Map<String, Long>>();

    public FlatFileStorage(Path dataFolder, Logger logger, long cutoffMillis, int maxItems,
                           int auditHistory) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.cutoffMillis = cutoffMillis;
        this.maxItems = maxItems;
        this.auditHistory = Math.max(100, auditHistory);
        loadStats();
        loadAudit();
    }

    @Override
    public String id() {
        return "flat";
    }

    @Override
    public synchronized List<RecoveryRecord> loadRecovery() {
        records.clear();
        Path file = dataFolder.resolve("recovery.log");
        if (Files.exists(file)) {
            long cutoff = System.currentTimeMillis() - cutoffMillis;
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    RecoveryFormat.Parsed parsed = RecoveryFormat.parse(line);
                    if (parsed == null || parsed.record.getTimeMillis() < cutoff) {
                        continue;
                    }
                    records.add(parsed.record);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not read recovery log: " + e);
            }
        }
        trimRecovery();
        writeRecovery();
        return new ArrayList<RecoveryRecord>(records);
    }

    @Override
    public synchronized void insertRecovery(RecoveryRecord record) {
        deleteById(record.getId());
        records.add(record);
        trimRecovery();
        writeRecovery();
    }

    @Override
    public synchronized void deleteRecovery(String id) {
        if (deleteById(id)) {
            writeRecovery();
        }
    }

    @Override
    public synchronized List<RecoveryRecord> loadRecoveryFor(String playerUuid, int limit) {
        // flat storage is local-only: this mirrors the local list
        List<RecoveryRecord> out = new ArrayList<RecoveryRecord>();
        for (int i = records.size() - 1; i >= 0 && out.size() < limit; i--) {
            RecoveryRecord record = records.get(i);
            if (playerUuid.equals(record.getPlayerUuid())) {
                out.add(record);
            }
        }
        return out;
    }

    @Override
    public synchronized int countRecoveryFor(String playerUuid) {
        int count = 0;
        for (RecoveryRecord record : records) {
            if (playerUuid.equals(record.getPlayerUuid())) {
                count++;
            }
        }
        return count;
    }

    private boolean deleteById(String id) {
        boolean changed = false;
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).getId().equals(id)) {
                records.remove(i);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public synchronized void appendAudit(AuditRecord record) {
        audit.add(record);
        while (audit.size() > auditHistory) {
            audit.remove(0);
        }
        writeAudit();
    }

    @Override
    public synchronized List<AuditRecord> auditPage(int pageSize, int pageIndex) {
        List<AuditRecord> out = new ArrayList<AuditRecord>();
        int start = audit.size() - 1 - Math.max(0, pageIndex) * pageSize;
        for (int i = start; i >= 0 && out.size() < pageSize; i--) {
            out.add(audit.get(i));
        }
        return out;
    }

    @Override
    public synchronized void incrementExpired(String day, String material) {
        Map<String, Long> perDay = stats.get(day);
        if (perDay == null) {
            perDay = new LinkedHashMap<String, Long>();
            stats.put(day, perDay);
        }
        Long count = perDay.get(material);
        perDay.put(material, (count == null ? 0L : count) + 1L);
        writeStats();
    }

    @Override
    public synchronized Map<String, Map<String, Long>> expiredStats(String sinceDay) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<String, Map<String, Long>>();
        for (Map.Entry<String, Map<String, Long>> day : stats.entrySet()) {
            if (day.getKey().compareTo(sinceDay) >= 0) {
                out.put(day.getKey(), new LinkedHashMap<String, Long>(day.getValue()));
            }
        }
        return out;
    }

    @Override
    public void close() {
        // files are written through on every change - nothing to flush
    }

    private void trimRecovery() {
        while (records.size() > maxItems) {
            records.remove(0);
        }
    }

    /** Full rewrite with atomic move - same durability model as 2.6. */
    private void writeRecovery() {
        StringBuilder out = new StringBuilder();
        for (RecoveryRecord record : records) {
            out.append(RecoveryFormat.format(record)).append('\n');
        }
        writeAtomically(dataFolder.resolve("recovery.log"), out.toString());
    }

    private void writeAudit() {
        writeAtomically(dataFolder.resolve("audit.log"), AuditFormat.serialize(audit));
    }

    private void loadAudit() {
        Path file = dataFolder.resolve("audit.log");
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<AuditRecord> parsed = AuditFormat.parse(Files.readAllLines(file, StandardCharsets.UTF_8));
            audit.addAll(parsed.subList(Math.max(0, parsed.size() - auditHistory), parsed.size()));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not read audit log: " + e);
        }
    }

    private void writeStats() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Map<String, Long>> day : stats.entrySet()) {
            for (Map.Entry<String, Long> material : day.getValue().entrySet()) {
                yaml.set("stats." + day.getKey() + "." + material.getKey(), material.getValue());
            }
        }
        try {
            Files.createDirectories(dataFolder);
            yaml.save(dataFolder.resolve("stats.yml").toFile());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not write stats.yml: " + e);
        }
    }

    private void writeAtomically(Path file, String content) {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not write " + file.getFileName() + ": " + e);
        }
    }

    /** Reads stats.yml back; called once from the constructor. Package private for tests. */
    void loadStats() {
        Path file = dataFolder.resolve("stats.yml");
        if (!Files.exists(file)) {
            return;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
            ConfigurationSection days = yaml.getConfigurationSection("stats");
            if (days == null) {
                return;
            }
            for (String day : days.getKeys(false)) {
                ConfigurationSection materials = days.getConfigurationSection(day);
                if (materials == null) {
                    continue;
                }
                Map<String, Long> perDay = new LinkedHashMap<String, Long>();
                for (String material : materials.getKeys(false)) {
                    perDay.put(material, materials.getLong(material));
                }
                stats.put(day, perDay);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not read stats.yml: " + e);
        }
    }
}
