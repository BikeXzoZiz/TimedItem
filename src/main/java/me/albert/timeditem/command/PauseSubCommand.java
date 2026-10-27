package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.storage.AuditRecord;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;

/** /ti pause - freezes a running expiry timer (date-based timers only). */
public final class PauseSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public PauseSubCommand(TimedItemPlugin plugin) {
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
        if (type == TimeLore.Type.PAUSED) {
            player.sendMessage(config.getPrefix() + config.getAlreadyPaused());
            return;
        }
        if (type != TimeLore.Type.EXPIRE) {
            player.sendMessage(config.getPrefix() + config.getTimerNotStarted());
            return;
        }
        LocalDateTime expiry = TimeLore.expiryOf(item, config);
        if (expiry == null) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
        if (remaining <= 0) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        TimeLore.applyPaused(item, config, remaining);
        player.sendMessage(config.getPrefix() + config.getTimerPaused()
                .replace("{time}", TimeLore.formatDuration(remaining, config)));
        CommandSupport.audit(plugin, player, AuditRecord.PAUSE, player.getName(), item,
                TimeLore.formatDuration(remaining, config));
    }
}
