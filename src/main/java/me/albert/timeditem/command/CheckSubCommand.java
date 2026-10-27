package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * /ti check <player> - lists every timed item the target carries
 * (slot, item, timer kind, remaining). Online players only: reading
 * an offline player's inventory is deliberately not supported.
 */
public final class CheckSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public CheckSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        PluginConfig config = plugin.pluginConfig();
        if (args.length < 2) {
            sender.sendMessage(config.getPrefix() + config.getCheckUsage());
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(config.getPrefix()
                    + config.getCheckOffline().replace("{player}", args[1]));
            return;
        }
        sender.sendMessage(config.getPrefix() + config.getCheckHeader()
                .replace("{player}", target.getName()));
        ItemStack[] contents = target.getInventory().getContents();
        int found = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            TimeLore.TimerInfo info = TimeLore.analyze(item, config);
            if (info == null) {
                continue;
            }
            found++;
            long remaining;
            if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
                remaining = Math.max(0L,
                        Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds());
            } else {
                remaining = Math.max(0L, info.getSeconds());
            }
            String custom = plugin.hooks() == null ? null : plugin.hooks().itemId(item);
            String itemDesc = custom == null ? item.getType().name() : custom;
            sender.sendMessage(config.getPrefix() + config.getCheckLine()
                    .replace("{slot}", String.valueOf(slot))
                    .replace("{item}", itemDesc)
                    .replace("{type}", info.getType().name().toLowerCase(Locale.ROOT))
                    .replace("{time}", TimeLore.formatDuration(remaining, config)));
        }
        if (found == 0) {
            sender.sendMessage(config.getPrefix()
                    + config.getCheckEmpty().replace("{player}", target.getName()));
        }
    }
}
