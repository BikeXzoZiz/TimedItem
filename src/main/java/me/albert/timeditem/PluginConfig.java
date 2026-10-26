package me.albert.timeditem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Typed cache of config.yml. Every key has a default so a missing or
 * partial config can never crash the plugin (the 1.x version NPE'd).
 */
public final class PluginConfig {

    private final TimedItemPlugin plugin;

    private String prefix;
    private String expire;
    private String expireSoon;
    private String noItem;
    private String playerOnly;
    private String noTimer;
    private String itemCleared;
    private String timeAdded;
    private String timerPaused;
    private String timerResumed;
    private String timerNotStarted;
    private String alreadyPaused;
    private String alreadyRunning;
    private String invalidUsage;
    private String configReload;
    private String timeSet;
    private String infoRemaining;
    private String infoLifetime;
    private String giveSent;
    private String giveReceived;
    private String targetNotFound;
    private String materialBlacklisted;
    private String restoreGiven;
    private String restoreEmpty;
    private String updateAvailable;
    private String debugStatus;
    private String debugOn;
    private String debugOff;
    private String debugExported;
    private String debugExportFailed;
    private String debugUsage;
    private String itemNotFound;
    private String findUsage;
    private String findNoMatch;
    private String findResult;
    private String findTruncated;
    private String findHitPlayer;
    private String findHitContainer;
    private String findHitGround;
    private String statsHeader;
    private String statsLine;
    private String statsTotal;
    private String statsEmpty;
    private String auditHeader;
    private String auditLine;
    private String auditEmpty;
    private String transformMessage;
    private String graceMessage;
    private String renewalSuccess;
    private String renewalTokenMissing;
    private String restoreReminder;
    private String transformUnknown;

    private boolean expiryTransformEnabled;
    private String expiryTransformDefault;
    private Map<String, Transforms.Profile> transformProfiles;
    private boolean graceEnabled;
    private int graceSeconds;
    private boolean renewalEnabled;
    private long renewalAddSeconds;
    private Material renewalTokenMaterial;
    private String renewalTokenCustomId;
    private String renewalTokenName;
    private boolean discordWebhookUseEmbed;
    private String discordWebhookEmbedTitle;
    private int discordWebhookEmbedColor;

    private List<VisualStages.Stage> visualStages;
    private boolean hologramEnabled;
    private String hologramFormat;
    private double hologramHeight;
    private boolean liveLoreEnabled;
    private String liveLoreFormat;
    private boolean containerScanEnabled;
    private String containerScanMode;
    private int containerScanSeconds;
    private boolean antiHideEnabled;
    private long antiHideBelowSeconds;
    private boolean returnToOwnerEnabled;
    private boolean removeTimerOnReturn;
    private boolean restoreNetworkEnabled;

    private String guiAdminTitle;
    private String guiAdminOwner;
    private String guiAdminType;
    private String guiAdminExpireNow;
    private String guiAdminRemoveTimer;
    private String guiAdminEmpty;
    private String guiAdminPrev;
    private String guiAdminNext;
    private String guiAdminExpired;
    private String guiAdminRemoved;
    private String antiHideBlocked;
    private String infoOwner;
    private String returnedToOwner;
    private String sellBlocked;
    private String restoreLoading;
    private String restoreNetworkTitle;
    private String checkUsage;
    private String checkOffline;
    private String checkHeader;
    private String checkLine;
    private String checkEmpty;

    private String expiresAtPrefix;
    private String usefulLifePrefix;
    private String pausedPrefix;
    private String playtimePrefix;
    private String heldPrefix;
    private String unitS;
    private String unitM;
    private String unitH;
    private String unitD;
    private Map<String, Long> unitSeconds;

    private boolean startDateCalculateOnClick;
    private boolean resetPlayerHealthIfArmor;
    private boolean metricsEnabled;
    private boolean debugMode;
    private boolean expireCheckEnabled;
    private int expireCheckSeconds;
    private boolean scanWorldItems;
    private boolean ignoreCreative;
    private boolean warningEnabled;
    private int warningSeconds;
    private List<Integer> warningStages;
    private String warningSound;
    private float warningVolume;
    private float warningPitch;
    private boolean particleEnabled;
    private String particleName;
    private int particleCount;
    private List<String> expiryCommands;
    private boolean actionbarEnabled;
    private String actionbarFormat;
    private boolean soundEnabled;
    private String soundName;
    private float soundVolume;
    private float soundPitch;
    private boolean updateCheckEnabled;
    private String updateCheckRepository;
    private boolean recoveryEnabled;
    private int recoveryMaxItems;
    private int recoveryKeepDays;
    private String storageType;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUser;
    private String mysqlPassword;
    private boolean mysqlUseSsl;
    private int mysqlPoolSize;
    private String sqliteFile;
    private String mongodbUri;
    private String mongodbDatabase;
    private int auditHistory;
    private boolean discordWebhookEnabled;
    private String discordWebhookUrl;
    private String discordWebhookUsername;
    private String discordWebhookMessage;
    private String guiMenuTitle;
    private String guiPresetLore;
    private String guiToggleName;
    private String guiToggleLore;
    private String guiRemoveName;
    private String guiRemoveLore;
    private String guiCloseName;
    private String guiStatusName;
    private String guiStatusNoTimer;
    private String guiStatusTypeExpire;
    private String guiStatusTypePaused;
    private String guiStatusTypeLifetime;
    private String guiStatusTypePlaytime;
    private String guiStatusTypeHeld;
    private String guiStatusRemaining;
    private String guiStatusExpiresAt;
    private String guiStatusDuration;
    private String guiRestoreTitle;
    private String guiRestoreDate;
    private String guiRestorePlayer;
    private String guiRestoreWorld;
    private String guiRestoreClick;
    private String guiRestorePrev;
    private String guiRestoreNext;
    private Set<Material> blacklistedMaterials;
    private Set<String> disabledHooks;
    private String shopkeepersAutoTimer;
    private String crazycratesAutoTimer;

    /** Hook ids that can be switched off under Hooks.* in config.yml. */
    private static final String[] HOOK_IDS = {
            "mmoitems", "itemsadder", "oraxen", "nexo", "executableitems",
            "slimefun", "ecoitems", "shopkeepers", "crazycrates",
            "quickshop", "zauctionhouse", "crazyauctions", "zshop", "husksync"
    };

    public PluginConfig(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        FileConfiguration raw = plugin.getConfig();
        raw.addDefault("config-version", 13);
        raw.addDefault("Settings.language", "en");
        raw.addDefault("Settings.start_date_calculate_on_click", true);
        raw.addDefault("Settings.reset_player_health_if_armor", true);
        raw.addDefault("Settings.metrics", true);
        raw.addDefault("Settings.debug", false);
        raw.addDefault("Settings.expire_check.enabled", true);
        raw.addDefault("Settings.expire_check.interval_seconds", 10);
        raw.addDefault("Settings.expire_check.scan_world_items", false);
        raw.addDefault("Settings.expire_check.ignore_creative", true);
        raw.addDefault("Settings.discord_webhook.enabled", false);
        raw.addDefault("Settings.discord_webhook.url", "");
        raw.addDefault("Settings.discord_webhook.username", "TimedItem");
        raw.addDefault("Settings.discord_webhook.message",
                "{player}'s timed item expired: {item} ({world})");
        raw.addDefault("Settings.warning.enabled", true);
        raw.addDefault("Settings.warning.seconds", 60);
        raw.addDefault("Settings.warning.stages", Collections.singletonList(60));
        raw.addDefault("Settings.warning.sound.name", "block.note_block.pling");
        raw.addDefault("Settings.warning.sound.volume", 1.0);
        raw.addDefault("Settings.warning.sound.pitch", 1.5);
        raw.addDefault("Settings.particles.enabled", true);
        raw.addDefault("Settings.particles.name", "FLAME");
        raw.addDefault("Settings.particles.count", 30);
        raw.addDefault("Settings.expiry_commands", new java.util.ArrayList<String>());
        raw.addDefault("Settings.actionbar.enabled", true);
        raw.addDefault("Settings.actionbar.format", "&e⌛ {time}");
        raw.addDefault("Settings.sound.enabled", true);
        raw.addDefault("Settings.sound.name", "entity.blaze.death");
        raw.addDefault("Settings.sound.volume", 10.0);
        raw.addDefault("Settings.sound.pitch", 1.2);
        raw.addDefault("Settings.update_check.enabled", true);
        raw.addDefault("Settings.update_check.repository", "BikeXzoZiz/TimedItem");
        raw.addDefault("Settings.recovery.enabled", true);
        raw.addDefault("Settings.recovery.max_items", 500);
        raw.addDefault("Settings.recovery.keep_days", 7);
        raw.addDefault("Settings.storage.type", "flat");
        raw.addDefault("Settings.storage.mysql.host", "localhost");
        raw.addDefault("Settings.storage.mysql.port", 3306);
        raw.addDefault("Settings.storage.mysql.database", "timeditem");
        raw.addDefault("Settings.storage.mysql.user", "timeditem");
        raw.addDefault("Settings.storage.mysql.password", "");
        raw.addDefault("Settings.storage.mysql.use_ssl", false);
        raw.addDefault("Settings.storage.mysql.pool_size", 4);
        raw.addDefault("Settings.storage.sqlite.file", "database.db");
        raw.addDefault("Settings.storage.mongodb.uri", "mongodb://localhost:27017");
        raw.addDefault("Settings.storage.mongodb.database", "timeditem");
        raw.addDefault("Settings.storage.audit_history", 5000);
        raw.addDefault("Settings.expiry_transform.enabled", false);
        raw.addDefault("Settings.expiry_transform.default", "");
        raw.addDefault("Settings.transforms", new java.util.ArrayList<String>());
        raw.addDefault("Settings.grace.enabled", false);
        raw.addDefault("Settings.grace.seconds", 30);
        raw.addDefault("Settings.renewal.enabled", false);
        raw.addDefault("Settings.renewal.add_time", "7d");
        raw.addDefault("Settings.renewal.token.material", "CLOCK");
        raw.addDefault("Settings.renewal.token.custom_id", "");
        raw.addDefault("Settings.renewal.token.name", "");
        raw.addDefault("Settings.discord_webhook.use_embed", false);
        raw.addDefault("Settings.discord_webhook.embed_title", "TimedItem");
        raw.addDefault("Settings.discord_webhook.embed_color", "ff5555");
        raw.addDefault("Settings.visual_stages", new java.util.ArrayList<String>());
        raw.addDefault("Settings.hologram.enabled", false);
        raw.addDefault("Settings.hologram.format", "&e⌛ {time}");
        raw.addDefault("Settings.hologram.height", 0.5);
        raw.addDefault("Settings.live_lore.enabled", false);
        raw.addDefault("Settings.live_lore.format", "&e⌛ {time}");
        raw.addDefault("Settings.container_scan.enabled", false);
        raw.addDefault("Settings.container_scan.mode", "open");
        raw.addDefault("Settings.container_scan.interval_seconds", 60);
        raw.addDefault("Settings.anti_hide.enabled", false);
        raw.addDefault("Settings.anti_hide.block_below_seconds", 60);
        raw.addDefault("Settings.return_to_owner.enabled", false);
        raw.addDefault("Settings.return_to_owner.remove_timer_on_return", true);
        raw.addDefault("Settings.restore.network", false);
        raw.addDefault("Settings.blacklist.materials", new java.util.ArrayList<String>());
        raw.addDefault("Hooks.mmoitems", true);
        raw.addDefault("Hooks.itemsadder", true);
        raw.addDefault("Hooks.oraxen", true);
        raw.addDefault("Hooks.nexo", true);
        raw.addDefault("Hooks.executableitems", true);
        raw.addDefault("Hooks.slimefun", true);
        raw.addDefault("Hooks.ecoitems", true);
        raw.addDefault("Hooks.crazycrates.enabled", true);
        raw.addDefault("Hooks.crazycrates.auto_timer", "");
        raw.addDefault("Hooks.shopkeepers.enabled", true);
        raw.addDefault("Hooks.shopkeepers.auto_timer", "");
        raw.options().copyDefaults(true);
        plugin.saveConfig();

        FileConfiguration lang = loadLanguage(raw);

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
        List<Integer> stages = new java.util.ArrayList<Integer>();
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
        actionbarFormat = color(raw.getString("Settings.actionbar.format"));
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

        // GUI labels also come from the language file.
        guiMenuTitle = color(lang.getString("Gui.menu_title"));
        guiPresetLore = color(lang.getString("Gui.preset_lore"));
        guiToggleName = color(lang.getString("Gui.toggle_name"));
        guiToggleLore = color(lang.getString("Gui.toggle_lore"));
        guiRemoveName = color(lang.getString("Gui.remove_name"));
        guiRemoveLore = color(lang.getString("Gui.remove_lore"));
        guiCloseName = color(lang.getString("Gui.close_name"));
        guiStatusName = color(lang.getString("Gui.status_name"));
        guiStatusNoTimer = color(lang.getString("Gui.status_no_timer"));
        guiStatusTypeExpire = color(lang.getString("Gui.status_type_expire"));
        guiStatusTypePaused = color(lang.getString("Gui.status_type_paused"));
        guiStatusTypeLifetime = color(lang.getString("Gui.status_type_lifetime"));
        guiStatusTypePlaytime = color(lang.getString("Gui.status_type_playtime"));
        guiStatusTypeHeld = color(lang.getString("Gui.status_type_held"));
        guiStatusRemaining = color(lang.getString("Gui.status_remaining"));
        guiStatusExpiresAt = color(lang.getString("Gui.status_expires_at"));
        guiStatusDuration = color(lang.getString("Gui.status_duration"));
        guiRestoreTitle = color(lang.getString("Gui.restore_title"));
        guiRestoreDate = color(lang.getString("Gui.restore_date"));
        guiRestorePlayer = color(lang.getString("Gui.restore_player"));
        guiRestoreWorld = color(lang.getString("Gui.restore_world"));
        guiRestoreClick = color(lang.getString("Gui.restore_click"));
        guiRestorePrev = color(lang.getString("Gui.restore_prev"));
        guiRestoreNext = color(lang.getString("Gui.restore_next"));

        // Messages and lore format come from the language file, not config.yml.
        prefix = color(lang.getString("Messages.prefix"));
        expire = color(lang.getString("Messages.expire"));
        expireSoon = color(lang.getString("Messages.expire_soon"));
        noItem = color(lang.getString("Messages.no_item"));
        playerOnly = color(lang.getString("Messages.player_only"));
        noTimer = color(lang.getString("Messages.no_timer"));
        itemCleared = color(lang.getString("Messages.item_cleared"));
        timeAdded = color(lang.getString("Messages.time_added"));
        timerPaused = color(lang.getString("Messages.timer_paused"));
        timerResumed = color(lang.getString("Messages.timer_resumed"));
        timerNotStarted = color(lang.getString("Messages.timer_not_started"));
        alreadyPaused = color(lang.getString("Messages.already_paused"));
        alreadyRunning = color(lang.getString("Messages.already_running"));
        invalidUsage = color(lang.getString("Messages.invalid_usage"));
        configReload = color(lang.getString("Messages.config_reload"));
        timeSet = color(lang.getString("Messages.time_set"));
        infoRemaining = color(lang.getString("Messages.info_remaining"));
        infoLifetime = color(lang.getString("Messages.info_lifetime"));
        giveSent = color(lang.getString("Messages.give_sent"));
        giveReceived = color(lang.getString("Messages.give_received"));
        targetNotFound = color(lang.getString("Messages.target_not_found"));
        materialBlacklisted = color(lang.getString("Messages.material_blacklisted"));
        restoreGiven = color(lang.getString("Messages.restore_given"));
        restoreEmpty = color(lang.getString("Messages.restore_empty"));
        updateAvailable = color(lang.getString("Messages.update_available"));

        debugStatus = color(lang.getString("Messages.debug_status"));
        debugOn = color(lang.getString("Messages.debug_on"));
        debugOff = color(lang.getString("Messages.debug_off"));
        debugExported = color(lang.getString("Messages.debug_exported"));
        debugExportFailed = color(lang.getString("Messages.debug_export_failed"));
        debugUsage = color(lang.getString("Messages.debug_usage"));
        itemNotFound = color(lang.getString("Messages.item_not_found"));

        findUsage = color(lang.getString("Messages.find_usage"));
        findNoMatch = color(lang.getString("Messages.find_no_match"));
        findResult = color(lang.getString("Messages.find_result"));
        findTruncated = color(lang.getString("Messages.find_truncated"));
        findHitPlayer = color(lang.getString("Messages.find_hit_player"));
        findHitContainer = color(lang.getString("Messages.find_hit_container"));
        findHitGround = color(lang.getString("Messages.find_hit_ground"));
        statsHeader = color(lang.getString("Messages.stats_header"));
        statsLine = color(lang.getString("Messages.stats_line"));
        statsTotal = color(lang.getString("Messages.stats_total"));
        statsEmpty = color(lang.getString("Messages.stats_empty"));
        auditHeader = color(lang.getString("Messages.audit_header"));
        auditLine = color(lang.getString("Messages.audit_line"));
        auditEmpty = color(lang.getString("Messages.audit_empty"));
        transformMessage = color(lang.getString("Messages.transform_received"));
        graceMessage = color(lang.getString("Messages.grace_blocked"));
        renewalSuccess = color(lang.getString("Messages.renewal_success"));
        renewalTokenMissing = color(lang.getString("Messages.renewal_token_missing"));
        restoreReminder = color(lang.getString("Messages.restore_reminder"));
        transformUnknown = color(lang.getString("Messages.transform_unknown"));
        antiHideBlocked = color(lang.getString("Messages.anti_hide_blocked"));
        checkUsage = color(lang.getString("Messages.check_usage"));
        checkOffline = color(lang.getString("Messages.check_offline"));
        checkHeader = color(lang.getString("Messages.check_header"));
        checkLine = color(lang.getString("Messages.check_line"));
        checkEmpty = color(lang.getString("Messages.check_empty"));
        guiAdminTitle = color(lang.getString("Gui.admin_title"));
        guiAdminOwner = color(lang.getString("Gui.admin_owner"));
        guiAdminType = color(lang.getString("Gui.admin_type"));
        guiAdminExpireNow = color(lang.getString("Gui.admin_expire_now"));
        guiAdminRemoveTimer = color(lang.getString("Gui.admin_remove_timer"));
        guiAdminEmpty = color(lang.getString("Gui.admin_empty"));
        guiAdminPrev = color(lang.getString("Gui.admin_prev"));
        guiAdminNext = color(lang.getString("Gui.admin_next"));
        guiAdminExpired = color(lang.getString("Messages.admin_expired"));
        guiAdminRemoved = color(lang.getString("Messages.admin_removed"));
        infoOwner = color(lang.getString("Messages.info_owner"));
        returnedToOwner = color(lang.getString("Messages.returned_to_owner"));
        sellBlocked = color(lang.getString("Messages.sell_blocked"));
        restoreLoading = color(lang.getString("Messages.restore_loading"));
        restoreNetworkTitle = color(lang.getString("Messages.restore_network_title"));

        expiresAtPrefix = color(lang.getString("DateFormat.expires_at"));
        usefulLifePrefix = color(lang.getString("DateFormat.useful_life"));
        pausedPrefix = color(lang.getString("DateFormat.paused"));
        playtimePrefix = color(lang.getString("DateFormat.playtime"));
        heldPrefix = color(lang.getString("DateFormat.held"));
        unitS = lang.getString("DateFormat.s", "s");
        unitM = lang.getString("DateFormat.m", "m");
        unitH = lang.getString("DateFormat.h", "h");
        unitD = lang.getString("DateFormat.d", "d");

        Map<String, Long> units = new HashMap<String, Long>();
        units.put(unitS.toLowerCase(Locale.ROOT), 1L);
        units.put(unitM.toLowerCase(Locale.ROOT), 60L);
        units.put(unitH.toLowerCase(Locale.ROOT), 3600L);
        units.put(unitD.toLowerCase(Locale.ROOT), 86400L);
        unitSeconds = Collections.unmodifiableMap(units);

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
        renewalEnabled = raw.getBoolean("Settings.renewal.enabled", false);
        String addTime = raw.getString("Settings.renewal.add_time", "7d");
        renewalAddSeconds = addTime == null ? 0L : DurationParser.parse(addTime.trim(), units);
        if (renewalEnabled && renewalAddSeconds <= 0) {
            plugin.getLogger().warning("Settings.renewal.add_time '" + addTime
                    + "' is not a valid duration - renewal disabled.");
            renewalEnabled = false;
        }
        String tokenMaterial = raw.getString("Settings.renewal.token.material", "CLOCK");
        renewalTokenMaterial = tokenMaterial == null || tokenMaterial.trim().isEmpty()
                ? null : Material.matchMaterial(tokenMaterial.trim());
        if (renewalEnabled && renewalTokenMaterial == null) {
            plugin.getLogger().warning("Settings.renewal.token.material '" + tokenMaterial
                    + "' is unknown - renewal disabled.");
            renewalEnabled = false;
        }
        String tokenCustom = raw.getString("Settings.renewal.token.custom_id", "");
        renewalTokenCustomId = tokenCustom == null ? "" : tokenCustom.trim();
        if (renewalEnabled && renewalTokenMaterial == null && renewalTokenCustomId.isEmpty()) {
            plugin.getLogger().warning("Renewal token has neither material nor custom_id"
                    + " - renewal disabled.");
            renewalEnabled = false;
        }
        String tokenName = raw.getString("Settings.renewal.token.name", "");
        renewalTokenName = tokenName == null ? ""
                : ChatColor.stripColor(color(tokenName)).toLowerCase(Locale.ROOT);

        // Discord webhook embed mode
        discordWebhookUseEmbed = raw.getBoolean("Settings.discord_webhook.use_embed", false);
        String embedTitle = raw.getString("Settings.discord_webhook.embed_title", "TimedItem");
        discordWebhookEmbedTitle = embedTitle == null ? "TimedItem" : embedTitle;
        discordWebhookEmbedColor = parseHexColor(
                raw.getString("Settings.discord_webhook.embed_color", "ff5555"));

        // Visual decay stages (3.0+)
        visualStages = java.util.Collections.unmodifiableList(
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

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getExpire() {
        return expire;
    }

    public String getExpireSoon() {
        return expireSoon;
    }

    public String getNoItem() {
        return noItem;
    }

    public String getPlayerOnly() {
        return playerOnly;
    }

    public String getNoTimer() {
        return noTimer;
    }

    public String getItemCleared() {
        return itemCleared;
    }

    public String getTimeAdded() {
        return timeAdded;
    }

    public String getTimerPaused() {
        return timerPaused;
    }

    public String getTimerResumed() {
        return timerResumed;
    }

    public String getTimerNotStarted() {
        return timerNotStarted;
    }

    public String getAlreadyPaused() {
        return alreadyPaused;
    }

    public String getAlreadyRunning() {
        return alreadyRunning;
    }

    public String getInvalidUsage() {
        return invalidUsage;
    }

    public String getConfigReload() {
        return configReload;
    }

    public String getTimeSet() {
        return timeSet;
    }

    public String getInfoRemaining() {
        return infoRemaining;
    }

    public String getInfoLifetime() {
        return infoLifetime;
    }

    public String getGiveSent() {
        return giveSent;
    }

    public String getGiveReceived() {
        return giveReceived;
    }

    public String getTargetNotFound() {
        return targetNotFound;
    }

    public String getMaterialBlacklisted() {
        return materialBlacklisted;
    }

    public String getRestoreGiven() {
        return restoreGiven;
    }

    public String getRestoreEmpty() {
        return restoreEmpty;
    }

    public String getUpdateAvailable() {
        return updateAvailable;
    }

    public String getDebugStatus() {
        return debugStatus;
    }

    public String getDebugOn() {
        return debugOn;
    }

    public String getDebugOff() {
        return debugOff;
    }

    public String getDebugExported() {
        return debugExported;
    }

    public String getDebugExportFailed() {
        return debugExportFailed;
    }

    public String getDebugUsage() {
        return debugUsage;
    }

    public String getItemNotFound() {
        return itemNotFound;
    }

    public String getFindUsage() {
        return findUsage;
    }

    public String getFindNoMatch() {
        return findNoMatch;
    }

    public String getFindResult() {
        return findResult;
    }

    public String getFindTruncated() {
        return findTruncated;
    }

    public String getFindHitPlayer() {
        return findHitPlayer;
    }

    public String getFindHitContainer() {
        return findHitContainer;
    }

    public String getFindHitGround() {
        return findHitGround;
    }

    public String getStatsHeader() {
        return statsHeader;
    }

    public String getStatsLine() {
        return statsLine;
    }

    public String getStatsTotal() {
        return statsTotal;
    }

    public String getStatsEmpty() {
        return statsEmpty;
    }

    public String getAuditHeader() {
        return auditHeader;
    }

    public String getAuditLine() {
        return auditLine;
    }

    public String getAuditEmpty() {
        return auditEmpty;
    }

    public String getTransformMessage() {
        return transformMessage;
    }

    public String getGraceMessage() {
        return graceMessage;
    }

    public String getRenewalSuccess() {
        return renewalSuccess;
    }

    public String getRenewalTokenMissing() {
        return renewalTokenMissing;
    }

    public String getRestoreReminder() {
        return restoreReminder;
    }

    public String getTransformUnknown() {
        return transformUnknown;
    }
    public String getAntiHideBlocked() {
        return antiHideBlocked;
    }

    public String getCheckUsage() {
        return checkUsage;
    }

    public String getCheckOffline() {
        return checkOffline;
    }

    public String getCheckHeader() {
        return checkHeader;
    }

    public String getCheckLine() {
        return checkLine;
    }

    public String getCheckEmpty() {
        return checkEmpty;
    }

    public String getGuiAdminTitle() {
        return guiAdminTitle;
    }

    public String getGuiAdminOwner() {
        return guiAdminOwner;
    }

    public String getGuiAdminType() {
        return guiAdminType;
    }

    public String getGuiAdminExpireNow() {
        return guiAdminExpireNow;
    }

    public String getGuiAdminRemoveTimer() {
        return guiAdminRemoveTimer;
    }

    public String getGuiAdminEmpty() {
        return guiAdminEmpty;
    }

    public String getGuiAdminPrev() {
        return guiAdminPrev;
    }

    public String getGuiAdminNext() {
        return guiAdminNext;
    }

    public String getGuiAdminExpired() {
        return guiAdminExpired;
    }

    public String getGuiAdminRemoved() {
        return guiAdminRemoved;
    }

    /** Parsed visual decay stages (Settings.visual_stages), most severe last index. */
    public List<VisualStages.Stage> visualStages() {
        return visualStages;
    }

    public boolean isHologramEnabled() {
        return hologramEnabled;
    }

    public String getHologramFormat() {
        return hologramFormat;
    }

    public double getHologramHeight() {
        return hologramHeight;
    }

    public boolean isLiveLoreEnabled() {
        return liveLoreEnabled;
    }

    public String getLiveLoreFormat() {
        return liveLoreFormat;
    }

    public boolean isContainerScanEnabled() {
        return containerScanEnabled;
    }

    /** open, periodic or both. */
    public String getContainerScanMode() {
        return containerScanMode;
    }

    public int getContainerScanSeconds() {
        return containerScanSeconds;
    }

    public boolean isAntiHideEnabled() {
        return antiHideEnabled;
    }

    public long getAntiHideBelowSeconds() {
        return antiHideBelowSeconds;
    }
    public boolean isReturnToOwnerEnabled() {
        return returnToOwnerEnabled;
    }

    public boolean isRemoveTimerOnReturn() {
        return removeTimerOnReturn;
    }

    /** Network restore: /ti restore reads the shared database instead of local memory. */
    public boolean isRestoreNetworkEnabled() {
        return restoreNetworkEnabled;
    }

    public String getInfoOwner() {
        return infoOwner;
    }

    public String getReturnedToOwner() {
        return returnedToOwner;
    }

    public String getSellBlocked() {
        return sellBlocked;
    }

    public String getRestoreLoading() {
        return restoreLoading;
    }

    public String getRestoreNetworkTitle() {
        return restoreNetworkTitle;
    }

    /** True when expiry transformation is enabled (Settings.expiry_transform). */
    public boolean isExpiryTransformEnabled() {
        return expiryTransformEnabled;
    }

    /** Profile key used when an item carries no transform tag ("" = none). */
    public String getExpiryTransformDefault() {
        return expiryTransformDefault;
    }

    /** All parsed profiles from Settings.transforms, keyed lowercase. */
    public Map<String, Transforms.Profile> transformProfiles() {
        return transformProfiles;
    }

    public boolean isGraceEnabled() {
        return graceEnabled;
    }

    public int getGraceSeconds() {
        return graceSeconds;
    }

    public boolean isRenewalEnabled() {
        return renewalEnabled;
    }

    /** Seconds added per renewal token (0 = renewal off). */
    public long getRenewalAddSeconds() {
        return renewalAddSeconds;
    }

    public Material getRenewalTokenMaterial() {
        return renewalTokenMaterial;
    }

    public String getRenewalTokenCustomId() {
        return renewalTokenCustomId;
    }

    /** Required token display name (stripped, lowercase; "" = any name). */
    public String getRenewalTokenName() {
        return renewalTokenName;
    }

    public boolean isDiscordWebhookUseEmbed() {
        return discordWebhookUseEmbed;
    }

    public String getDiscordWebhookEmbedTitle() {
        return discordWebhookEmbedTitle;
    }

    public int getDiscordWebhookEmbedColor() {
        return discordWebhookEmbedColor;
    }

    /** True unless the hook was switched off under Hooks.&lt;id&gt; in config.yml. */
    public boolean isHookEnabled(String id) {
        return !disabledHooks.contains(id.toLowerCase(Locale.ROOT));
    }

    /** Hooks.shopkeepers.auto_timer - empty means the hook is off. */
    public String getShopkeepersAutoTimer() {
        return shopkeepersAutoTimer;
    }

    /** Hooks.crazycrates.auto_timer - empty means the hook is off. */
    public String getCrazyCratesAutoTimer() {
        return crazycratesAutoTimer;
    }

    public String getExpiresAtPrefix() {
        return expiresAtPrefix;
    }

    public String getUsefulLifePrefix() {
        return usefulLifePrefix;
    }

    public String getPausedPrefix() {
        return pausedPrefix;
    }

    public String getPlaytimePrefix() {
        return playtimePrefix;
    }

    public String getHeldPrefix() {
        return heldPrefix;
    }

    public String getUnitS() {
        return unitS;
    }

    public String getUnitM() {
        return unitM;
    }

    public String getUnitH() {
        return unitH;
    }

    public String getUnitD() {
        return unitD;
    }

    /** Configured unit labels (lowercased) mapped to their length in seconds. */
    public Map<String, Long> unitSeconds() {
        return unitSeconds;
    }

    public boolean isStartDateCalculateOnClick() {
        return startDateCalculateOnClick;
    }

    public boolean isResetPlayerHealthIfArmor() {
        return resetPlayerHealthIfArmor;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    /** Verbose [Debug] logging; a session switch on top of Settings.debug. */
    public boolean isDebugEnabled() {
        return debugMode;
    }

    public boolean isExpireCheckEnabled() {
        return expireCheckEnabled;
    }

    public int getExpireCheckSeconds() {
        return expireCheckSeconds;
    }

    public boolean isScanWorldItems() {
        return scanWorldItems;
    }

    /** True when creative players are skipped entirely (no expiry, no countdown). */
    public boolean isIgnoreCreative() {
        return ignoreCreative;
    }

    public boolean isWarningEnabled() {
        return warningEnabled;
    }

    public int getWarningSeconds() {
        return warningSeconds;
    }

    /** Warning thresholds in seconds, highest first. */
    public List<Integer> getWarningStages() {
        return warningStages;
    }

    public String getWarningSound() {
        return warningSound;
    }

    public float getWarningVolume() {
        return warningVolume;
    }

    public float getWarningPitch() {
        return warningPitch;
    }

    public boolean isParticleEnabled() {
        return particleEnabled;
    }

    public String getParticleName() {
        return particleName;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public List<String> getExpiryCommands() {
        return expiryCommands;
    }

    public boolean isActionbarEnabled() {
        return actionbarEnabled;
    }

    public String getActionbarFormat() {
        return actionbarFormat;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public String getSoundName() {
        return soundName;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }

    public String getUpdateCheckRepository() {
        return updateCheckRepository;
    }

    public boolean isRecoveryEnabled() {
        return recoveryEnabled;
    }

    public int getRecoveryMaxItems() {
        return recoveryMaxItems;
    }

    public int getRecoveryKeepDays() {
        return recoveryKeepDays;
    }

    /** Settings.storage.type: flat, sqlite, mysql or mongodb. */
    public String getStorageType() {
        return storageType;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUser() {
        return mysqlUser;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public boolean isMysqlUseSsl() {
        return mysqlUseSsl;
    }

    public int getMysqlPoolSize() {
        return mysqlPoolSize;
    }

    public String getSqliteFile() {
        return sqliteFile;
    }

    public String getMongodbUri() {
        return mongodbUri;
    }

    public String getMongodbDatabase() {
        return mongodbDatabase;
    }

    /** Maximum audit entries kept by the storage backend. */
    public int getAuditHistory() {
        return auditHistory;
    }

    public boolean isDiscordWebhookEnabled() {
        return discordWebhookEnabled;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }

    public String getDiscordWebhookUsername() {
        return discordWebhookUsername;
    }

    public String getDiscordWebhookMessage() {
        return discordWebhookMessage;
    }

    public String getGuiMenuTitle() {
        return guiMenuTitle;
    }

    public String getGuiPresetLore() {
        return guiPresetLore;
    }

    public String getGuiToggleName() {
        return guiToggleName;
    }

    public String getGuiToggleLore() {
        return guiToggleLore;
    }

    public String getGuiRemoveName() {
        return guiRemoveName;
    }

    public String getGuiRemoveLore() {
        return guiRemoveLore;
    }

    public String getGuiCloseName() {
        return guiCloseName;
    }

    public String getGuiStatusName() {
        return guiStatusName;
    }

    public String getGuiStatusNoTimer() {
        return guiStatusNoTimer;
    }

    public String getGuiStatusTypeExpire() {
        return guiStatusTypeExpire;
    }

    public String getGuiStatusTypePaused() {
        return guiStatusTypePaused;
    }

    public String getGuiStatusTypeLifetime() {
        return guiStatusTypeLifetime;
    }

    public String getGuiStatusTypePlaytime() {
        return guiStatusTypePlaytime;
    }

    public String getGuiStatusTypeHeld() {
        return guiStatusTypeHeld;
    }

    public String getGuiStatusRemaining() {
        return guiStatusRemaining;
    }

    public String getGuiStatusExpiresAt() {
        return guiStatusExpiresAt;
    }

    public String getGuiStatusDuration() {
        return guiStatusDuration;
    }

    public String getGuiRestoreTitle() {
        return guiRestoreTitle;
    }

    public String getGuiRestoreDate() {
        return guiRestoreDate;
    }

    public String getGuiRestorePlayer() {
        return guiRestorePlayer;
    }

    public String getGuiRestoreWorld() {
        return guiRestoreWorld;
    }

    public String getGuiRestoreClick() {
        return guiRestoreClick;
    }

    public String getGuiRestorePrev() {
        return guiRestorePrev;
    }

    public String getGuiRestoreNext() {
        return guiRestoreNext;
    }

    /** Materials that may never receive a timer. */
    public Set<Material> blacklistedMaterials() {
        return blacklistedMaterials;
    }

    /**
     * Loads lang/messages_&lt;language&gt;.yml (Settings.language). Old
     * installs carry their messages inside config.yml - those values are
     * carried over into the language file once and removed from
     * config.yml, so user edits survive the move. Missing keys self-heal
     * from the built-in English defaults and are written back.
     */
    private FileConfiguration loadLanguage(FileConfiguration raw) {
        String language = raw.getString("Settings.language", "en");
        if (language == null || !language.matches("[a-z0-9_-]+")) {
            language = "en";
        }
        String path = "lang/messages_" + language + ".yml";
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            try {
                plugin.saveResource(path, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown language '" + language + "' - using English.");
                path = "lang/messages_en.yml";
                file = new File(plugin.getDataFolder(), path);
                if (!file.exists()) {
                    plugin.saveResource(path, false);
                }
            }
        }
        YamlConfiguration lang = YamlConfiguration.loadConfiguration(file);

        boolean migrated = false;
        if (raw.isSet("Messages")) {
            for (String key : raw.getConfigurationSection("Messages").getKeys(true)) {
                lang.set("Messages." + key, raw.get("Messages." + key));
            }
            raw.set("Messages", null);
            migrated = true;
        }
        if (raw.isSet("DateFormat")) {
            for (String key : raw.getConfigurationSection("DateFormat").getKeys(true)) {
                lang.set("DateFormat." + key, raw.get("DateFormat." + key));
            }
            raw.set("DateFormat", null);
            migrated = true;
        }
        if (migrated) {
            plugin.saveConfig();
            plugin.getLogger().info("Moved messages from config.yml to " + path
                    + " - edit that file from now on.");
        }

        lang.options().copyDefaults(true);
        addMessageDefaults(lang);
        try {
            lang.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + path + ": " + e);
        }
        return lang;
    }

    /** Built-in English fallbacks for every message key (self-healing). */
    private static void addMessageDefaults(FileConfiguration lang) {
        lang.addDefault("Messages.prefix", "&6[TimedItem] &r");
        lang.addDefault("Messages.expire", "&cYour item has expired and was removed!");
        lang.addDefault("Messages.expire_soon", "&eYour item will expire in &c{time}&e!");
        lang.addDefault("Messages.no_item", "&cHold the item you want to manage first.");
        lang.addDefault("Messages.player_only", "&cOnly players can use this command.");
        lang.addDefault("Messages.no_timer", "&7The item in your hand has no timer.");
        lang.addDefault("Messages.item_cleared", "&aTimer removed from the item in your hand.");
        lang.addDefault("Messages.time_added", "&aTime added. Remaining: &e{time}");
        lang.addDefault("Messages.timer_paused", "&aTimer paused with &e{time}&a remaining.");
        lang.addDefault("Messages.timer_resumed", "&aTimer resumed. Expires in &e{time}&a.");
        lang.addDefault("Messages.timer_not_started", "&7The timer on this item has not started yet.");
        lang.addDefault("Messages.already_paused", "&7The timer on this item is already paused.");
        lang.addDefault("Messages.already_running", "&7The timer on this item is already running.");
        lang.addDefault("Messages.invalid_usage",
                "&cUsage: /ti set <time>, /ti add <time>, /ti give <player> [plugin:id] <time> [amount], /ti pause, /ti resume, /ti remove, /ti info, /ti menu, /ti restore, /ti debug, /ti reload");
        lang.addDefault("Messages.config_reload", "&aTimedItem config reloaded.");
        lang.addDefault("Messages.time_set", "&aTimer set: &e{time}");
        lang.addDefault("Messages.info_remaining", "&7Time remaining: &e{time}");
        lang.addDefault("Messages.info_lifetime", "&7Lifetime: &e{time} &7(starts on first use)");
        lang.addDefault("Messages.give_sent", "&aGave a timed item (&e{time}&a) to &e{player}&a.");
        lang.addDefault("Messages.give_received", "&aYou received a timed item expiring in &e{time}&a.");
        lang.addDefault("Messages.target_not_found", "&cPlayer &e{player} &cis not online.");
        lang.addDefault("Messages.material_blacklisted",
                "&cThis item's material cannot carry a TimedItem timer.");
        lang.addDefault("Messages.restore_given", "&aRecovered the item from the expiry log.");
        lang.addDefault("Messages.restore_empty", "&7The expiry recovery log is empty.");
        lang.addDefault("Messages.update_available",
                "&eTimedItem &6{version} &eis available: &7{url}");
        lang.addDefault("Messages.debug_status",
                "&7Debug mode: &e{state}&7. Buffered log entries: &e{entries}&7."
                        + " Recovery entries: &e{recovery}&7. Item storage: &e{storage}&7.");
        lang.addDefault("Messages.debug_on",
                "&aDebug mode enabled - verbose TimedItem logging is now visible in the console."
                        + " Use /ti debug export to write a bug report.");
        lang.addDefault("Messages.debug_off", "&7Debug mode disabled.");
        lang.addDefault("Messages.debug_exported",
                "&aDebug report written to &e{file}");
        lang.addDefault("Messages.debug_export_failed",
                "&cCould not write the debug report: &7{error}");
        lang.addDefault("Messages.debug_usage", "&7Usage: /ti debug [on|off|export]");
        lang.addDefault("Messages.item_not_found",
                "&cUnknown custom item: &e{id}&c. Use <plugin>:<id>, e.g. mmoitems:SWORD:CUTLASS.");
        lang.addDefault("Messages.find_usage",
                "&7Usage: /ti find <material|plugin:id|name>");
        lang.addDefault("Messages.find_no_match",
                "&7No timed items matching &e{query} &7were found.");
        lang.addDefault("Messages.find_result",
                "&aFound &e{count} &atimed item(s) matching &e{query}&a:");
        lang.addDefault("Messages.find_truncated",
                "&7...and &e{rest} &7more (showing the first {shown}).");
        lang.addDefault("Messages.find_hit_player", " &8- &e{player} &7(slot {slot})");
        lang.addDefault("Messages.find_hit_container", " &8- &7{world} &e{x},{y},{z} &7({type})");
        lang.addDefault("Messages.find_hit_ground", " &8- &7{world} &e{x},{y},{z}");
        lang.addDefault("Messages.stats_header", "&6Expired items (last {days} days):");
        lang.addDefault("Messages.stats_line", " &e{material}&7: &a{count}");
        lang.addDefault("Messages.stats_total", "&7Total expired: &e{count}");
        lang.addDefault("Messages.stats_empty",
                "&7No items have expired in the last {days} days.");
        lang.addDefault("Messages.audit_header", "&6Audit log (page {page}):");
        lang.addDefault("Messages.audit_line",
                " &8[{date}] &e{actor} &7{action} &7target=&f{target} &7item=&f{item} &7{details}");
        lang.addDefault("Messages.audit_empty", "&7The audit log is empty.");
        lang.addDefault("Messages.transform_received",
                "&7Your expired item turned into &e{item}&7.");
        lang.addDefault("Messages.grace_blocked",
                "&cThis item expires in &e{time} &cand can no longer be used!");
        lang.addDefault("Messages.renewal_success",
                "&aRenewed with a token. Remaining: &e{time}");
        lang.addDefault("Messages.renewal_token_missing",
                "&7Hold the item to renew in one hand and the renewal token in the other.");
        lang.addDefault("Messages.restore_reminder",
                "&eYou have &6{count} &eexpired item(s) waiting for restore. &7(/ti restore)");
        lang.addDefault("Messages.transform_unknown",
                "&cUnknown transform profile: &e{id}&c. Define it under Settings.transforms.");
        lang.addDefault("Messages.anti_hide_blocked",
                "&cThis item expires in &e{time} &c- it can no longer be stored in containers!");
        lang.addDefault("Messages.check_usage", "&7Usage: /ti check <player>");
        lang.addDefault("Messages.check_offline", "&cPlayer &e{player} &cmust be online for a check.");
        lang.addDefault("Messages.check_header", "&6Timed items of &e{player}&6:");
        lang.addDefault("Messages.check_line", " &8- &7slot &e{slot}&7: &e{item} &7({type}, &e{time}&7)");
        lang.addDefault("Messages.check_empty", "&7{player} carries no timed items.");
        lang.addDefault("Gui.admin_title", "&8TimedItem Admin &7({page}/{pages})");
        lang.addDefault("Gui.admin_owner", "&7Owner: &e{player}");
        lang.addDefault("Gui.admin_type", "&7Timer: &e{type} &7- &e{time}");
        lang.addDefault("Gui.admin_expire_now", "&aLeft-click: &7expire now");
        lang.addDefault("Gui.admin_remove_timer", "&cRight-click: &7remove timer");
        lang.addDefault("Gui.admin_empty", "&7No timed items seen by the last scan");
        lang.addDefault("Gui.admin_prev", "&ePrevious page");
        lang.addDefault("Gui.admin_next", "&eNext page");
        lang.addDefault("Messages.admin_expired", "&aExpired the item of &e{player}&a.");
        lang.addDefault("Messages.admin_removed", "&aRemoved the timer of &e{player}&a's item.");
        lang.addDefault("Messages.info_owner", "&7Owner: &e{player}");
        lang.addDefault("Messages.returned_to_owner",
                "&aYour rented item expired and was returned to you.");
        lang.addDefault("Messages.sell_blocked",
                "&cTimed items cannot be sold or listed! &7(expires in &e{time}&7)");
        lang.addDefault("Messages.restore_loading", "&7Loading expired items...");
        lang.addDefault("Messages.restore_network_title", "&8Expired items (all servers) &7({page}/{pages})");
        lang.addDefault("DateFormat.expires_at", "&cExpires: ");
        lang.addDefault("DateFormat.useful_life", "&7Lifetime: ");
        lang.addDefault("DateFormat.paused", "&7Paused: ");
        lang.addDefault("DateFormat.playtime", "&6Playtime: ");
        lang.addDefault("DateFormat.held", "&6Held time: ");
        lang.addDefault("DateFormat.s", "s");
        lang.addDefault("DateFormat.m", "m");
        lang.addDefault("DateFormat.h", "h");
        lang.addDefault("DateFormat.d", "d");
        lang.addDefault("Gui.menu_title", "&6TimedItem Menu");
        lang.addDefault("Gui.preset_lore", "&7Set this lifetime on the item in your hand");
        lang.addDefault("Gui.toggle_name", "&6Pause / Resume");
        lang.addDefault("Gui.toggle_lore", "&7Freeze the countdown, or resume a paused item");
        lang.addDefault("Gui.remove_name", "&cRemove timer");
        lang.addDefault("Gui.remove_lore", "&7Strip the timer from the item in your hand");
        lang.addDefault("Gui.close_name", "&cClose");
        lang.addDefault("Gui.status_name", "&6Held item status");
        lang.addDefault("Gui.status_no_timer", "&7Held item: &cno timer");
        lang.addDefault("Gui.status_type_expire", "&7Type: &eexpire");
        lang.addDefault("Gui.status_type_paused", "&7Type: &epaused");
        lang.addDefault("Gui.status_type_lifetime", "&7Type: &elifetime (not started)");
        lang.addDefault("Gui.status_type_playtime", "&7Type: &eplaytime (while online)");
        lang.addDefault("Gui.status_type_held", "&7Type: &eheld time (while held/worn)");
        lang.addDefault("Gui.status_remaining", "&7Remaining: &e{time}");
        lang.addDefault("Gui.status_expires_at", "&7Expires at: &e{date}");
        lang.addDefault("Gui.status_duration", "&7Duration: &e{time}");
        lang.addDefault("Gui.restore_title", "&8TimedItem Restore &7({page}/{pages})");
        lang.addDefault("Gui.restore_date", "&8{date}");
        lang.addDefault("Gui.restore_player", "&7Player: &e{player}");
        lang.addDefault("Gui.restore_world", "&7World: &e{world}");
        lang.addDefault("Gui.restore_click", "&aClick to recover");
        lang.addDefault("Gui.restore_prev", "&ePrevious page");
        lang.addDefault("Gui.restore_next", "&eNext page");
    }
}
