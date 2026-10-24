package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TimedItemPlugin extends JavaPlugin {

    private PluginConfig config;
    private ExpiryListener listener;
    private BukkitTask expiryTask;

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
        startExpiryCheckTask();
        getLogger().info("TimedItem enabled - timed items expire when their timer runs out.");
    }

    @Override
    public void onDisable() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }

    /** (Re)starts the periodic expiry check using the current config values. */
    public void startExpiryCheckTask() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        if (!config.isExpireCheckEnabled()) {
            return;
        }
        long ticks = config.getExpireCheckSeconds() * 20L;
        expiryTask = Bukkit.getScheduler().runTaskTimer(this, new ExpiryCheckTask(this, listener), ticks, ticks);
    }

    public PluginConfig pluginConfig() {
        return config;
    }
}
