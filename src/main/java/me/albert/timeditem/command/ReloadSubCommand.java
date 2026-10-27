package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.SchedulerCompat;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.command.CommandSender;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * /ti reload - loads config.yml and the language file on an async
 * thread (disk I/O + parsing never touch the tick loop), then swaps
 * the new immutable snapshot in on the main/global thread and
 * re-applies everything that follows the config. A failed load keeps
 * the old snapshot active; only one reload runs at a time.
 */
public final class ReloadSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;
    private final AtomicBoolean inFlight = new AtomicBoolean();

    public ReloadSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        PluginConfig config = plugin.pluginConfig();
        if (!inFlight.compareAndSet(false, true)) {
            sender.sendMessage(config.getPrefix() + config.getReloadInProgress());
            return;
        }
        SchedulerCompat.runAsync(plugin, () -> {
            try {
                PluginConfig fresh = PluginConfig.load(plugin);
                SchedulerCompat.runGlobal(plugin, () -> {
                    plugin.applyConfig(fresh);
                    PluginConfig now = plugin.pluginConfig();
                    sender.sendMessage(now.getPrefix() + now.getConfigReload());
                });
            } catch (Throwable t) {
                plugin.getLogger().severe("Config reload failed - keeping the previous config: " + t);
                PluginConfig now = plugin.pluginConfig();
                String message = now.getPrefix() + now.getReloadFailed()
                        .replace("{error}", String.valueOf(t.getMessage()));
                SchedulerCompat.runGlobal(plugin, () -> sender.sendMessage(message));
            } finally {
                inFlight.set(false);
            }
        });
    }
}
