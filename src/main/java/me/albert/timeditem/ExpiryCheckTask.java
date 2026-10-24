package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Periodically scans every online player's inventory for expired items,
 * so items are removed even if the player never clicks, uses, or takes
 * damage while holding them.
 */
public final class ExpiryCheckTask implements Runnable {

    private final TimedItemPlugin plugin;
    private final ExpiryListener listener;

    public ExpiryCheckTask(TimedItemPlugin plugin, ExpiryListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }
            listener.scanInventory(player, false);
        }
    }
}
