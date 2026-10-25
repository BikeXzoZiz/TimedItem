package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Tab completion for /ti: subcommands, time examples, online players, restore pages. */
public final class TimedItemTabCompleter implements TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "set", "add", "give", "pause", "resume", "remove", "info", "menu", "restore", "reload");
    private static final List<String> TIME_HINTS =
            Arrays.asList("60s", "5m", "30m", "1h", "6h", "1d", "1d12h", "7d", "30d");
    private static final List<String> AMOUNT_HINTS = Arrays.asList("1", "16", "32", "64");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            filter(SUBS, args[0], out);
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("set") || sub.equals("add")) {
            if (args.length == 2) {
                filter(TIME_HINTS, args[1], out);
            }
        } else if (sub.equals("give")) {
            if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        out.add(player.getName());
                    }
                }
            } else if (args.length == 3) {
                filter(TIME_HINTS, args[2], out);
            } else if (args.length == 4) {
                filter(AMOUNT_HINTS, args[3], out);
            }
        } else if (sub.equals("restore")) {
            if (args.length == 2) {
                filter(Arrays.asList("1", "2", "3"), args[1], out);
            }
        }
        return out;
    }

    private void filter(List<String> options, String prefix, List<String> out) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
    }
}
