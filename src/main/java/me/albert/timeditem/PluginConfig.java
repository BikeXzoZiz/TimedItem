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

    private String expiresAtPrefix;
    private String usefulLifePrefix;
    private String pausedPrefix;
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

    public PluginConfig(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        FileConfiguration raw = plugin.getConfig();
        raw.addDefault("config-version", 7);
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
        raw.addDefault("Settings.blacklist.materials", new java.util.ArrayList<String>());
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

        expiresAtPrefix = color(lang.getString("DateFormat.expires_at"));
        usefulLifePrefix = color(lang.getString("DateFormat.useful_life"));
        pausedPrefix = color(lang.getString("DateFormat.paused"));
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

    public String getExpiresAtPrefix() {
        return expiresAtPrefix;
    }

    public String getUsefulLifePrefix() {
        return usefulLifePrefix;
    }

    public String getPausedPrefix() {
        return pausedPrefix;
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
                "&cUsage: /ti set <time>, /ti add <time>, /ti give <player> <time> [amount], /ti pause, /ti resume, /ti remove, /ti info, /ti menu, /ti restore, /ti debug, /ti reload");
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
        lang.addDefault("DateFormat.expires_at", "&cExpires: ");
        lang.addDefault("DateFormat.useful_life", "&7Lifetime: ");
        lang.addDefault("DateFormat.paused", "&7Paused: ");
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
