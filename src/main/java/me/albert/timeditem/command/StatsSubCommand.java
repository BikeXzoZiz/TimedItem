package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.SchedulerCompat;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** /ti stats [days] - daily expiry statistics from the storage backend (async query). */
public final class StatsSubCommand implements SubCommand {

    /** Cap for /ti stats materials listed. */
    private static final int STATS_TOP = 10;

    private final TimedItemPlugin plugin;

    public StatsSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        PluginConfig config = plugin.pluginConfig();
        int days = 7;
        if (args.length >= 2) {
            try {
                days = Math.max(1, Math.min(90, Integer.parseInt(args[1])));
            } catch (NumberFormatException e) {
                sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
                return;
            }
        }
        final int window = days;
        final String sinceDay = new SimpleDateFormat("yyyy-MM-dd").format(
                new Date(System.currentTimeMillis() - (days - 1L) * 86400000L));
        SchedulerCompat.runAsync(plugin, () -> {
            Map<String, Map<String, Long>> perDay = plugin.storage().expiredStats(sinceDay);
            Map<String, Long> totals = new HashMap<String, Long>();
            long total = 0;
            for (Map<String, Long> materials : perDay.values()) {
                for (Map.Entry<String, Long> entry : materials.entrySet()) {
                    Long existing = totals.get(entry.getKey());
                    long value = entry.getValue() == null ? 0L : entry.getValue();
                    totals.put(entry.getKey(), (existing == null ? 0L : existing) + value);
                    total += value;
                }
            }
            if (totals.isEmpty()) {
                sender.sendMessage(config.getPrefix()
                        + config.getStatsEmpty().replace("{days}", String.valueOf(window)));
                return;
            }
            sender.sendMessage(config.getPrefix() + config.getStatsHeader()
                    .replace("{days}", String.valueOf(window)));
            List<Map.Entry<String, Long>> top = new ArrayList<Map.Entry<String, Long>>(totals.entrySet());
            Collections.sort(top, new Comparator<Map.Entry<String, Long>>() {
                @Override
                public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                    return Long.compare(b.getValue(), a.getValue());
                }
            });
            for (int i = 0; i < top.size() && i < STATS_TOP; i++) {
                sender.sendMessage(config.getPrefix() + config.getStatsLine()
                        .replace("{material}", top.get(i).getKey())
                        .replace("{count}", String.valueOf(top.get(i).getValue())));
            }
            sender.sendMessage(config.getPrefix() + config.getStatsTotal()
                    .replace("{count}", String.valueOf(total)));
        });
    }
}
