package me.albert.timeditem.command;

import me.albert.timeditem.DebugLog;
import me.albert.timeditem.DebugReport;
import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.PdcCompat;
import me.albert.timeditem.SchedulerCompat;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * /ti debug [on|off|export] - without arguments prints the current
 * debug state. on/off is a session switch that resets to
 * Settings.debug on reload or restart; export writes a bug report
 * file to plugins/TimedItem/debug/.
 */
public final class DebugSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public DebugSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        PluginConfig config = plugin.pluginConfig();
        if (args.length >= 2) {
            String option = args[1].toLowerCase(Locale.ROOT);
            if (option.equals("on")) {
                DebugLog.setEnabled(true);
                sender.sendMessage(config.getPrefix() + config.getDebugOn());
                return;
            }
            if (option.equals("off")) {
                DebugLog.setEnabled(false);
                sender.sendMessage(config.getPrefix() + config.getDebugOff());
                return;
            }
            if (option.equals("export")) {
                exportDebugReport(sender, config);
                return;
            }
            sender.sendMessage(config.getPrefix() + config.getDebugUsage());
            return;
        }
        sender.sendMessage(config.getPrefix() + config.getDebugStatus()
                .replace("{state}", DebugLog.isEnabled() ? "on" : "off")
                .replace("{entries}", String.valueOf(DebugLog.size()))
                .replace("{recovery}", String.valueOf(plugin.recoveryStore().size()))
                .replace("{storage}", PdcCompat.isAvailable() ? "PDC" : "lore"));
        sender.sendMessage(config.getPrefix() + config.getDebugUsage());
    }

    /** Snapshots the state here (main/region thread), writes the file async. */
    private void exportDebugReport(CommandSender sender, PluginConfig config) {
        final DebugReport.Snapshot snapshot = DebugReport.snapshot(plugin);
        final List<String> logLines = DebugLog.snapshot();
        final File file = new File(new File(plugin.getDataFolder(), "debug"),
                DebugReport.fileName());
        SchedulerCompat.runAsync(plugin, () -> {
            try {
                Files.createDirectories(file.getParentFile().toPath());
                Files.write(file.toPath(),
                        DebugReport.render(snapshot, logLines).getBytes(StandardCharsets.UTF_8));
                sender.sendMessage(config.getPrefix() + config.getDebugExported()
                        .replace("{file}", file.getPath()));
            } catch (Exception e) {
                plugin.getLogger().warning("Could not write debug report: " + e);
                sender.sendMessage(config.getPrefix() + config.getDebugExportFailed()
                        .replace("{error}", String.valueOf(e.getMessage())));
            }
        });
    }
}
