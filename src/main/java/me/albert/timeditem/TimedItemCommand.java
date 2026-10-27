package me.albert.timeditem;

import me.albert.timeditem.command.AddSubCommand;
import me.albert.timeditem.command.AdminSubCommand;
import me.albert.timeditem.command.AuditSubCommand;
import me.albert.timeditem.command.CheckSubCommand;
import me.albert.timeditem.command.DebugSubCommand;
import me.albert.timeditem.command.FindSubCommand;
import me.albert.timeditem.command.GiveSubCommand;
import me.albert.timeditem.command.InfoSubCommand;
import me.albert.timeditem.command.MenuSubCommand;
import me.albert.timeditem.command.PauseSubCommand;
import me.albert.timeditem.command.ReloadSubCommand;
import me.albert.timeditem.command.RemoveSubCommand;
import me.albert.timeditem.command.RestoreSubCommand;
import me.albert.timeditem.command.ResumeSubCommand;
import me.albert.timeditem.command.SetSubCommand;
import me.albert.timeditem.command.StatsSubCommand;
import me.albert.timeditem.command.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * /ti entry point: dispatches to one {@link SubCommand} handler per
 * subcommand, printing the usage message for unknown or missing ones.
 */
public final class TimedItemCommand implements CommandExecutor {

    private final TimedItemPlugin plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<String, SubCommand>();

    public TimedItemCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
        register("reload", new ReloadSubCommand(plugin));
        register("set", new SetSubCommand(plugin));
        register("add", new AddSubCommand(plugin));
        register("pause", new PauseSubCommand(plugin));
        register("resume", new ResumeSubCommand(plugin));
        register("give", new GiveSubCommand(plugin));
        register("menu", new MenuSubCommand(plugin));
        register("remove", new RemoveSubCommand(plugin));
        register("restore", new RestoreSubCommand(plugin));
        register("info", new InfoSubCommand(plugin));
        register("debug", new DebugSubCommand(plugin));
        register("find", new FindSubCommand(plugin));
        register("stats", new StatsSubCommand(plugin));
        register("audit", new AuditSubCommand(plugin));
        register("admin", new AdminSubCommand(plugin));
        register("check", new CheckSubCommand(plugin));
    }

    private void register(String name, SubCommand sub) {
        subCommands.put(name, sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            SubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
            if (sub != null) {
                sub.handle(sender, args);
                return true;
            }
        }
        PluginConfig config = plugin.pluginConfig();
        sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
        return true;
    }
}
