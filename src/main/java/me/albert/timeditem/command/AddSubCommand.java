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

/**
 * /ti add <time> - extends whatever timer the held item currently has,
 * for every timer kind (expire, lifetime, paused, playtime, held).
 */
public final class AddSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public AddSubCommand(TimedItemPlugin plugin) {
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
        if (args.length < 2) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        long delta = CommandSupport.parseDuration(plugin, args);
        if (delta <= 0) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        if (TimeLore.isBlacklisted(item, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type == TimeLore.Type.EXPIRE) {
            LocalDateTime expiry = TimeLore.expiryOf(item, config);
            if (expiry == null) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
            long newRemaining = Math.max(0, remaining) + delta;
            TimeLore.applyExpiry(item, config, newRemaining);
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newRemaining, config)));
            CommandSupport.audit(plugin, player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config));
        } else if (type == TimeLore.Type.LIFETIME) {
            long seconds = TimeLore.lifetimeSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long newSeconds = seconds + delta;
            TimeLore.applyLifetime(item, config, newSeconds);
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newSeconds, config)));
            CommandSupport.audit(plugin, player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config));
        } else if (type == TimeLore.Type.PAUSED) {
            long seconds = TimeLore.pausedSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long newSeconds = seconds + delta;
            TimeLore.applyPaused(item, config, newSeconds);
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newSeconds, config)));
            CommandSupport.audit(plugin, player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config));
        } else if (type == TimeLore.Type.PLAYTIME || type == TimeLore.Type.HELD) {
            long seconds = type == TimeLore.Type.PLAYTIME
                    ? TimeLore.playtimeSeconds(item, config)
                    : TimeLore.heldSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long newSeconds = seconds + delta;
            if (type == TimeLore.Type.PLAYTIME) {
                TimeLore.applyPlaytime(item, config, newSeconds);
            } else {
                TimeLore.applyHeld(item, config, newSeconds);
            }
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newSeconds, config)));
            CommandSupport.audit(plugin, player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config));
        } else {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
        }
    }
}
