package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of config.yml (Settings.* and Hooks.*). Built by
 * {@link ConfigLoader}; values are validated and clamped once while the
 * snapshot is built, so readers never see an inconsistent state and a
 * /ti reload can swap the whole instance atomically.
 */
final class Settings {

    /** Hook ids that can be switched off under Hooks.* in config.yml. */
    private static final String[] HOOK_IDS = {
            "mmoitems", "itemsadder", "oraxen", "nexo", "executableitems",
            "slimefun", "ecoitems", "shopkeepers", "crazycrates",
            "quickshop", "zauctionhouse", "crazyauctions", "zshop", "husksync"
    };

    final boolean startDateCalculateOnClick;
    final boolean resetPlayerHealthIfArmor;
    final boolean metricsEnabled;
    final boolean debugMode;
    final boolean expireCheckEnabled;
    final int expireCheckSeconds;
    final boolean scanWorldItems;
    final boolean ignoreCreative;
    final boolean warningEnabled;
    final int warningSeconds;
    /** Warning thresholds in seconds, highest first. */
    final List<Integer> warningStages;
    final String warningSound;
    final float warningVolume;
    final float warningPitch;
    final boolean particleEnabled;
    final String particleName;
    final int particleCount;
    final List<String> expiryCommands;
    final boolean actionbarEnabled;
    final String actionbarFormat;
    final boolean soundEnabled;
    final String soundName;
    final float soundVolume;
    final float soundPitch;
    final boolean updateCheckEnabled;
    final String updateCheckRepository;
    final boolean recoveryEnabled;
    final int recoveryMaxItems;
    final int recoveryKeepDays;
    final String storageType;
    final String mysqlHost;
    final int mysqlPort;
    final String mysqlDatabase;
    final String mysqlUser;
    final String mysqlPassword;
    final boolean mysqlUseSsl;
    final int mysqlPoolSize;
    final String sqliteFile;
    final String mongodbUri;
    final String mongodbDatabase;
    final int auditHistory;
    final boolean discordWebhookEnabled;
    final String discordWebhookUrl;
    final String discordWebhookUsername;
    final String discordWebhookMessage;
    final boolean discordWebhookUseEmbed;
    final String discordWebhookEmbedTitle;
    final int discordWebhookEmbedColor;

    final boolean expiryTransformEnabled;
    final String expiryTransformDefault;
    final Map<String, Transforms.Profile> transformProfiles;
    final boolean graceEnabled;
    final int graceSeconds;
    final boolean renewalEnabled;
    final long renewalAddSeconds;
    final Material renewalTokenMaterial;
    final String renewalTokenCustomId;
    final String renewalTokenName;

    final List<VisualStages.Stage> visualStages;
    final boolean hologramEnabled;
    final String hologramFormat;
    final double hologramHeight;
    final boolean liveLoreEnabled;
    final String liveLoreFormat;
    final boolean containerScanEnabled;
    final String containerScanMode;
    final int containerScanSeconds;
    final boolean antiHideEnabled;
    final long antiHideBelowSeconds;
    final boolean returnToOwnerEnabled;
    final boolean removeTimerOnReturn;
    final boolean restoreNetworkEnabled;

    final Set<Material> blacklistedMaterials;
    final Set<String> disabledHooks;
    final String shopkeepersAutoTimer;
    final String crazycratesAutoTimer;

    Settings(FileConfiguration raw, TimedItemPlugin plugin, Map<String, Long> unitSeconds) {
        startDateCalculateOnClick = raw.getBoolean("Settings.start_date_calculate_on_click", true);
        resetPlayerHealthIfArmor = raw.getBoolean("Settings.reset_player_health_if_armor", true);
        metricsEnabled = raw.getBoolean("Settings.metrics", true);
        debugMode = raw.getBoolean("Settings.debug", false);
        expireCheckEnabled = raw.getBoolean("Settings.expire_check.enabled", true);
        expireCheckSeconds = Math.max(1, raw.getInt("Settings.expire_check.interval_seconds", 10));
        scanWorldItems = raw.getBoolean("Settings.expire_check.scan_world_items", false);
        ignoreCreative = raw.getBoolean("Settings.expire_check.ignore_creative", true);
        discordWebhookEnabled = raw.getBoolean("Settings.discord_webhook.enabled", false);
        String webhookUrl = raw.getString("Settings.discord_webhook.url", "");
        discordWebhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        discordWebhookUsername = raw.getString("Settings.discord_webhook.username", "TimedItem");
        discordWebhookMessage = raw.getString("Settings.discord_webhook.message",
                "{player}'s timed item expired: {item} ({world})");
        warningEnabled = raw.getBoolean("Settings.warning.enabled", true);
        warningSeconds = Math.max(1, raw.getInt("Settings.warning.seconds", 60));
        // Multiple thresholds fire one warning each as the item counts
        // down (e.g. [600, 60, 10]); an empty list falls back to
        // Settings.warning.seconds so old configs keep working.
        List<Integer> stages = new ArrayList<Integer>();
        for (int stage : raw.getIntegerList("Settings.warning.stages")) {
            if (stage > 0 && !stages.contains(stage)) {
                stages.add(stage);
            }
        }
        if (stages.isEmpty()) {
            stages.add(warningSeconds);
        }
        Collections.sort(stages, Collections.reverseOrder());
        warningStages = Collections.unmodifiableList(stages);
        warningSound = raw.getString("Settings.warning.sound.name");
        warningVolume = (float) raw.getDouble("Settings.warning.sound.volume", 1.0);
        warningPitch = (float) raw.getDouble("Settings.warning.sound.pitch", 1.5);
        particleEnabled = raw.getBoolean("Settings.particles.enabled", true);
        particleName = raw.getString("Settings.particles.name");
        particleCount = Math.max(1, raw.getInt("Settings.particles.count", 30));
        expiryCommands = raw.getStringList("Settings.expiry_commands");
        actionbarEnabled = raw.getBoolean("Settings.actionbar.enabled", true);
        actionbarFormat = ConfigLoader.color(raw.getString("Settings.actionbar.format"));
        soundEnabled = raw.getBoolean("Settings.sound.enabled", true);
        soundName = raw.getString("Settings.sound.name");
        soundVolume = (float) raw.getDouble("Settings.sound.volume", 10.0);
        soundPitch = (float) raw.getDouble("Settings.sound.pitch", 1.2);
        updateCheckEnabled = raw.getBoolean("Settings.update_check.enabled", true);
        String repository = raw.getString("Settings.update_check.repository", "BikeXzoZiz/TimedItem");
        updateCheckRepository = repository == null || repository.trim().isEmpty()
                ? "BikeXzoZiz/TimedItem" : repository.trim();
        recoveryEnabled = raw.getBoolean("Settings.recovery.enabled", true);
        recoveryMaxItems = Math.max(1, raw.getInt("Settings.recovery.max_items", 500));
        recoveryKeepDays = Math.max(1, raw.getInt("Settings.recovery.keep_days", 7));

        String type = raw.getString("Settings.storage.type", "flat");
        storageType = type == null ? "flat" : type.trim().toLowerCase(Locale.ROOT);
        mysqlHost = raw.getString("Settings.storage.mysql.host", "localhost");
        mysqlPort = raw.getInt("Settings.storage.mysql.port", 3306);
        mysqlDatabase = raw.getString("Settings.storage.mysql.database", "timeditem");
        mysqlUser = raw.getString("Settings.storage.mysql.user", "timeditem");
        mysqlPassword = raw.getString("Settings.storage.mysql.password", "");
        mysqlUseSsl = raw.getBoolean("Settings.storage.mysql.use_ssl", false);
        mysqlPoolSize = Math.max(1, raw.getInt("Settings.storage.mysql.pool_size", 4));
        sqliteFile = raw.getString("Settings.storage.sqlite.file", "database.db");
        mongodbUri = raw.getString("Settings.storage.mongodb.uri", "mongodb://localhost:27017");
        mongodbDatabase = raw.getString("Settings.storage.mongodb.database", "timeditem");
        auditHistory = Math.max(100, raw.getInt("Settings.storage.audit_history", 5000));
        Set<Material> blacklisted = new HashSet<Material>();
        for (String name : raw.getStringList("Settings.blacklist.materials")) {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                plugin.getLogger().warning("Ignoring unknown blacklisted material: " + name);
            } else {
                blacklisted.add(material);
            }
        }
        blacklistedMaterials = Collections.unmodifiableSet(blacklisted);

        Set<String> disabled = new HashSet<String>();
        for (String hook : HOOK_IDS) {
            if (!raw.getBoolean("Hooks." + hook, true)) {
                disabled.add(hook);
            }
        }
        disabledHooks = Collections.unmodifiableSet(disabled);
        String shopTimer = raw.getString("Hooks.shopkeepers.auto_timer", "");
        shopkeepersAutoTimer = shopTimer == null ? "" : shopTimer.trim();
        String crateTimer = raw.getString("Hooks.crazycrates.auto_timer", "");
        crazycratesAutoTimer = crateTimer == null ? "" : crateTimer.trim();

        // Expiry transformation (2.8+): profiles in Settings.transforms
        expiryTransformEnabled = raw.getBoolean("Settings.expiry_transform.enabled", false);
        String defaultTransform = raw.getString("Settings.expiry_transform.default", "");
        expiryTransformDefault = defaultTransform == null ? "" : defaultTransform.trim();
        transformProfiles = Transforms.unmodifiable(Transforms.parse(
                raw.getConfigurationSection("Settings.transforms"), plugin));

        // Grace period: item becomes unusable in its final seconds
        graceEnabled = raw.getBoolean("Settings.grace.enabled", false);
        graceSeconds = Math.max(1, raw.getInt("Settings.grace.seconds", 30));

        // Renewal token: right-click to extend a timed item
        boolean renewal = raw.getBoolean("Settings.renewal.enabled", false);
        String addTime = raw.getString("Settings.renewal.add_time", "7d");
        // parsed with the language file's unit labels, same as /ti set|add
        long addSeconds = addTime == null ? 0L : DurationParser.parse(addTime.trim(), unitSeconds);
        if (renewal && addSeconds <= 0) {
            plugin.getLogger().warning("Settings.renewal.add_time '" + addTime
                    + "' is not a valid duration - renewal disabled.");
            renewal = false;
        }
        String tokenMaterial = raw.getString("Settings.renewal.token.material", "CLOCK");
        Material tokenMat = tokenMaterial == null || tokenMaterial.trim().isEmpty()
                ? null : Material.matchMaterial(tokenMaterial.trim());
        if (renewal && tokenMat == null) {
            plugin.getLogger().warning("Settings.renewal.token.material '" + tokenMaterial
                    + "' is unknown - renewal disabled.");
            renewal = false;
        }
        String tokenCustom = raw.getString("Settings.renewal.token.custom_id", "");
        String tokenCustomId = tokenCustom == null ? "" : tokenCustom.trim();
        if (renewal && tokenMat == null && tokenCustomId.isEmpty()) {
            plugin.getLogger().warning("Renewal token has neither material nor custom_id"
                    + " - renewal disabled.");
            renewal = false;
        }
        String tokenName = raw.getString("Settings.renewal.token.name", "");
        renewalEnabled = renewal;
        renewalAddSeconds = addSeconds;
        renewalTokenMaterial = tokenMat;
        renewalTokenCustomId = tokenCustomId;
        renewalTokenName = tokenName == null ? ""
                : org.bukkit.ChatColor.stripColor(ConfigLoader.color(tokenName)).toLowerCase(Locale.ROOT);

        // Discord webhook embed mode
        discordWebhookUseEmbed = raw.getBoolean("Settings.discord_webhook.use_embed", false);
        String embedTitle = raw.getString("Settings.discord_webhook.embed_title", "TimedItem");
        discordWebhookEmbedTitle = embedTitle == null ? "TimedItem" : embedTitle;
        discordWebhookEmbedColor = parseHexColor(
                raw.getString("Settings.discord_webhook.embed_color", "ff5555"));

        // Visual decay stages (3.0+)
        visualStages = Collections.unmodifiableList(
                VisualStages.parse(raw.getMapList("Settings.visual_stages")));

        // Hologram countdown above dropped timed items
        hologramEnabled = raw.getBoolean("Settings.hologram.enabled", false);
        String holoFormat = raw.getString("Settings.hologram.format", "&e⌛ {time}");
        hologramFormat = holoFormat == null ? "&e⌛ {time}" : holoFormat;
        hologramHeight = raw.getDouble("Settings.hologram.height", 0.5);

        // Live lore countdown while holding the item
        liveLoreEnabled = raw.getBoolean("Settings.live_lore.enabled", false);
        String loreFormat = raw.getString("Settings.live_lore.format", "&e⌛ {time}");
        liveLoreFormat = loreFormat == null ? "&e⌛ {time}" : loreFormat;

        // Container scanning + anti-hide
        containerScanEnabled = raw.getBoolean("Settings.container_scan.enabled", false);
        String scanMode = raw.getString("Settings.container_scan.mode", "open");
        containerScanMode = scanMode == null ? "open"
                : scanMode.trim().toLowerCase(Locale.ROOT);
        containerScanSeconds = Math.max(5, raw.getInt("Settings.container_scan.interval_seconds", 60));
        antiHideEnabled = raw.getBoolean("Settings.anti_hide.enabled", false);
        antiHideBelowSeconds = Math.max(1, raw.getInt("Settings.anti_hide.block_below_seconds", 60));

        // Auto-return to owner (3.1+)
        returnToOwnerEnabled = raw.getBoolean("Settings.return_to_owner.enabled", false);
        removeTimerOnReturn = raw.getBoolean("Settings.return_to_owner.remove_timer_on_return", true);
        restoreNetworkEnabled = raw.getBoolean("Settings.restore.network", false);
    }

    /** Hex (or decimal) string to an RGB int, defaulting to the fallback. */
    private static int parseHexColor(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0xff5555;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            return Integer.parseInt(value, 16) & 0xffffff;
        } catch (NumberFormatException e) {
            return 0xff5555;
        }
    }
}
