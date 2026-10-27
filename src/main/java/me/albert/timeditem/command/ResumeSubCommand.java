package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.storage.AuditRecord;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /ti resume - turns a paused timer back into a running expiry. */
public final class ResumeSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public ResumeSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        Player player = CommandSupport.requirePlayer(plugin, sender);
        if (player == null) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        ItemStack item = CommandSupport.mainHand(plugin, player);
        if (item == null) {
            return;
        }
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type != TimeLore.Type.PAUSED) {
            if (type == TimeLore.Type.EXPIRE) {
                player.sendMessage(config.getPrefix() + config.getAlreadyRunning());
            } else {
                player.sendMessage(config.getPrefix() + config.getTimerNotStarted());
            }
            return;
        }
        long seconds = TimeLore.pausedSeconds(item, config);
        if (seconds <= 0) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        TimeLore.applyExpiry(item, config, seconds);
        player.sendMessage(config.getPrefix() + config.getTimerResumed()
                .replace("{time}", TimeLore.formatDuration(seconds, config)));
        CommandSupport.audit(plugin, player, AuditRecord.RESUME, player.getName(), item,
                TimeLore.formatDuration(seconds, config));
    }
}
