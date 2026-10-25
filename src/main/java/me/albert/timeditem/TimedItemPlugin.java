package me.albert.timeditem;

import me.albert.timeditem.api.TimedItemAPI;
import me.albert.timeditem.hook.TimedItemExpansion;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TimedItemPlugin extends JavaPlugin {

    /** bStats plugin id - replace with the real id after registering the plugin on bStats.org. */
    private static final int BSTATS_PLUGIN_ID = 17777;

    private PluginConfig config;
    private ExpiryListener listener;
    private TimedItemMenu menu;
    private UpdateChecker updateChecker;
    private RecoveryStore recoveryStore;
    private RestoreMenu restoreMenu;
    private BukkitTask expiryTask;
    private BukkitTask actionbarTask;

    @Override
    public void onEnable() {
        PdcCompat.init(this);
        config = new PluginConfig(this);
        config.load();
        listener = new ExpiryListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        menu = new TimedItemMenu(this);
        Bukkit.getPluginManager().registerEvents(menu, this);
        recoveryStore = new RecoveryStore(this);
        recoveryStore.load();
        restoreMenu = new RestoreMenu(this);
        Bukkit.getPluginManager().registerEvents(restoreMenu, this);
        PluginCommand command = getCommand("timeditem");
        if (command != null) {
            command.setExecutor(new TimedItemCommand(this));
            command.setTabCompleter(new TimedItemTabCompleter());
        }
        TimedItemAPI.init(this);
        startTasks();
        updateChecker = new UpdateChecker(this);
        Bukkit.getPluginManager().registerEvents(updateChecker, this);
        updateChecker.check();
        if (config.isMetricsEnabled()) {
            new Metrics(this, BSTATS_PLUGIN_ID);
        }
        hookPlaceholderApi();
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

    public RestoreMenu restoreMenu() {
        return restoreMenu;
    }

    @Override
    public void onDisable() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        if (actionbarTask != null) {
            actionbarTask.cancel();
            actionbarTask = null;
        }
    }

    /** (Re)starts the periodic expiry check and the actionbar task from the current config. */
    public void startTasks() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        if (actionbarTask != null) {
            actionbarTask.cancel();
            actionbarTask = null;
        }
        if (config.isExpireCheckEnabled()) {
            long ticks = config.getExpireCheckSeconds() * 20L;
            expiryTask = Bukkit.getScheduler().runTaskTimer(this, new ExpiryCheckTask(this, listener), ticks, ticks);
        }
        if (config.isActionbarEnabled()) {
            actionbarTask = Bukkit.getScheduler().runTaskTimer(this, new ActionbarTask(this), 20L, 20L);
        }
    }

    public PluginConfig pluginConfig() {
        return config;
    }
}
