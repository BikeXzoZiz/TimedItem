package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test harness entry point: registers /titest and refuses to run
 * without TimedItem being present.
 */
public final class HarnessPlugin extends JavaPlugin {

    private TimedItemPlugin timedItem;
    private Suite suite;

    @Override
    public void onEnable() {
        TimedItemPlugin timed = (TimedItemPlugin) Bukkit.getPluginManager().getPlugin("TimedItem");
        if (timed == null || !timed.isEnabled()) {
            getLogger().severe("TimedItem is not enabled - the harness has nothing to test.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.timedItem = timed;
        this.suite = new Suite(this);
        PluginCommand command = getCommand("titest");
        if (command != null) {
            command.setExecutor(suite);
            command.setTabCompleter(suite);
        }
        getLogger().info("Test harness ready - /titest run all (" + F.isFolia() + " platform)");
    }

    TimedItemPlugin timedItem() {
        return timedItem;
    }
}
