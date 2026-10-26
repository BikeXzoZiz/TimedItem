package me.albert.timeditem;

import me.albert.timeditem.storage.AuditRecord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class TimedItemCommand implements CommandExecutor {

    /** Audit entries shown per page of /ti audit. */
    private static final int AUDIT_PAGE_SIZE = 10;
    /** Cap for /ti stats materials listed. */
    private static final int STATS_TOP = 10;

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
                    audit(player, AuditRecord.REMOVE, player.getName(), item, null, config);
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
        if (sub.equals("find")) {
            handleFind(sender, args, config);
            return true;
        }
        if (sub.equals("stats")) {
            handleStats(sender, args, config);
            return true;
        }
        if (sub.equals("audit")) {
            handleAudit(sender, args, config);
            return true;
        }
        if (sub.equals("admin")) {
            Player player = requirePlayer(sender, config);
            if (player != null) {
                int page = 0;
                String filter = "";
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]) - 1;
                    } catch (NumberFormatException e) {
                        filter = args[1];
                    }
                }
                if (args.length >= 3) {
                    filter = args[2];
                }
                plugin.adminMenu().open(player, page, filter);
            }
            return true;
        }
        if (sub.equals("check")) {
            handleCheck(sender, args, config);
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
        ArgScan scan = scanArgs(args);
        // /ti set playtime <time> / /ti set held <time> (2.9+)
        String countdownType = null;
        String[] durationArgs = scan.args;
        if (scan.args.length >= 2) {
            String lower = scan.args[1].toLowerCase(Locale.ROOT);
            if (lower.equals("playtime") || lower.equals("held")) {
                countdownType = lower;
                durationArgs = shiftFirst(scan.args);
            }
        }
        if (durationArgs.length < 2) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        long seconds = parseDuration(durationArgs, config);
        if (seconds <= 0) {
            player.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        if (!checkTransform(scan.transformKey, player, config)) {
            return;
        }
        if (TimeLore.isBlacklisted(item, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        if ("playtime".equals(countdownType)) {
            TimeLore.applyPlaytime(item, config, seconds);
        } else if ("held".equals(countdownType)) {
            TimeLore.applyHeld(item, config, seconds);
        } else {
            TimeLore.applyLifetime(item, config, seconds);
            if (!config.isStartDateCalculateOnClick()) {
                TimeLore.startLifetime(item, config);
            }
        }
        if (scan.transformKey != null) {
            PdcCompat.writeTransform(item, scan.transformKey);
        }
        String pretty = TimeLore.formatDuration(seconds, config);
        player.sendMessage(config.getPrefix() + config.getTimeSet().replace("{time}", pretty));
        audit(player, AuditRecord.SET, player.getName(), item, pretty, config);
    }

    /** Drops one argument (the playtime/held selector) by shifting the rest left. */
    private static String[] shiftFirst(String[] args) {
        String[] shifted = new String[args.length - 1];
        shifted[0] = args[0];
        System.arraycopy(args, 2, shifted, 1, args.length - 2);
        return shifted;
    }

    /**
     * Recognizes "playtime:&lt;time&gt;" / "held:&lt;time&gt;" at args[index]
     * (2.9+): returns "playtime"/"held" and rewrites the token in place to
     * the bare duration, or null when the token is something else.
     */
    private static String extractCountdownPrefix(String[] args, int index) {
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
    private static void applyGiveTimer(ItemStack item, PluginConfig config, String countdownType,
                                       long seconds) {
        if ("playtime".equals(countdownType)) {
            TimeLore.applyPlaytime(item, config, seconds);
        } else if ("held".equals(countdownType)) {
            TimeLore.applyHeld(item, config, seconds);
        } else {
            TimeLore.applyExpiry(item, config, seconds);
        }
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
            audit(player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config), config);
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
            audit(player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config), config);
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
            audit(player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config), config);
        } else if (type == TimeLore.Type.PLAYTIME || type == TimeLore.Type.HELD) {
            long seconds = type == TimeLore.Type.PLAYTIME
                    ? TimeLore.playtimeSeconds(item, config)
                    : TimeLore.heldSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            long newSeconds = seconds + delta;
            if (type == TimeLore.Type.PLAYTIME) {
                TimeLore.applyPlaytime(item, config, newSeconds);
            } else {
                TimeLore.applyHeld(item, config, newSeconds);
            }
            player.sendMessage(config.getPrefix() + config.getTimeAdded()
                    .replace("{time}", TimeLore.formatDuration(newSeconds, config)));
            audit(player, AuditRecord.ADD, player.getName(), item,
                    "+" + TimeLore.formatDuration(delta, config), config);
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
     *
     * Both flows accept a "transform:<key>" token anywhere after the
     * subcommand to tag the gift with an expiry transformation profile.
     */
    private void handleGive(CommandSender sender, String[] args, PluginConfig config) {
        ArgScan scan = scanArgs(args);
        args = scan.args;
        // "playtime:<time>" / "held:<time>" as the duration token (2.9+)
        String countdownType = extractCountdownPrefix(args, 2);
        if (args.length >= 3 && args[2].indexOf(':') >= 0) {
            handleGiveCustom(sender, args, config, scan.transformKey);
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
        if (!checkTransform(scan.transformKey, player, config)) {
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
        applyGiveTimer(gift, config, countdownType, seconds);
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
        audit(player, AuditRecord.GIVE, target.getName(), gift, pretty, config);
    }

    /**
     * /ti give <player> <plugin>:<id> <time> [amount] - hands the target
     * a timed copy of a custom item built by an integrated plugin.
     */
    private void handleGiveCustom(CommandSender sender, String[] args, PluginConfig config,
                                  String transformKey) {
        if (args.length < 4) {
            sender.sendMessage(config.getPrefix() + config.getInvalidUsage());
            return;
        }
        String countdownType = extractCountdownPrefix(args, 3);
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
        if (!checkTransform(transformKey, sender, config)) {
            return;
        }
        amount = Math.min(amount, built.getMaxStackSize());
        built.setAmount(amount);
        applyGiveTimer(built, config, countdownType, seconds);
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
        audit(sender, AuditRecord.GIVE, target.getName(), built, token + " " + pretty, config);
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
        audit(player, AuditRecord.PAUSE, player.getName(), item,
                TimeLore.formatDuration(remaining, config), config);
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
        audit(player, AuditRecord.RESUME, player.getName(), item,
                TimeLore.formatDuration(seconds, config), config);
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
        } else if (type == TimeLore.Type.PLAYTIME || type == TimeLore.Type.HELD) {
            long seconds = type == TimeLore.Type.PLAYTIME
                    ? TimeLore.playtimeSeconds(item, config)
                    : TimeLore.heldSeconds(item, config);
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            message = config.getInfoRemaining().replace("{time}", TimeLore.formatDuration(seconds, config));
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

    /** Parsed arguments of one command: "transform:<key>" tokens pulled out. */
    private static final class ArgScan {
        final String[] args;
        final String transformKey;

        ArgScan(String[] args, String transformKey) {
            this.args = args;
            this.transformKey = transformKey;
        }
    }

    /** Pulls every "transform:<key>" token out of the arguments. */
    private static ArgScan scanArgs(String[] args) {
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

    /** Validates a transform:<key> token; messages and returns false when unknown. */
    private boolean checkTransform(String key, CommandSender sender, PluginConfig config) {
        if (key == null) {
            return true;
        }
        if (Transforms.find(config.transformProfiles(), key) == null) {
            sender.sendMessage(config.getPrefix()
                    + config.getTransformUnknown().replace("{id}", key));
            return false;
        }
        return true;
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

    /**
     * Records one audit entry through the storage writer (async, never
     * blocks the command or a region thread).
     */
    private void audit(CommandSender actor, String action, String target, ItemStack item,
                       String details, PluginConfig config) {
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

    /**
     * /ti find <material|plugin:id|name> - locates timed items in online
     * player inventories; on classic servers also on the ground, in item
     * frames, on armor stands and in every loaded container. On Folia
     * only the per-player search is possible (cross-region reads are not
     * safe), so there it dispatches onto each player's region and
     * collects the results on an async thread.
     */
    private void handleFind(CommandSender sender, String[] args, PluginConfig config) {
        if (args.length < 2) {
            sender.sendMessage(config.getPrefix() + config.getFindUsage());
            return;
        }
        final Predicate<ItemStack> matcher = ItemFinder.compile(plugin, args[1]);
        if (matcher == null) {
            sender.sendMessage(config.getPrefix() + config.getFindUsage());
            return;
        }
        final String query = args[1];
        if (SchedulerCompat.isFolia()) {
            final List<Player> online = new ArrayList<Player>(Bukkit.getOnlinePlayers());
            final List<String> hits = Collections.synchronizedList(new ArrayList<String>());
            final CountDownLatch done = new CountDownLatch(online.size());
            for (final Player player : online) {
                SchedulerCompat.runAtPlayer(plugin, player, () -> {
                    try {
                        if (player.isOnline()) {
                            hits.addAll(ItemFinder.findInPlayer(plugin, player, matcher));
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            SchedulerCompat.runAsync(plugin, () -> {
                try {
                    done.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sendFindResult(sender, config, query, hits);
            });
            return;
        }
        List<String> hits = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            hits.addAll(ItemFinder.findInPlayer(plugin, player, matcher));
        }
        if (hits.size() < ItemFinder.MAX_HITS) {
            hits.addAll(ItemFinder.findInWorlds(plugin, matcher));
        }
        sendFindResult(sender, config, query, hits);
    }

    private void sendFindResult(CommandSender sender, PluginConfig config, String query,
                                List<String> hits) {
        if (hits.isEmpty()) {
            sender.sendMessage(config.getPrefix()
                    + config.getFindNoMatch().replace("{query}", query));
            return;
        }
        sender.sendMessage(config.getPrefix() + config.getFindResult()
                .replace("{count}", String.valueOf(hits.size()))
                .replace("{query}", query));
        int shown = Math.min(hits.size(), ItemFinder.MAX_HITS);
        for (int i = 0; i < shown; i++) {
            sender.sendMessage(hits.get(i));
        }
        if (hits.size() > shown) {
            sender.sendMessage(config.getPrefix() + config.getFindTruncated()
                    .replace("{rest}", String.valueOf(hits.size() - shown))
                    .replace("{shown}", String.valueOf(shown)));
        }
    }

    /** /ti stats [days] - daily expiry statistics from the storage backend (async query). */
    private void handleStats(CommandSender sender, String[] args, PluginConfig config) {
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

    /**
     * /ti check <player> - lists every timed item the target carries
     * (slot, item, timer kind, remaining). Online players only: reading
     * an offline player's inventory is deliberately not supported.
     */
    private void handleCheck(CommandSender sender, String[] args, PluginConfig config) {
        if (args.length < 2) {
            sender.sendMessage(config.getPrefix() + config.getCheckUsage());
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(config.getPrefix()
                    + config.getCheckOffline().replace("{player}", args[1]));
            return;
        }
        sender.sendMessage(config.getPrefix() + config.getCheckHeader()
                .replace("{player}", target.getName()));
        PluginConfig cfg = config;
        ItemStack[] contents = target.getInventory().getContents();
        int found = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            TimeLore.TimerInfo info = TimeLore.analyze(item, cfg);
            if (info == null) {
                continue;
            }
            found++;
            long remaining;
            if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
                remaining = Math.max(0L, Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds());
            } else {
                remaining = Math.max(0L, info.getSeconds());
            }
            String custom = plugin.hooks() == null ? null : plugin.hooks().itemId(item);
            String itemDesc = custom == null ? item.getType().name() : custom;
            sender.sendMessage(config.getPrefix() + config.getCheckLine()
                    .replace("{slot}", String.valueOf(slot))
                    .replace("{item}", itemDesc)
                    .replace("{type}", info.getType().name().toLowerCase(Locale.ROOT))
                    .replace("{time}", TimeLore.formatDuration(remaining, cfg)));
        }
        if (found == 0) {
            sender.sendMessage(config.getPrefix()
                    + config.getCheckEmpty().replace("{player}", target.getName()));
        }
    }

    /** /ti audit [page] - recent timer actions from the audit log (async query). */
    private void handleAudit(CommandSender sender, String[] args, PluginConfig config) {        int page = 0;
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
