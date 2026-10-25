package me.albert.timeditem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

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
    private boolean expireCheckEnabled;
    private int expireCheckSeconds;
    private boolean scanWorldItems;
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
    private Set<Material> blacklistedMaterials;

    public PluginConfig(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        FileConfiguration raw = plugin.getConfig();
        raw.addDefault("config-version", 5);
        raw.addDefault("Messages.prefix", "&6[TimedItem] &r");
        raw.addDefault("Messages.expire", "&cYour item has expired and was removed!");
        raw.addDefault("Messages.expire_soon", "&eYour item will expire in &c{time}&e!");
        raw.addDefault("Messages.no_item", "&cHold the item you want to manage first.");
        raw.addDefault("Messages.player_only", "&cOnly players can use this command.");
        raw.addDefault("Messages.no_timer", "&7The item in your hand has no timer.");
        raw.addDefault("Messages.item_cleared", "&aTimer removed from the item in your hand.");
        raw.addDefault("Messages.time_added", "&aTime added. Remaining: &e{time}");
        raw.addDefault("Messages.timer_paused", "&aTimer paused with &e{time}&a remaining.");
        raw.addDefault("Messages.timer_resumed", "&aTimer resumed. Expires in &e{time}&a.");
        raw.addDefault("Messages.timer_not_started", "&7The timer on this item has not started yet.");
        raw.addDefault("Messages.already_paused", "&7The timer on this item is already paused.");
        raw.addDefault("Messages.already_running", "&7The timer on this item is already running.");
        raw.addDefault("Messages.invalid_usage",
                "&cUsage: /ti set <time>, /ti add <time>, /ti give <player> <time> [amount], /ti pause, /ti resume, /ti remove, /ti info, /ti menu, /ti restore, /ti reload");
        raw.addDefault("Messages.config_reload", "&aTimedItem config reloaded.");
        raw.addDefault("Messages.time_set", "&aTimer set: &e{time}");
        raw.addDefault("Messages.info_remaining", "&7Time remaining: &e{time}");
        raw.addDefault("Messages.info_lifetime", "&7Lifetime: &e{time} &7(starts on first use)");
        raw.addDefault("Messages.give_sent", "&aGave a timed item (&e{time}&a) to &e{player}&a.");
        raw.addDefault("Messages.give_received", "&aYou received a timed item expiring in &e{time}&a.");
        raw.addDefault("Messages.target_not_found", "&cPlayer &e{player} &cis not online.");
        raw.addDefault("Messages.material_blacklisted", "&cThis item's material cannot carry a TimedItem timer.");
        raw.addDefault("Messages.restore_given", "&aRecovered the item from the expiry log.");
        raw.addDefault("Messages.restore_empty", "&7The expiry recovery log is empty.");
        raw.addDefault("Messages.update_available", "&eTimedItem &6{version} &eis available: &7{url}");
        raw.addDefault("DateFormat.expires_at", "&cExpires: ");
        raw.addDefault("DateFormat.useful_life", "&7Lifetime: ");
        raw.addDefault("DateFormat.paused", "&7Paused: ");
        raw.addDefault("DateFormat.s", "s");
        raw.addDefault("DateFormat.m", "m");
        raw.addDefault("DateFormat.h", "h");
        raw.addDefault("DateFormat.d", "d");
        raw.addDefault("Settings.start_date_calculate_on_click", true);
        raw.addDefault("Settings.reset_player_health_if_armor", true);
        raw.addDefault("Settings.metrics", true);
        raw.addDefault("Settings.expire_check.enabled", true);
        raw.addDefault("Settings.expire_check.interval_seconds", 10);
        raw.addDefault("Settings.expire_check.scan_world_items", false);
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

        prefix = color(raw.getString("Messages.prefix"));
        expire = color(raw.getString("Messages.expire"));
        expireSoon = color(raw.getString("Messages.expire_soon"));
        noItem = color(raw.getString("Messages.no_item"));
        playerOnly = color(raw.getString("Messages.player_only"));
        noTimer = color(raw.getString("Messages.no_timer"));
        itemCleared = color(raw.getString("Messages.item_cleared"));
        timeAdded = color(raw.getString("Messages.time_added"));
        timerPaused = color(raw.getString("Messages.timer_paused"));
        timerResumed = color(raw.getString("Messages.timer_resumed"));
        timerNotStarted = color(raw.getString("Messages.timer_not_started"));
        alreadyPaused = color(raw.getString("Messages.already_paused"));
        alreadyRunning = color(raw.getString("Messages.already_running"));
        invalidUsage = color(raw.getString("Messages.invalid_usage"));
        configReload = color(raw.getString("Messages.config_reload"));
        timeSet = color(raw.getString("Messages.time_set"));
        infoRemaining = color(raw.getString("Messages.info_remaining"));
        infoLifetime = color(raw.getString("Messages.info_lifetime"));
        giveSent = color(raw.getString("Messages.give_sent"));
        giveReceived = color(raw.getString("Messages.give_received"));
        targetNotFound = color(raw.getString("Messages.target_not_found"));
        materialBlacklisted = color(raw.getString("Messages.material_blacklisted"));
        restoreGiven = color(raw.getString("Messages.restore_given"));
        restoreEmpty = color(raw.getString("Messages.restore_empty"));
        updateAvailable = color(raw.getString("Messages.update_available"));

        expiresAtPrefix = color(raw.getString("DateFormat.expires_at"));
        usefulLifePrefix = color(raw.getString("DateFormat.useful_life"));
        pausedPrefix = color(raw.getString("DateFormat.paused"));
        unitS = raw.getString("DateFormat.s");
        unitM = raw.getString("DateFormat.m");
        unitH = raw.getString("DateFormat.h");
        unitD = raw.getString("DateFormat.d");

        Map<String, Long> units = new HashMap<String, Long>();
        units.put(unitS.toLowerCase(Locale.ROOT), 1L);
        units.put(unitM.toLowerCase(Locale.ROOT), 60L);
        units.put(unitH.toLowerCase(Locale.ROOT), 3600L);
        units.put(unitD.toLowerCase(Locale.ROOT), 86400L);
        unitSeconds = Collections.unmodifiableMap(units);

        startDateCalculateOnClick = raw.getBoolean("Settings.start_date_calculate_on_click", true);
        resetPlayerHealthIfArmor = raw.getBoolean("Settings.reset_player_health_if_armor", true);
        metricsEnabled = raw.getBoolean("Settings.metrics", true);
        expireCheckEnabled = raw.getBoolean("Settings.expire_check.enabled", true);
        expireCheckSeconds = Math.max(1, raw.getInt("Settings.expire_check.interval_seconds", 10));
        scanWorldItems = raw.getBoolean("Settings.expire_check.scan_world_items", false);
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

    public boolean isExpireCheckEnabled() {
        return expireCheckEnabled;
    }

    public int getExpireCheckSeconds() {
        return expireCheckSeconds;
    }

    public boolean isScanWorldItems() {
        return scanWorldItems;
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

    /** Materials that may never receive a timer. */
    public Set<Material> blacklistedMaterials() {
        return blacklistedMaterials;
    }
}
