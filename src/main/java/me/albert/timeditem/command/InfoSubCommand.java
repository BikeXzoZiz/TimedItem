package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.PdcCompat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;

/** /ti info - prints the timer kind and remaining time of the held item. */
public final class InfoSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public InfoSubCommand(TimedItemPlugin plugin) {
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
        if (type == null) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        String[] owner = PdcCompat.readOwner(item);
        if (owner != null) {
            player.sendMessage(config.getPrefix() + config.getInfoOwner()
                    .replace("{player}", owner[1]));
        }
        String message;
        if (type == TimeLore.Type.EXPIRE) {
            LocalDateTime expiry = TimeLore.expiryOf(item, config);
            if (expiry == null) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
            String pretty = remaining > 0 ? TimeLore.formatDuration(remaining, config)
                    : "0" + config.getUnitS();
            message = config.getInfoRemaining().replace("{time}",
                    pretty + " &8(" + TimeLore.LORE_FORMAT.format(expiry) + ")");
        } else if (type == TimeLore.Type.PAUSED) {
            long seconds = TimeLore.pausedSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            message = config.getTimerPaused().replace("{time}",
                    TimeLore.formatDuration(seconds, config));
        } else if (type == TimeLore.Type.PLAYTIME || type == TimeLore.Type.HELD) {
            long seconds = type == TimeLore.Type.PLAYTIME
                    ? TimeLore.playtimeSeconds(item, config)
                    : TimeLore.heldSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            message = config.getInfoRemaining().replace("{time}",
                    TimeLore.formatDuration(seconds, config));
        } else {
            long seconds = TimeLore.lifetimeSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            message = config.getInfoLifetime().replace("{time}",
                    TimeLore.formatDuration(seconds, config));
        }
        player.sendMessage(config.getPrefix() + PluginConfig.color(message));
    }
}
