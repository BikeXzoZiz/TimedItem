package me.albert.timeditem.command;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ti menu - opens the player GUI (presets, pause/resume, remove). */
public final class MenuSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public MenuSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        Player player = CommandSupport.requirePlayer(plugin, sender);
        if (player != null) {
            plugin.menu().open(player);
        }
    }
}
