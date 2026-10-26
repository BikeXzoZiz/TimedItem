package me.albert.timeditem;

import me.albert.timeditem.api.TimedItemAPI;
import me.albert.timeditem.hook.HookManager;
import me.albert.timeditem.hook.TimedItemExpansion;
import me.albert.timeditem.storage.StorageWriter;
import me.albert.timeditem.storage.StorageFactory;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TimedItemPlugin extends JavaPlugin {

    /** bStats plugin id - TimedItemRewrite, https://bstats.org/plugin/bukkit/TimedItemRewrite/34946 */
    private static final int BSTATS_PLUGIN_ID = 34946;

    private PluginConfig config;
    private ExpiryService expiryService;
    private ExpiryListener listener;
    private TimedItemMenu menu;
    private UpdateChecker updateChecker;
    private RecoveryStore recoveryStore;
    private RestoreMenu restoreMenu;
    private DiscordWebhook discordWebhook;
    private FoliaGroundExpiry foliaGroundExpiry;
    private HookManager hooks;
    private StorageWriter storageWriter;
    private TrackedItems trackedItems;
    private AdminMenu adminMenu;
    private GroundHolograms holograms;
    private SchedulerCompat.Handle expiryTask;
    private SchedulerCompat.Handle actionbarTask;
    private SchedulerCompat.Handle containerScanTask;

    @Override
    public void onEnable() {
        PdcCompat.init(this);
        HologramCompat.init();
        config = new PluginConfig(this);
        config.load();
        DebugLog.install(getLogger());
        DebugLog.setEnabled(config.isDebugEnabled());
        // storage is created once and never re-created by /ti reload -
        // changing Settings.storage.type needs a restart by design
        storageWriter = new StorageWriter(StorageFactory.create(this), getLogger());
        trackedItems = new TrackedItems();
        listener = new ExpiryListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        menu = new TimedItemMenu(this);
        Bukkit.getPluginManager().registerEvents(menu, this);
        adminMenu = new AdminMenu(this);
        Bukkit.getPluginManager().registerEvents(adminMenu, this);
        recoveryStore = new RecoveryStore(this, storageWriter);
        recoveryStore.load();
        expiryService = new ExpiryService(this);
        // created before the tasks start: on Folia a region thread can
        // expire an item while onEnable is still running
        discordWebhook = new DiscordWebhook(this);
        if (SchedulerCompat.isFolia()) {
            foliaGroundExpiry = new FoliaGroundExpiry(this);
        }
        restoreMenu = new RestoreMenu(this);
        Bukkit.getPluginManager().registerEvents(restoreMenu, this);
        // container scanning (open mode); periodic runs from startTasks
        Bukkit.getPluginManager().registerEvents(new ContainerGuard(this), this);
        holograms = new GroundHolograms(this);
        holograms.start();
        PluginCommand command = getCommand("timeditem");
        if (command != null) {
            command.setExecutor(new TimedItemCommand(this));
            command.setTabCompleter(new TimedItemTabCompleter(this));
        }
        TimedItemAPI.init(this);
        hooks = HookManager.create(this);
        startTasks();
        updateChecker = new UpdateChecker(this);
        Bukkit.getPluginManager().registerEvents(updateChecker, this);
        updateChecker.check();
        if (config.isMetricsEnabled()) {
            try {
                new Metrics(this, BSTATS_PLUGIN_ID);
            } catch (Throwable t) {
                getLogger().warning("Could not start bStats metrics: " + t);
            }
        }
        hookPlaceholderApi();
        if (SchedulerCompat.isFolia()) {
            getLogger().info("Folia detected - expiry tasks run on the owning region"
                    + " threads and ground items are tracked per entity.");
        }
        getLogger().info("TimedItem enabled - timed items expire when their timer runs out.");
    }

    private void hookPlaceholderApi() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            new TimedItemExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI (%timeditem_...% placeholders).");
        } catch (Throwable t) {
            getLogger().warning("Failed to hook PlaceholderAPI: " + t);
        }
    }

    public TimedItemMenu menu() {
        return menu;
    }

    public RecoveryStore recoveryStore() {
        return recoveryStore;
    }

    public ExpiryService expiryService() {
        return expiryService;
    }

    public DiscordWebhook discordWebhook() {
        return discordWebhook;
    }

    public RestoreMenu restoreMenu() {
        return restoreMenu;
    }

    /** Async persistence behind recovery, audit and stats. */
    public StorageWriter storage() {
        return storageWriter;
    }

    /** Timed items seen by the last periodic scan (admin overview). */
    public TrackedItems trackedItems() {
        return trackedItems;
    }

    public AdminMenu adminMenu() {
        return adminMenu;
    }

    /** Integrations with other item plugins (Hooks.* config section). */
    public HookManager hooks() {
        return hooks;
    }

    @Override
    public void onDisable() {
        cancelTask(expiryTask);
        expiryTask = null;
        cancelTask(actionbarTask);
        actionbarTask = null;
        cancelTask(containerScanTask);
        containerScanTask = null;
        if (holograms != null) {
            holograms.stop();
        }
        if (storageWriter != null) {
            storageWriter.close();
        }
        if (discordWebhook != null) {
            discordWebhook.close();
        }
        if (foliaGroundExpiry != null) {
            foliaGroundExpiry.stop();
        }
        if (hooks != null) {
            hooks.close();
        }
        DebugLog.uninstall();
    }

    private void cancelTask(SchedulerCompat.Handle handle) {
        if (handle != null) {
            handle.cancel();
        }
    }

    /** (Re)starts the periodic expiry check and the actionbar task from the current config. */
    public void startTasks() {
        cancelTask(expiryTask);
        expiryTask = null;
        cancelTask(actionbarTask);
        actionbarTask = null;
        cancelTask(containerScanTask);
        containerScanTask = null;
        if (config.isExpireCheckEnabled()) {
            long ticks = config.getExpireCheckSeconds() * 20L;
            expiryTask = SchedulerCompat.runGlobalTimer(this, ticks, ticks,
                    new ExpiryCheckTask(this, listener));
        }
        if (config.isActionbarEnabled()) {
            actionbarTask = SchedulerCompat.runGlobalTimer(this, 20L, 20L, new ActionbarTask(this));
        }
        if (config.isContainerScanEnabled() && !SchedulerCompat.isFolia()) {
            String mode = config.getContainerScanMode();
            if (mode.equals("periodic") || mode.equals("both")) {
                long ticks = config.getContainerScanSeconds() * 20L;
                containerScanTask = SchedulerCompat.runGlobalTimer(this, ticks, ticks,
                        new ContainerScanTask(this));
            }
        }
        if (foliaGroundExpiry != null) {
            // restart so a changed interval applies to every tracking task
            foliaGroundExpiry.stop();
            if (config.isScanWorldItems()) {
                foliaGroundExpiry.start(config.getExpireCheckSeconds() * 20L);
            }
        }
    }

    public PluginConfig pluginConfig() {
        return config;
    }
}
