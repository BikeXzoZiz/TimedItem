package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.Transforms;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.DurationParser;
import me.albert.timeditem.PdcCompat;
import me.albert.timeditem.storage.AuditRecord;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared command plumbing: sender/item guards, the audit write and the
 * "transform:<key>" argument scanning used by several subcommands.
 */
final class CommandSupport {

    private CommandSupport() {
    }

    static Player requirePlayer(TimedItemPlugin plugin, CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        PluginConfig config = plugin.pluginConfig();
        sender.sendMessage(config.getPrefix() + config.getPlayerOnly());
        return null;
    }

    static ItemStack mainHand(TimedItemPlugin plugin, Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            PluginConfig config = plugin.pluginConfig();
            player.sendMessage(config.getPrefix() + config.getNoItem());
            return null;
        }
        return item;
    }

    /** Joins all remaining args and parses them, so both "1d12h" and the legacy "120 s" work. */
    static long parseDuration(TimedItemPlugin plugin, String[] args) {
        StringBuilder input = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            input.append(args[i]);
        }
        return DurationParser.parse(input.toString(), plugin.pluginConfig().unitSeconds());
    }

    /** Validates a transform:<key> token; messages and returns false when unknown. */
    static boolean checkTransform(TimedItemPlugin plugin, String key, CommandSender sender) {
        if (key == null) {
            return true;
        }
        PluginConfig config = plugin.pluginConfig();
        if (Transforms.find(config.transformProfiles(), key) == null) {
            sender.sendMessage(config.getPrefix()
                    + config.getTransformUnknown().replace("{id}", key));
            return false;
        }
        return true;
    }

    /** Parsed arguments of one command: "transform:<key>" tokens pulled out. */
    static final class ArgScan {
        final String[] args;
        final String transformKey;

        ArgScan(String[] args, String transformKey) {
            this.args = args;
            this.transformKey = transformKey;
        }
    }

    /** Pulls every "transform:<key>" token out of the arguments. */
    static ArgScan scanArgs(String[] args) {
        List<String> kept = new ArrayList<String>();
        String key = null;
        for (String arg : args) {
            if (arg.length() > 10 && arg.toLowerCase(Locale.ROOT).startsWith("transform:")) {
                key = arg.substring(10);
            } else {
                kept.add(arg);
            }
        }
        return new ArgScan(kept.toArray(new String[0]), key);
    }

    /**
     * Recognizes "playtime:&lt;time&gt;" / "held:&lt;time&gt;" at args[index]
     * (2.9+): returns "playtime"/"held" and rewrites the token in place to
     * the bare duration, or null when the token is something else.
     */
    static String extractCountdownPrefix(String[] args, int index) {
        if (args.length <= index) {
            return null;
        }
        String token = args[index];
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.startsWith("playtime:")) {
            args[index] = token.substring("playtime:".length());
            return "playtime";
        }
        if (lower.startsWith("held:")) {
            args[index] = token.substring("held:".length());
            return "held";
        }
        return null;
    }

    /** Applies the give duration in the requested timer kind. */
    static void applyGiveTimer(ItemStack item, PluginConfig config, String countdownType,
                               long seconds) {
        if ("playtime".equals(countdownType)) {
            TimeLore.applyPlaytime(item, config, seconds);
        } else if ("held".equals(countdownType)) {
            TimeLore.applyHeld(item, config, seconds);
        } else {
            TimeLore.applyExpiry(item, config, seconds);
        }
    }

    /**
     * Records one audit entry through the storage writer (async, never
     * blocks the command or a region thread).
     */
    static void audit(TimedItemPlugin plugin, CommandSender actor, String action, String target,
                      ItemStack item, String details) {
        PluginConfig config = plugin.pluginConfig();
        String actorName = actor instanceof Player ? ((Player) actor).getName() : "console";
        String actorUuid = actor instanceof Player
                ? ((Player) actor).getUniqueId().toString() : null;
        String itemUid = item == null ? null : PdcCompat.readUid(item);
        String itemId;
        if (item == null) {
            itemId = null;
        } else {
            String custom = plugin.hooks().itemId(item);
            itemId = custom != null ? custom : item.getType().name();
        }
        plugin.storage().appendAudit(new AuditRecord(System.currentTimeMillis(), actorUuid,
                actorName, action, target, itemUid, itemId, details));
    }
}
