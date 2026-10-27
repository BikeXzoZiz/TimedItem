package me.albert.timeditem.command;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.storage.AuditRecord;
import me.albert.timeditem.DurationParser;
import me.albert.timeditem.PdcCompat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * /ti give <player> <time> [amount] - turns a copy of the held item
 * into a timed item expiring in <time> and hands it to the target.
 * The time is one token such as 1d12h (the legacy two-token form is
 * not supported here because the last argument is the amount).
 *
 * /ti give <player> <plugin>:<id> <time> [amount] - builds a custom
 * item from an integrated plugin (mmoitems:SWORD:CUTLASS,
 * itemsadder:namespace:id, cc:Classic:GKITS, ...) instead; the
 * sender needs no item in hand, so console works too.
 *
 * Both flows accept a "transform:<key>" token anywhere after the
 * subcommand to tag the gift with an expiry transformation profile.
 */
public final class GiveSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public GiveSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        CommandSupport.ArgScan scan = CommandSupport.scanArgs(args);
        args = scan.args;
        // "playtime:<time>" / "held:<time>" as the duration token (2.9+)
        String countdownType = CommandSupport.extractCountdownPrefix(args, 2);
        if (args.length >= 3 && args[2].indexOf(':') >= 0) {
            handleGiveCustom(sender, args, scan.transformKey);
            return;
        }
        Player player = CommandSupport.requirePlayer(plugin, sender);
        if (player == null) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        if (args.length < 3) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(config.getPrefix()
                    + config.getTargetNotFound().replace("{player}", args[1]));
            return;
        }
        ItemStack held = CommandSupport.mainHand(plugin, player);
        if (held == null) {
            return;
        }
        if (TimeLore.isBlacklisted(held, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        if (!CommandSupport.checkTransform(plugin, scan.transformKey, player)) {
            return;
        }
        long seconds = DurationParser.parse(args[2], config.unitSeconds());
        if (seconds <= 0) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(config.getPrefix() + config.getInvalidUsage());
                return;
            }
        }
        if (amount < 1 || amount > held.getAmount()) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        ItemStack gift = held.clone();
        gift.setAmount(amount);
        CommandSupport.applyGiveTimer(gift, config, countdownType, seconds);
        PdcCompat.stampOwner(gift, player);
        if (scan.transformKey != null) {
            PdcCompat.writeTransform(gift, scan.transformKey);
        }
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(gift);
        for (ItemStack rest : leftover.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), rest);
        }
        if (amount >= held.getAmount()) {
            player.getInventory().setItemInMainHand(null);
        } else {
            held.setAmount(held.getAmount() - amount);
            player.getInventory().setItemInMainHand(held);
        }
        String pretty = TimeLore.formatDuration(seconds, config);
        player.sendMessage(config.getPrefix() + config.getGiveSent()
                .replace("{time}", pretty).replace("{player}", target.getName()));
        if (!target.equals(player)) {
            target.sendMessage(config.getPrefix() + config.getGiveReceived()
                    .replace("{time}", pretty));
        }
        CommandSupport.audit(plugin, player, AuditRecord.GIVE, target.getName(), gift, pretty);
    }

    /**
     * /ti give <player> <plugin>:<id> <time> [amount] - hands the target
     * a timed copy of a custom item built by an integrated plugin.
     */
    private void handleGiveCustom(CommandSender sender, String[] args, String transformKey) {
        PluginConfig config = plugin.pluginConfig();
        if (args.length < 4) {
            sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        String countdownType = CommandSupport.extractCountdownPrefix(args, 3);
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(config.getPrefix()
                    + config.getTargetNotFound().replace("{player}", args[1]));
            return;
        }
        String token = args[2];
        ItemStack built = plugin.hooks().build(token);
        if (built == null) {
            sender.sendMessage(config.getPrefix()
                    + config.getItemNotFound().replace("{id}", token));
            return;
        }
        long seconds = DurationParser.parse(args[3], config.unitSeconds());
        if (seconds <= 0) {
            sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
                return;
            }
        }
        if (amount < 1) {
            sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        if (TimeLore.isBlacklisted(built, config)) {
            sender.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        if (!CommandSupport.checkTransform(plugin, transformKey, sender)) {
            return;
        }
        amount = Math.min(amount, built.getMaxStackSize());
        built.setAmount(amount);
        CommandSupport.applyGiveTimer(built, config, countdownType, seconds);
        PdcCompat.stampOwner(built, sender instanceof Player ? (Player) sender : null);
        if (transformKey != null) {
            PdcCompat.writeTransform(built, transformKey);
        }
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(built);
        for (ItemStack rest : leftover.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), rest);
        }
        String pretty = TimeLore.formatDuration(seconds, config);
        sender.sendMessage(config.getPrefix() + config.getGiveSent()
                .replace("{time}", pretty).replace("{player}", target.getName()));
        if (!target.equals(sender)) {
            target.sendMessage(config.getPrefix() + config.getGiveReceived()
                    .replace("{time}", pretty));
        }
        CommandSupport.audit(plugin, sender, AuditRecord.GIVE, target.getName(), built,
                token + " " + pretty);
    }
}
