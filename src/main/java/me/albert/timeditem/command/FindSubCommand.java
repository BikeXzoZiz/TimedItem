package me.albert.timeditem.command;

import me.albert.timeditem.ItemFinder;
import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.SchedulerCompat;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * /ti find <material|plugin:id|name> - locates timed items in online
 * player inventories; on classic servers also on the ground, in item
 * frames, on armor stands and in every loaded container. On Folia
 * only the per-player search is possible (cross-region reads are not
 * safe), so there it dispatches onto each player's region and
 * collects the results on an async thread.
 */
public final class FindSubCommand implements SubCommand {

    private final TimedItemPlugin plugin;

    public FindSubCommand(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        PluginConfig config = plugin.pluginConfig();
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
}
