package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.storage.AuditRecord;
import me.albert.timeditem.PdcCompat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * /ti set <time> (or "playtime <time>" / "held <time>") - stamps a
 * timer on the held item, replacing any previous one.
 */
public final class SetSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public SetSubCommand(TimedItemPlugin plugin) {
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
        CommandSupport.ArgScan scan = CommandSupport.scanArgs(args);
        // /ti set playtime <time> / /ti set held <time> (2.9+)
        String countdownType = null;
        String[] durationArgs = scan.args;
        if (scan.args.length >= 2) {
            String lower = scan.args[1].toLowerCase(Locale.ROOT);
            if (lower.equals("playtime") || lower.equals("held")) {
                countdownType = lower;
                durationArgs = shiftFirst(scan.args);
            }
        }
        if (durationArgs.length < 2) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        long seconds = CommandSupport.parseDuration(plugin, durationArgs);
        if (seconds <= 0) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        if (!CommandSupport.checkTransform(plugin, scan.transformKey, player)) {
            return;
        }
        if (TimeLore.isBlacklisted(item, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        if ("playtime".equals(countdownType)) {
            TimeLore.applyPlaytime(item, config, seconds);
        } else if ("held".equals(countdownType)) {
            TimeLore.applyHeld(item, config, seconds);
        } else {
            TimeLore.applyLifetime(item, config, seconds);
            if (!config.isStartDateCalculateOnClick()) {
                TimeLore.startLifetime(item, config);
            }
        }
        if (scan.transformKey != null) {
            PdcCompat.writeTransform(item, scan.transformKey);
        }
        String pretty = TimeLore.formatDuration(seconds, config);
        player.sendMessage(config.getPrefix() + config.getTimeSet().replace("{time}", pretty));
        CommandSupport.audit(plugin, player, AuditRecord.SET, player.getName(), item, pretty);
    }

    /** Drops one argument (the playtime/held selector) by shifting the rest left. */
    private static String[] shiftFirst(String[] args) {
        String[] shifted = new String[args.length - 1];
        shifted[0] = args[0];
        System.arraycopy(args, 2, shifted, 1, args.length - 2);
        return shifted;
    }
}
