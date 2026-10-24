package me.albert.timeditem;

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
    private BukkitTask expiryTask;
    private BukkitTask actionbarTask;

    @Override
    public void onEnable() {
        config = new PluginConfig(this);
        config.load();
        listener = new ExpiryListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        PluginCommand command = getCommand("timeditem");
        if (command != null) {
            command.setExecutor(new TimedItemCommand(this));
        }
        startTasks();
        if (config.isMetricsEnabled()) {
            new Metrics(this, BSTATS_PLUGIN_ID);
        }
        getLogger().info("TimedItem enabled - timed items expire when their timer runs out.");
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
