package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.RecoveryStore;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /ti restore [page|player] - opens the restore GUI. Local mode lists
 * the in-memory recovery log; network mode (3.1+) reads the shared
 * database across all servers of the network and can target any
 * player by name (admin command only).
 */
public final class RestoreSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public RestoreSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        Player player = CommandSupport.requirePlayer(plugin, sender);
        if (player == null) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        // network mode (3.1+): read one player's entries from the
        // shared database across all servers of the network
        if (plugin.isNetworkRestoreActive()) {
            UUID target = player.getUniqueId();
            String targetName = player.getName();
            int page = 0;
            if (args.length >= 2) {
                if (args[1].matches("\\d+")) {
                    try {
                        page = Math.max(0, Integer.parseInt(args[1]) - 1);
                    } catch (NumberFormatException ignored) {
                        page = 0;
                    }
                } else {
                    Player online = Bukkit.getPlayerExact(args[1]);
                    if (online != null) {
                        target = online.getUniqueId();
                        targetName = online.getName();
                    } else {
                        // may resolve a name via the usercache; admin command only
                        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                        target = offline.getUniqueId();
                        targetName = args[1];
                    }
                }
            }
            plugin.restoreMenu().openNetwork(player, target, targetName, page);
            return;
        }
        RecoveryStore store = plugin.recoveryStore();
        if (store.size() == 0) {
            player.sendMessage(config.getPrefix() + config.getRestoreEmpty());
        } else {
            int page = 0;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException ignored) {
                    page = 0;
                }
            }
            plugin.restoreMenu().open(player, page);
        }
    }
}
