package me.albert.timeditem;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Grace period (2.8+): in the final seconds the item can no longer
 * be used, but it still only disappears when the timer hits zero.
 */
final class GraceHandler {

    private final TimedItemPlugin plugin;
    private final NoticeCooldown notices;

    GraceHandler(TimedItemPlugin plugin, NoticeCooldown notices) {
        this.plugin = plugin;
        this.notices = notices;
    }

    /** Blocks the interaction inside the grace window; true = blocked. */
    boolean handle(PlayerInteractEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.isGraceEnabled()) {
            return false;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return false;
        }
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null || info.getType() != TimeLore.Type.EXPIRE || info.getExpiry() == null) {
            return false;
        }
        long remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
        if (remaining <= 0 || remaining > config.getGraceSeconds()) {
            return false;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (notices.tryAcquire(player.getUniqueId())) {
            player.sendMessage(config.getPrefix() + config.getGraceMessage()
                    .replace("{time}", TimeLore.formatDuration(remaining, config)));
            player.playSound(player.getLocation(), config.getWarningSound(),
                    config.getWarningVolume(), config.getWarningPitch());
        }
        return true;
    }
}
