package me.albert.timeditem.command;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ti admin [page|filter] [filter] - opens the admin overview of all
 * timed items seen by the last periodic scan.
 */
public final class AdminSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public AdminSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        Player player = CommandSupport.requirePlayer(plugin, sender);
        if (player == null) {
            return;
        }
        int page = 0;
        String filter = "";
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                filter = args[1];
            }
        }
        if (args.length >= 3) {
            filter = args[2];
        }
        plugin.adminMenu().open(player, page, filter);
    }
}
