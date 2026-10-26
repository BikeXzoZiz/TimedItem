package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Shows the remaining time of the held timed item in the actionbar,
 * refreshed once per second. Covers every timer type: counting
 * down (EXPIRE), frozen remaining time (PAUSED), the not-yet-started
 * lifetime total (LIFETIME) and the seconds-based countdowns
 * PLAYTIME / HELD (2.9+).
 */
public final class ActionbarTask implements Runnable {

    private final TimedItemPlugin plugin;

    public ActionbarTask(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (ActionbarCompat.isBroken()) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        if (!config.isActionbarEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (SchedulerCompat.isFolia()) {
                // Sending the actionbar must happen on the owning region.
                SchedulerCompat.runAtPlayer(plugin, player, () -> send(player, config));
            } else if (config.isIgnoreCreative() && player.getGameMode() == GameMode.CREATIVE) {
                continue;
            } else {
                send(player, config);
            }
        }
    }

    private void send(Player player, PluginConfig config) {
        if (!player.isOnline()
                || (config.isIgnoreCreative() && player.getGameMode() == GameMode.CREATIVE)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null) {
            return;
        }
        long remaining;
        if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
        } else {
            remaining = info.getSeconds();
        }
        if (remaining < 0) {
            return;
        }
        if (config.isLiveLoreEnabled()) {
            TimeLore.updateLiveDisplay(item, config, info, remaining);
        }
        String text = PluginConfig.color(config.getActionbarFormat()
                .replace("{time}", TimeLore.formatDuration(remaining, config)));
        ActionbarCompat.send(player, text);
    }
}
