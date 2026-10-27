package me.albert.timeditem;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Read-only view over the loaded configuration: messages from the
 * language file ({@link Messages}) plus settings from config.yml
 * ({@link Settings}).
 *
 * Instances are immutable snapshots built by {@link ConfigLoader}. A
 * /ti reload builds a fresh instance off the main thread and swaps it
 * in through a volatile field on {@link TimedItemPlugin} - readers
 * (including background tasks) always see one complete snapshot, never
 * a half-applied mix, so no field here needs volatile or locking.
 */
public final class PluginConfig {

    private final Messages messages;
    private final Settings settings;

    PluginConfig(Messages messages, Settings settings) {
        this.messages = messages;
        this.settings = settings;
    }

    /** Loads config.yml + the language file and returns a new snapshot. */
    public static PluginConfig load(TimedItemPlugin plugin) {
        return ConfigLoader.load(plugin);
    }

    /** Translates &amp;-codes to chat colors. */
    public static String color(String text) {
        return ConfigLoader.color(text);
    }

    public String getPrefix() {
        return messages.prefix;
    }

    public String getExpire() {
        return messages.expire;
    }

    public String getExpireSoon() {
        return messages.expireSoon;
    }

    public String getNoItem() {
        return messages.noItem;
    }

    public String getPlayerOnly() {
        return messages.playerOnly;
    }

    public String getNoTimer() {
        return messages.noTimer;
    }

    public String getItemCleared() {
        return messages.itemCleared;
    }

    public String getTimeAdded() {
        return messages.timeAdded;
    }

    public String getTimerPaused() {
        return messages.timerPaused;
    }

    public String getTimerResumed() {
        return messages.timerResumed;
    }

    public String getTimerNotStarted() {
        return messages.timerNotStarted;
    }

    public String getAlreadyPaused() {
        return messages.alreadyPaused;
    }

    public String getAlreadyRunning() {
        return messages.alreadyRunning;
    }

    public String getInvalidUsage() {
        return messages.invalidUsage;
    }

    public String getConfigReload() {
        return messages.configReload;
    }

    /** Sent when /ti reload is requested while one is already running. */
    public String getReloadInProgress() {
        return messages.reloadInProgress;
    }

    /** Sent when the new config failed to load; the old one stays active. */
    public String getReloadFailed() {
        return messages.reloadFailed;
    }

    public String getTimeSet() {
        return messages.timeSet;
    }

    public String getInfoRemaining() {
        return messages.infoRemaining;
    }

    public String getInfoLifetime() {
        return messages.infoLifetime;
    }

    public String getGiveSent() {
        return messages.giveSent;
    }

    public String getGiveReceived() {
        return messages.giveReceived;
    }

    public String getTargetNotFound() {
        return messages.targetNotFound;
    }

    public String getMaterialBlacklisted() {
        return messages.materialBlacklisted;
    }

    public String getRestoreGiven() {
        return messages.restoreGiven;
    }

    public String getRestoreEmpty() {
        return messages.restoreEmpty;
    }

    public String getUpdateAvailable() {
        return messages.updateAvailable;
    }

    public String getDebugStatus() {
        return messages.debugStatus;
    }

    public String getDebugOn() {
        return messages.debugOn;
    }

    public String getDebugOff() {
        return messages.debugOff;
    }

    public String getDebugExported() {
        return messages.debugExported;
    }

    public String getDebugExportFailed() {
        return messages.debugExportFailed;
    }

    public String getDebugUsage() {
        return messages.debugUsage;
    }

    public String getItemNotFound() {
        return messages.itemNotFound;
    }

    public String getFindUsage() {
        return messages.findUsage;
    }

    public String getFindNoMatch() {
        return messages.findNoMatch;
    }

    public String getFindResult() {
        return messages.findResult;
    }

    public String getFindTruncated() {
        return messages.findTruncated;
    }

    public String getFindHitPlayer() {
        return messages.findHitPlayer;
    }

    public String getFindHitContainer() {
        return messages.findHitContainer;
    }

    public String getFindHitGround() {
        return messages.findHitGround;
    }

    public String getStatsHeader() {
        return messages.statsHeader;
    }

    public String getStatsLine() {
        return messages.statsLine;
    }

    public String getStatsTotal() {
        return messages.statsTotal;
    }

    public String getStatsEmpty() {
        return messages.statsEmpty;
    }

    public String getAuditHeader() {
        return messages.auditHeader;
    }

    public String getAuditLine() {
        return messages.auditLine;
    }

    public String getAuditEmpty() {
        return messages.auditEmpty;
    }

    public String getTransformMessage() {
        return messages.transformMessage;
    }

    public String getGraceMessage() {
        return messages.graceMessage;
    }

    public String getRenewalSuccess() {
        return messages.renewalSuccess;
    }

    public String getRenewalTokenMissing() {
        return messages.renewalTokenMissing;
    }

    public String getRestoreReminder() {
        return messages.restoreReminder;
    }

    public String getTransformUnknown() {
        return messages.transformUnknown;
    }

    public String getAntiHideBlocked() {
        return messages.antiHideBlocked;
    }

    public String getCheckUsage() {
        return messages.checkUsage;
    }

    public String getCheckOffline() {
        return messages.checkOffline;
    }

    public String getCheckHeader() {
        return messages.checkHeader;
    }

    public String getCheckLine() {
        return messages.checkLine;
    }

    public String getCheckEmpty() {
        return messages.checkEmpty;
    }

    public String getGuiAdminTitle() {
        return messages.guiAdminTitle;
    }

    public String getGuiAdminOwner() {
        return messages.guiAdminOwner;
    }

    public String getGuiAdminType() {
        return messages.guiAdminType;
    }

    public String getGuiAdminExpireNow() {
        return messages.guiAdminExpireNow;
    }

    public String getGuiAdminRemoveTimer() {
        return messages.guiAdminRemoveTimer;
    }

    public String getGuiAdminEmpty() {
        return messages.guiAdminEmpty;
    }

    public String getGuiAdminPrev() {
        return messages.guiAdminPrev;
    }

    public String getGuiAdminNext() {
        return messages.guiAdminNext;
    }

    public String getGuiAdminExpired() {
        return messages.guiAdminExpired;
    }

    public String getGuiAdminRemoved() {
        return messages.guiAdminRemoved;
    }

    public String getInfoOwner() {
        return messages.infoOwner;
    }

    public String getReturnedToOwner() {
        return messages.returnedToOwner;
    }

    public String getSellBlocked() {
        return messages.sellBlocked;
    }

    public String getRestoreLoading() {
        return messages.restoreLoading;
    }

    public String getRestoreNetworkTitle() {
        return messages.restoreNetworkTitle;
    }

    public String getExpiresAtPrefix() {
        return messages.expiresAtPrefix;
    }

    public String getUsefulLifePrefix() {
        return messages.usefulLifePrefix;
    }

    public String getPausedPrefix() {
        return messages.pausedPrefix;
    }

    public String getPlaytimePrefix() {
        return messages.playtimePrefix;
    }

    public String getHeldPrefix() {
        return messages.heldPrefix;
    }

    public String getUnitS() {
        return messages.unitS;
    }

    public String getUnitM() {
        return messages.unitM;
    }

    public String getUnitH() {
        return messages.unitH;
    }

    public String getUnitD() {
        return messages.unitD;
    }

    /** Configured unit labels (lowercased) mapped to their length in seconds. */
    public Map<String, Long> unitSeconds() {
        return messages.unitSeconds;
    }

    public String getGuiMenuTitle() {
        return messages.guiMenuTitle;
    }

    public String getGuiPresetLore() {
        return messages.guiPresetLore;
    }

    public String getGuiToggleName() {
        return messages.guiToggleName;
    }

    public String getGuiToggleLore() {
        return messages.guiToggleLore;
    }

    public String getGuiRemoveName() {
        return messages.guiRemoveName;
    }

    public String getGuiRemoveLore() {
        return messages.guiRemoveLore;
    }

    public String getGuiCloseName() {
        return messages.guiCloseName;
    }

    public String getGuiStatusName() {
        return messages.guiStatusName;
    }

    public String getGuiStatusNoTimer() {
        return messages.guiStatusNoTimer;
    }

    public String getGuiStatusTypeExpire() {
        return messages.guiStatusTypeExpire;
    }

    public String getGuiStatusTypePaused() {
        return messages.guiStatusTypePaused;
    }

    public String getGuiStatusTypeLifetime() {
        return messages.guiStatusTypeLifetime;
    }

    public String getGuiStatusTypePlaytime() {
        return messages.guiStatusTypePlaytime;
    }

    public String getGuiStatusTypeHeld() {
        return messages.guiStatusTypeHeld;
    }

    public String getGuiStatusRemaining() {
        return messages.guiStatusRemaining;
    }

    public String getGuiStatusExpiresAt() {
        return messages.guiStatusExpiresAt;
    }

    public String getGuiStatusDuration() {
        return messages.guiStatusDuration;
    }

    public String getGuiRestoreTitle() {
        return messages.guiRestoreTitle;
    }

    public String getGuiRestoreDate() {
        return messages.guiRestoreDate;
    }

    public String getGuiRestorePlayer() {
        return messages.guiRestorePlayer;
    }

    public String getGuiRestoreWorld() {
        return messages.guiRestoreWorld;
    }

    public String getGuiRestoreClick() {
        return messages.guiRestoreClick;
    }

    public String getGuiRestorePrev() {
        return messages.guiRestorePrev;
    }

    public String getGuiRestoreNext() {
        return messages.guiRestoreNext;
    }

    public boolean isStartDateCalculateOnClick() {
        return settings.startDateCalculateOnClick;
    }

    public boolean isResetPlayerHealthIfArmor() {
        return settings.resetPlayerHealthIfArmor;
    }

    public boolean isMetricsEnabled() {
        return settings.metricsEnabled;
    }

    /** Verbose [Debug] logging; a session switch on top of Settings.debug. */
    public boolean isDebugEnabled() {
        return settings.debugMode;
    }

    public boolean isExpireCheckEnabled() {
        return settings.expireCheckEnabled;
    }

    public int getExpireCheckSeconds() {
        return settings.expireCheckSeconds;
    }

    public boolean isScanWorldItems() {
        return settings.scanWorldItems;
    }

    /** True when creative players are skipped entirely (no expiry, no countdown). */
    public boolean isIgnoreCreative() {
        return settings.ignoreCreative;
    }

    public boolean isWarningEnabled() {
        return settings.warningEnabled;
    }

    public int getWarningSeconds() {
        return settings.warningSeconds;
    }

    /** Warning thresholds in seconds, highest first. */
    public List<Integer> getWarningStages() {
        return settings.warningStages;
    }

    public String getWarningSound() {
        return settings.warningSound;
    }

    public float getWarningVolume() {
        return settings.warningVolume;
    }

    public float getWarningPitch() {
        return settings.warningPitch;
    }

    public boolean isParticleEnabled() {
        return settings.particleEnabled;
    }

    public String getParticleName() {
        return settings.particleName;
    }

    public int getParticleCount() {
        return settings.particleCount;
    }

    public List<String> getExpiryCommands() {
        return settings.expiryCommands;
    }

    public boolean isActionbarEnabled() {
        return settings.actionbarEnabled;
    }

    public String getActionbarFormat() {
        return settings.actionbarFormat;
    }

    public boolean isSoundEnabled() {
        return settings.soundEnabled;
    }

    public String getSoundName() {
        return settings.soundName;
    }

    public float getSoundVolume() {
        return settings.soundVolume;
    }

    public float getSoundPitch() {
        return settings.soundPitch;
    }

    public boolean isUpdateCheckEnabled() {
        return settings.updateCheckEnabled;
    }

    public String getUpdateCheckRepository() {
        return settings.updateCheckRepository;
    }

    public boolean isRecoveryEnabled() {
        return settings.recoveryEnabled;
    }

    public int getRecoveryMaxItems() {
        return settings.recoveryMaxItems;
    }

    public int getRecoveryKeepDays() {
        return settings.recoveryKeepDays;
    }

    /** Settings.storage.type: flat, sqlite, mysql or mongodb. */
    public String getStorageType() {
        return settings.storageType;
    }

    public String getMysqlHost() {
        return settings.mysqlHost;
    }

    public int getMysqlPort() {
        return settings.mysqlPort;
    }

    public String getMysqlDatabase() {
        return settings.mysqlDatabase;
    }

    public String getMysqlUser() {
        return settings.mysqlUser;
    }

    public String getMysqlPassword() {
        return settings.mysqlPassword;
    }

    public boolean isMysqlUseSsl() {
        return settings.mysqlUseSsl;
    }

    public int getMysqlPoolSize() {
        return settings.mysqlPoolSize;
    }

    public String getSqliteFile() {
        return settings.sqliteFile;
    }

    public String getMongodbUri() {
        return settings.mongodbUri;
    }

    public String getMongodbDatabase() {
        return settings.mongodbDatabase;
    }

    /** Maximum audit entries kept by the storage backend. */
    public int getAuditHistory() {
        return settings.auditHistory;
    }

    public boolean isDiscordWebhookEnabled() {
        return settings.discordWebhookEnabled;
    }

    public String getDiscordWebhookUrl() {
        return settings.discordWebhookUrl;
    }

    public String getDiscordWebhookUsername() {
        return settings.discordWebhookUsername;
    }

    public String getDiscordWebhookMessage() {
        return settings.discordWebhookMessage;
    }

    public boolean isDiscordWebhookUseEmbed() {
        return settings.discordWebhookUseEmbed;
    }

    public String getDiscordWebhookEmbedTitle() {
        return settings.discordWebhookEmbedTitle;
    }

    public int getDiscordWebhookEmbedColor() {
        return settings.discordWebhookEmbedColor;
    }

    /** True when expiry transformation is enabled (Settings.expiry_transform). */
    public boolean isExpiryTransformEnabled() {
        return settings.expiryTransformEnabled;
    }

    /** Profile key used when an item carries no transform tag ("" = none). */
    public String getExpiryTransformDefault() {
        return settings.expiryTransformDefault;
    }

    /** All parsed profiles from Settings.transforms, keyed lowercase. */
    public Map<String, Transforms.Profile> transformProfiles() {
        return settings.transformProfiles;
    }

    public boolean isGraceEnabled() {
        return settings.graceEnabled;
    }

    public int getGraceSeconds() {
        return settings.graceSeconds;
    }

    public boolean isRenewalEnabled() {
        return settings.renewalEnabled;
    }

    /** Seconds added per renewal token (0 = renewal off). */
    public long getRenewalAddSeconds() {
        return settings.renewalAddSeconds;
    }

    public Material getRenewalTokenMaterial() {
        return settings.renewalTokenMaterial;
    }

    public String getRenewalTokenCustomId() {
        return settings.renewalTokenCustomId;
    }

    /** Required token display name (stripped, lowercase; "" = any name). */
    public String getRenewalTokenName() {
        return settings.renewalTokenName;
    }

    /** Parsed visual decay stages (Settings.visual_stages), most severe last index. */
    public List<VisualStages.Stage> visualStages() {
        return settings.visualStages;
    }

    public boolean isHologramEnabled() {
        return settings.hologramEnabled;
    }

    public String getHologramFormat() {
        return settings.hologramFormat;
    }

    public double getHologramHeight() {
        return settings.hologramHeight;
    }

    public boolean isLiveLoreEnabled() {
        return settings.liveLoreEnabled;
    }

    public String getLiveLoreFormat() {
        return settings.liveLoreFormat;
    }

    public boolean isContainerScanEnabled() {
        return settings.containerScanEnabled;
    }

    /** open, periodic or both. */
    public String getContainerScanMode() {
        return settings.containerScanMode;
    }

    public int getContainerScanSeconds() {
        return settings.containerScanSeconds;
    }

    public boolean isAntiHideEnabled() {
        return settings.antiHideEnabled;
    }

    public long getAntiHideBelowSeconds() {
        return settings.antiHideBelowSeconds;
    }

    public boolean isReturnToOwnerEnabled() {
        return settings.returnToOwnerEnabled;
    }

    public boolean isRemoveTimerOnReturn() {
        return settings.removeTimerOnReturn;
    }

    /** Network restore: /ti restore reads the shared database instead of local memory. */
    public boolean isRestoreNetworkEnabled() {
        return settings.restoreNetworkEnabled;
    }

    /** Materials that may never receive a timer. */
    public Set<Material> blacklistedMaterials() {
        return settings.blacklistedMaterials;
    }

    /** True unless the hook was switched off under Hooks.&lt;id&gt; in config.yml. */
    public boolean isHookEnabled(String id) {
        return !settings.disabledHooks.contains(id.toLowerCase(Locale.ROOT));
    }

    /** Hooks.shopkeepers.auto_timer - empty means the hook is off. */
    public String getShopkeepersAutoTimer() {
        return settings.shopkeepersAutoTimer;
    }

    /** Hooks.crazycrates.auto_timer - empty means the hook is off. */
    public String getCrazyCratesAutoTimer() {
        return settings.crazycratesAutoTimer;
    }
}
