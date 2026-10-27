package me.albert.timeditem.command;

import org.bukkit.command.CommandSender;

/**
 * One /ti subcommand. Handlers are registered by name in
 * {@link TimedItemCommand} and receive the raw args (args[0] is the
 * subcommand itself). Each handler reads the config fresh via
 * plugin.pluginConfig(), so a /ti reload swaps behaviour mid-flight.
 */
public interface SubCommand {

    void handle(CommandSender sender, String[] args);
}
