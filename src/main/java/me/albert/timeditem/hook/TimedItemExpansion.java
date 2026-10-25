package me.albert.timeditem.hook;

import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * PlaceholderAPI expansion for the held item. Only instantiated when
 * PlaceholderAPI is installed, so the class never loads (and the
 * me.clip classes are never touched) on servers without it.
 */
public final class TimedItemExpansion extends PlaceholderExpansion {

    private final TimedItemPlugin plugin;

    public TimedItemExpansion(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "timeditem";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BikeXzoZiz";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(@Nullable OfflinePlayer offline, @NotNull String params) {
        if (!(offline instanceof Player)) {
            return null;
        }
        Player player = (Player) offline;
        ItemStack item = player.getInventory().getItemInMainHand();
        PluginConfig config = plugin.pluginConfig();
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null) {
            return null;
        }
        if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            long remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
            switch (params) {
                case "remaining":
                case "seconds":
                    return String.valueOf(Math.max(0L, remaining));
                case "time":
                    return remaining > 0 ? TimeLore.formatDuration(remaining, config) : "0" + config.getUnitS();
                case "type":
                    return "expire";
                case "date":
                    return TimeLore.LORE_FORMAT.format(info.getExpiry());
                default:
                    return null;
            }
        }
        switch (params) {
            case "remaining":
            case "time":
                return info.getSeconds() >= 0 ? TimeLore.formatDuration(Math.max(0L, info.getSeconds()), config) : null;
            case "seconds":
                return info.getSeconds() >= 0 ? String.valueOf(info.getSeconds()) : null;
            case "type":
                return info.getType() == TimeLore.Type.PAUSED ? "paused" : "lifetime";
            case "date":
                return null;
            default:
                return null;
        }
    }
}
