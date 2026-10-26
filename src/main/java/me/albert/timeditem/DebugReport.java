package me.albert.timeditem;

import org.bukkit.Bukkit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds the /ti debug export bug report.
 *
 * snapshot() gathers live state on the calling thread (a command runs on
 * the main thread or a region thread); render() turns the plain-data
 * snapshot plus the captured log lines into the report text and can run
 * on any thread - including the async writer and the tests.
 *
 * The report is deliberately conservative about privacy: the Discord
 * webhook URL is never included, and recovery entries appear only as a
 * count - no item data, no player names.
 */
public final class DebugReport {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DebugReport() {
    }

    /** Plain data for one report; public fields keep the pure render testable. */
    public static final class Snapshot {
        public String pluginVersion = "";
        public String serverVersion = "";
        public String apiVersion = "";
        public boolean folia;
        public String javaVersion = "";
        public boolean pdcAvailable;
        public String itemFormat = "";
        public boolean placeholderApi;
        public String hooks = "";
        public int onlinePlayers;
        public int worldCount;

        public String language = "en";
        public boolean debugMode;
        public boolean expireCheckEnabled;
        public int expireCheckSeconds;
        public boolean scanWorldItems;
        public boolean ignoreCreative;
        public boolean warningEnabled;
        public boolean actionbarEnabled;
        public boolean metricsEnabled;
        public boolean updateCheckEnabled;
        public boolean recoveryEnabled;
        public int recoveryMaxItems;
        public int recoveryKeepDays;
        public boolean webhookEnabled;
        public int blacklistSize;

        public int recoveryEntries;
        public boolean recoveryFileExists;
        public long recoveryFileBytes;
        public String recoveryFilePath = "";

        public String generatedAt = "";
    }

    /** Gathers the current state; call on the main thread or a region thread. */
    public static Snapshot snapshot(TimedItemPlugin plugin) {
        Snapshot s = new Snapshot();
        PluginConfig config = plugin.pluginConfig();
        s.pluginVersion = plugin.getDescription().getVersion();
        s.serverVersion = Bukkit.getVersion();
        s.apiVersion = Bukkit.getBukkitVersion();
        s.folia = SchedulerCompat.isFolia();
        s.javaVersion = System.getProperty("java.version", "unknown");
        s.pdcAvailable = PdcCompat.isAvailable();
        s.itemFormat = ItemSerializer.currentFormat();
        s.placeholderApi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        s.hooks = plugin.hooks() == null ? "" : plugin.hooks().summary();
        s.onlinePlayers = Bukkit.getOnlinePlayers().size();
        s.worldCount = Bukkit.getWorlds().size();

        s.language = plugin.getConfig().getString("Settings.language", "en");
        s.debugMode = DebugLog.isEnabled();
        s.expireCheckEnabled = config.isExpireCheckEnabled();
        s.expireCheckSeconds = config.getExpireCheckSeconds();
        s.scanWorldItems = config.isScanWorldItems();
        s.ignoreCreative = config.isIgnoreCreative();
        s.warningEnabled = config.isWarningEnabled();
        s.actionbarEnabled = config.isActionbarEnabled();
        s.metricsEnabled = config.isMetricsEnabled();
        s.updateCheckEnabled = config.isUpdateCheckEnabled();
        s.recoveryEnabled = config.isRecoveryEnabled();
        s.recoveryMaxItems = config.getRecoveryMaxItems();
        s.recoveryKeepDays = config.getRecoveryKeepDays();
        s.webhookEnabled = config.isDiscordWebhookEnabled();
        s.blacklistSize = config.blacklistedMaterials().size();

        s.recoveryEntries = plugin.recoveryStore().size();
        Path recoveryFile = plugin.recoveryStore().file();
        s.recoveryFilePath = recoveryFile.toString();
        s.recoveryFileExists = Files.exists(recoveryFile);
        if (s.recoveryFileExists) {
            try {
                s.recoveryFileBytes = Files.size(recoveryFile);
            } catch (Exception e) {
                s.recoveryFileBytes = -1;
            }
        }
        s.generatedAt = LocalDateTime.now().format(TIMESTAMP);
        return s;
    }

    /** Name for one report file, e.g. report-2026-10-26_18-04-59.txt. */
    public static String fileName() {
        return "report-" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt";
    }

    /** Renders the report; pure text work, safe on any thread. */
    public static String render(Snapshot s, List<String> recentLog) {
        StringBuilder out = new StringBuilder();
        out.append("================================================================\n");
        out.append(" TimedItem debug report\n");
        out.append(" Generated: ").append(s.generatedAt).append('\n');
        out.append(" The Discord webhook URL is never included in reports.\n");
        out.append("================================================================\n");

        out.append("\n[Environment]\n");
        out.append("plugin_version: ").append(s.pluginVersion).append('\n');
        out.append("server: ").append(s.serverVersion).append('\n');
        out.append("bukkit_api: ").append(s.apiVersion).append('\n');
        out.append("folia: ").append(s.folia).append('\n');
        out.append("java: ").append(s.javaVersion).append('\n');
        out.append("item_storage: ").append(s.pdcAvailable ? "PDC" : "lore")
                .append(" (recovery format tag: ").append(s.itemFormat).append(")\n");
        out.append("placeholderapi: ").append(s.placeholderApi).append('\n');
        out.append("hooks: ").append(s.hooks).append('\n');
        out.append("online_players: ").append(s.onlinePlayers).append('\n');
        out.append("worlds: ").append(s.worldCount).append('\n');

        out.append("\n[Settings]\n");
        out.append("language: ").append(s.language).append('\n');
        out.append("debug: ").append(s.debugMode).append('\n');
        out.append("expire_check.enabled: ").append(s.expireCheckEnabled).append('\n');
        out.append("expire_check.interval_seconds: ").append(s.expireCheckSeconds).append('\n');
        out.append("expire_check.scan_world_items: ").append(s.scanWorldItems).append('\n');
        out.append("expire_check.ignore_creative: ").append(s.ignoreCreative).append('\n');
        out.append("warning.enabled: ").append(s.warningEnabled).append('\n');
        out.append("actionbar.enabled: ").append(s.actionbarEnabled).append('\n');
        out.append("metrics: ").append(s.metricsEnabled).append('\n');
        out.append("update_check.enabled: ").append(s.updateCheckEnabled).append('\n');
        out.append("recovery.enabled: ").append(s.recoveryEnabled).append('\n');
        out.append("recovery.max_items: ").append(s.recoveryMaxItems).append('\n');
        out.append("recovery.keep_days: ").append(s.recoveryKeepDays).append('\n');
        out.append("discord_webhook.enabled: ").append(s.webhookEnabled).append('\n');
        out.append("blacklist.materials.size: ").append(s.blacklistSize).append('\n');

        out.append("\n[Recovery]\n");
        out.append("entries: ").append(s.recoveryEntries).append('\n');
        out.append("file: ").append(s.recoveryFilePath);
        if (s.recoveryFileExists) {
            out.append(" (exists, ").append(s.recoveryFileBytes).append(" bytes)");
        } else {
            out.append(" (missing)");
        }
        out.append('\n');

        out.append("\n[Recent log (oldest first, last ").append(recentLog.size()).append(" lines)]\n");
        for (String line : recentLog) {
            out.append(line).append('\n');
        }
        out.append("\n[End of report]\n");
        return out.toString();
    }
}
