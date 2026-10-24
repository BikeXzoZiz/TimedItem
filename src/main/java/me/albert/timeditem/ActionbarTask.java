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
 * refreshed once per second.
 */
public final class ActionbarTask implements Runnable {

    private final TimedItemPlugin plugin;
    private static boolean broken;

    public ActionbarTask(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (broken) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        if (!config.isActionbarEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (TimeLore.typeOf(item, config) != TimeLore.Type.EXPIRE) {
                continue;
            }
            LocalDateTime expiry = TimeLore.expiryOf(item, config);
            if (expiry == null) {
                continue;
            }
            long remaining = Duration.between(LocalDateTime.now(), expiry).getSeconds();
            if (remaining < 0) {
                remaining = 0;
            }
            String text = PluginConfig.color(config.getActionbarFormat()
                    .replace("{time}", TimeLore.formatDuration(remaining, config)));
            try {
                ActionbarCompat.send(player, text);
            } catch (Throwable t) {
                broken = true;
                return;
            }
        }
    }
}
