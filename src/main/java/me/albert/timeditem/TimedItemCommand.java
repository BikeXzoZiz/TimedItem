package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TimedItemCommand implements CommandExecutor {

    private final TimedItemPlugin plugin;

    public TimedItemCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PluginConfig config = plugin.pluginConfig();
        if (args.length == 0) {
            sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            config.load();
            // the session switch follows the config on every reload
            DebugLog.setEnabled(config.isDebugEnabled());
            plugin.recoveryStore().load();
            plugin.hooks().applyConfig();
            plugin.startTasks();
            sender.sendMessage(config.getPrefix() + config.getConfigReload());
            return true;
        }
        if (sub.equals("set")) {
            handleSet(sender, args, config);
            return true;
        }
        if (sub.equals("add")) {
            handleAdd(sender, args, config);
            return true;
        }
        if (sub.equals("pause")) {
            Player player = requirePlayer(sender, config);
            if (player != null) {
                ItemStack item = mainHand(player, config);
                if (item != null) {
                    handlePause(player, item, config);
                }
            }
            return true;
        }
        if (sub.equals("resume")) {
            Player player = requirePlayer(sender, config);
            if (player != null) {
                ItemStack item = mainHand(player, config);
                if (item != null) {
                    handleResume(player, item, config);
                }
            }
            return true;
        }
        if (sub.equals("give")) {
            handleGive(sender, args, config);
            return true;
        }
        if (sub.equals("menu")) {
            Player player = requirePlayer(sender, config);
            if (player != null) {
                plugin.menu().open(player);
            }
            return true;
        }
        if (sub.equals("remove")) {
            Player player = requirePlayer(sender, config);
            if (player != null) {
                ItemStack item = mainHand(player, config);
                if (item != null) {
                    TimeLore.removeTimer(item, config);
                    player.sendMessage(config.getPrefix() + config.getItemCleared());
                }
            }
            return true;
        }
        if (sub.equals("restore")) {
            Player player = requirePlayer(sender, config);
            if (player != null) {
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
            return true;
        }
        if (sub.equals("info")) {
            handleInfo(sender, config);
            return true;
        }
        if (sub.equals("debug")) {
            handleDebug(sender, args, config);
            return true;
        }
        sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
        return true;
    }

    /**
     * /ti debug [on|off|export] - without arguments prints the current
     * debug state. on/off is a session switch that resets to
     * Settings.debug on reload or restart; export writes a bug report
     * file to plugins/TimedItem/debug/.
     */
    private void handleDebug(CommandSender sender, String[] args, PluginConfig config) {
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

    private void handleSet(CommandSender sender, String[] args, PluginConfig config) {
        Player player = requirePlayer(sender, config);
        if (player == null) {
            return;
        }
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        long seconds = parseDuration(args, config);
        if (seconds <= 0) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        if (TimeLore.isBlacklisted(item, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        TimeLore.applyLifetime(item, config, seconds);
        if (!config.isStartDateCalculateOnClick()) {
            TimeLore.startLifetime(item, config);
        }
        String pretty = TimeLore.formatDuration(seconds, config);
        player.sendMessage(config.getPrefix() + config.getTimeSet().replace("{time}", pretty));
    }

    private void handleAdd(CommandSender sender, String[] args, PluginConfig config) {
        Player player = requirePlayer(sender, config);
        if (player == null) {
            return;
        }
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        long delta = parseDuration(args, config);
        if (delta <= 0) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        if (TimeLore.isBlacklisted(item, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type == TimeLore.Type.EXPIRE) {
            LocalDateTime expiry = TimeLore.expiryOf(item, config);
            if (expiry == null) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
            long newRemaining = Math.max(0, remaining) + delta;
            TimeLore.applyExpiry(item, config, newRemaining);
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newRemaining, config)));
        } else if (type == TimeLore.Type.LIFETIME) {
            long seconds = TimeLore.lifetimeSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long newSeconds = seconds + delta;
            TimeLore.applyLifetime(item, config, newSeconds);
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newSeconds, config)));
        } else if (type == TimeLore.Type.PAUSED) {
            long seconds = TimeLore.pausedSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long newSeconds = seconds + delta;
            TimeLore.applyPaused(item, config, newSeconds);
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newSeconds, config)));
        } else {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
        }
    }

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
     */
    private void handleGive(CommandSender sender, String[] args, PluginConfig config) {
        if (args.length >= 3 && args[2].indexOf(':') >= 0) {
            handleGiveCustom(sender, args, config);
            return;
        }
        Player player = requirePlayer(sender, config);
        if (player == null) {
            return;
        }
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
        ItemStack held = mainHand(player, config);
        if (held == null) {
            return;
        }
        if (TimeLore.isBlacklisted(held, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
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
        TimeLore.applyExpiry(gift, config, seconds);
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
    }

    /**
     * /ti give <player> <plugin>:<id> <time> [amount] - hands the target
     * a timed copy of a custom item built by an integrated plugin.
     */
    private void handleGiveCustom(CommandSender sender, String[] args, PluginConfig config) {
        if (args.length < 4) {
            sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
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
        amount = Math.min(amount, built.getMaxStackSize());
        built.setAmount(amount);
        TimeLore.applyExpiry(built, config, seconds);
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
    }

    private void handlePause(Player player, ItemStack item, PluginConfig config) {
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type == TimeLore.Type.PAUSED) {
            player.sendMessage(config.getPrefix() + config.getAlreadyPaused());
            return;
        }
        if (type != TimeLore.Type.EXPIRE) {
            player.sendMessage(config.getPrefix() + config.getTimerNotStarted());
            return;
        }
        LocalDateTime expiry = TimeLore.expiryOf(item, config);
        if (expiry == null) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
        if (remaining <= 0) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        TimeLore.applyPaused(item, config, remaining);
        player.sendMessage(config.getPrefix() + config.getTimerPaused()
                .replace("{time}", TimeLore.formatDuration(remaining, config)));
    }

    private void handleResume(Player player, ItemStack item, PluginConfig config) {
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type != TimeLore.Type.PAUSED) {
            if (type == TimeLore.Type.EXPIRE) {
                player.sendMessage(config.getPrefix() + config.getAlreadyRunning());
            } else {
                player.sendMessage(config.getPrefix() + config.getTimerNotStarted());
            }
            return;
        }
        long seconds = TimeLore.pausedSeconds(item, config);
        if (seconds <= 0) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        TimeLore.applyExpiry(item, config, seconds);
        player.sendMessage(config.getPrefix() + config.getTimerResumed()
                .replace("{time}", TimeLore.formatDuration(seconds, config)));
    }

    private void handleInfo(CommandSender sender, PluginConfig config) {
        Player player = requirePlayer(sender, config);
        if (player == null) {
            return;
        }
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        TimeLore.Type type = TimeLore.typeOf(item, config);
        if (type == null) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        String message;
        if (type == TimeLore.Type.EXPIRE) {
            LocalDateTime expiry = TimeLore.expiryOf(item, config);
            if (expiry == null) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
            String pretty = remaining > 0 ? TimeLore.formatDuration(remaining, config) : "0" + config.getUnitS();
            message = config.getInfoRemaining().replace("{time}",
                    pretty + " &8(" + TimeLore.LORE_FORMAT.format(expiry) + ")");
        } else if (type == TimeLore.Type.PAUSED) {
            long seconds = TimeLore.pausedSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            message = config.getTimerPaused().replace("{time}", TimeLore.formatDuration(seconds, config));
        } else {
            long seconds = TimeLore.lifetimeSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            message = config.getInfoLifetime().replace("{time}", TimeLore.formatDuration(seconds, config));
        }
        player.sendMessage(config.getPrefix() + PluginConfig.color(message));
    }

    /** Joins all remaining args and parses them, so both "1d12h" and the legacy "120 s" work. */
    private long parseDuration(String[] args, PluginConfig config) {
        StringBuilder input = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            input.append(args[i]);
        }
        return DurationParser.parse(input.toString(), config.unitSeconds());
    }

    private Player requirePlayer(CommandSender sender, PluginConfig config) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        sender.sendMessage(config.getPrefix() + config.getPlayerOnly());
        return null;
    }

    private ItemStack mainHand(Player player, PluginConfig config) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(config.getPrefix() + config.getNoItem());
            return null;
        }
        return item;
    }
}
