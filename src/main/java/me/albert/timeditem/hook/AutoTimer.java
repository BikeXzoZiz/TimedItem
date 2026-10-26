package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Shared logic for the distribution hooks: stamps the configured
 * expiry on an item handed out by a shop or crate, unless it already
 * carries a TimedItem timer or its material is blacklisted.
 */
final class AutoTimer {

    private AutoTimer() {
    }

    static void apply(ItemStack item, TimedItemPlugin plugin, long seconds) {
        if (item == null || seconds <= 0 || item.getType() == Material.AIR) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        if (TimeLore.isBlacklisted(item, config)) {
            return;
        }
        if (TimeLore.analyze(item, config) != null) {
            return; // already timed - never stack a second timer
        }
        TimeLore.applyExpiry(item, config, seconds);
        DebugLog.verbose("Auto-timer applied: " + TimeLore.formatDuration(seconds, config)
                + " on " + item.getType());
    }
}
