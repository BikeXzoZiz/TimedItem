package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

public final class TimedItemCommand implements CommandExecutor {

    private final TimedItemPlugin plugin;

    public TimedItemCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PluginConfig config = plugin.pluginConfig();
        if (args.length == 0) {
            sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            config.load();
            plugin.startExpiryCheckTask();
            sender.sendMessage(config.getPrefix() + config.getConfigReload());
            return true;
        }
        if (sub.equals("set")) {
            handleSet(sender, args, config);
            return true;
        }
        if (sub.equals("remove")) {
            Player player = requirePlayer(sender, config);
            if (player != null) {
                ItemStack item = mainHand(player, config);
                if (item != null) {
                    TimeLore.removeTimer(item, config);
                    player.sendMessage(config.getPrefix() + config.getItemCleared());
                }
            }
            return true;
        }
        if (sub.equals("info")) {
            handleInfo(sender, config);
            return true;
        }
        sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
        return true;
    }

    private void handleSet(CommandSender sender, String[] args, PluginConfig config) {
        Player player = requirePlayer(sender, config);
        if (player == null) {
            return;
        }
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        StringBuilder input = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            input.append(args[i]);
        }
        long seconds = DurationParser.parse(input.toString(), config.unitSeconds());
        if (seconds <= 0) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        TimeLore.applyLifetime(item, config, seconds);
        if (!config.isStartDateCalculateOnClick()) {
            TimeLore.startLifetime(item, config);
        }
        String pretty = TimeLore.formatDuration(seconds, config);
        player.sendMessage(config.getPrefix() + config.getTimeSet().replace("{time}", pretty));
    }

    private void handleInfo(CommandSender sender, PluginConfig config) {
        Player player = requirePlayer(sender, config);
        if (player == null) {
            return;
        }
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type == null) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        String message;
        if (type == TimeLore.Type.EXPIRE) {
            LocalDateTime expiry = TimeLore.expiryOf(item, config);
            if (expiry == null) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
            String pretty = remaining > 0 ? TimeLore.formatDuration(remaining, config) : "0" + config.getUnitS();
            message = config.getInfoRemaining().replace("{time}",
                    pretty + " &8(" + TimeLore.LORE_FORMAT.format(expiry) + ")");
        } else {
            long seconds = TimeLore.lifetimeSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            message = config.getInfoLifetime().replace("{time}", TimeLore.formatDuration(seconds, config));
        }
        player.sendMessage(config.getPrefix() + PluginConfig.color(message));
    }

    private Player requirePlayer(CommandSender sender, PluginConfig config) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        sender.sendMessage(config.getPrefix() + config.getPlayerOnly());
        return null;
    }

    private ItemStack mainHand(Player player, PluginConfig config) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(config.getPrefix() + config.getNoItem());
            return null;
        }
        return item;
    }
}
