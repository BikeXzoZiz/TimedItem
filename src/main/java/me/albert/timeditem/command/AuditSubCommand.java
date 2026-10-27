package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.SchedulerCompat;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.storage.AuditRecord;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** /ti audit [page] - recent timer actions from the audit log (async query). */
public final class AuditSubCommand implements SubCommand {

    /** Audit entries shown per page of /ti audit. */
    private static final int AUDIT_PAGE_SIZE = 10;

    private final TimedItemPlugin plugin;

    public AuditSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        PluginConfig config = plugin.pluginConfig();
        int page = 0;
        if (args.length >= 2) {
            try {
                page = Math.max(0, Integer.parseInt(args[1]) - 1);
            } catch (NumberFormatException e) {
                sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
                return;
            }
        }
        final int pageIndex = page;
        SchedulerCompat.runAsync(plugin, () -> {
            List<AuditRecord> entries = plugin.storage().auditPage(AUDIT_PAGE_SIZE, pageIndex);
            if (entries.isEmpty()) {
                sender.sendMessage(config.getPrefix() + config.getAuditEmpty());
                return;
            }
            sender.sendMessage(config.getPrefix() + config.getAuditHeader()
                    .replace("{page}", String.valueOf(pageIndex + 1)));
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (AuditRecord entry : entries) {
                sender.sendMessage(config.getPrefix() + config.getAuditLine()
                        .replace("{date}", format.format(new Date(entry.getTimeMillis())))
                        .replace("{actor}", entry.getActorName() == null ? "?" : entry.getActorName())
                        .replace("{action}", entry.getAction() == null ? "?" : entry.getAction())
                        .replace("{target}", entry.getTarget() == null ? "-" : entry.getTarget())
                        .replace("{item}", entry.getItemId() == null ? "-" : entry.getItemId())
                        .replace("{details}", entry.getDetails() == null ? "" : entry.getDetails()));
            }
        });
    }
}
