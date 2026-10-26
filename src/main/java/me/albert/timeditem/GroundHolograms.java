package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Floating countdown holograms above dropped timed items (3.0+,
 * Settings.hologram). A TextDisplay spawns with the item and follows its
 * remaining time once per second; it disappears when the item is picked
 * up, despawns or expires.
 *
 * Classic servers only: spawning and updating entities must happen on
 * the owning region, and the refresh task is a global one, so on Folia
 * the feature stays off (documented limitation).
 */
public final class GroundHolograms implements Listener {

    private final TimedItemPlugin plugin;
    /** Item entity UUID -> its display entity. */
    private final Map<UUID, Object> displays = new ConcurrentHashMap<UUID, Object>();
    private SchedulerCompat.Handle task;

    public GroundHolograms(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Registers the listeners and starts the 1s refresh task (non-Folia only). */
    public void start() {
        if (SchedulerCompat.isFolia() || !HologramCompat.isAvailable()) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = SchedulerCompat.runGlobalTimer(plugin, 20L, 20L, this::refresh);
    }

    /** Removes every display; called on reload and disable. */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Object display : displays.values()) {
            HologramCompat.remove(display);
        }
        displays.clear();
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        PluginConfig config = plugin.pluginConfig();
        if (!config.isHologramEnabled()) {
            return;
        }
        Item item = event.getEntity();
        TimeLore.TimerInfo info = TimeLore.analyze(item.getItemStack(), config);
        if (info == null) {
            return;
        }
        Location at = item.getLocation().add(0, config.getHologramHeight(), 0);
        Object display = HologramCompat.spawn(at, render(item.getItemStack(), info, config));
        if (display != null) {
            displays.put(item.getUniqueId(), display);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Object display = displays.remove(event.getItem().getUniqueId());
        if (display != null) {
            HologramCompat.remove(display);
        }
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        Object display = displays.remove(event.getEntity().getUniqueId());
        if (display != null) {
            HologramCompat.remove(display);
        }
    }

    /** Updates every tracked hologram; cleans up ones whose item is gone. */
    private void refresh() {
        PluginConfig config = plugin.pluginConfig();
        for (Map.Entry<UUID, Object> entry : displays.entrySet()) {
            Entity item = Bukkit.getEntity(entry.getKey());
            if (item == null || !item.isValid() || !(item instanceof Item)) {
                HologramCompat.remove(entry.getValue());
                displays.remove(entry.getKey());
                continue;
            }
            ItemStack stack = ((Item) item).getItemStack();
            TimeLore.TimerInfo info = TimeLore.analyze(stack, config);
            if (info == null) {
                HologramCompat.remove(entry.getValue());
                displays.remove(entry.getKey());
                continue;
            }
            HologramCompat.setText(entry.getValue(), render(stack, info, config));
        }
    }

    private String render(ItemStack stack, TimeLore.TimerInfo info, PluginConfig config) {
        long remaining;
        if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            remaining = Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
        } else {
            remaining = info.getSeconds();
        }
        if (remaining < 0) {
            remaining = 0;
        }
        String text = config.getHologramFormat()
                .replace("{time}", TimeLore.formatDuration(remaining, config))
                .replace("{item}", plugin.hooks() == null
                        ? stack.getType().name() : plugin.hooks().describe(stack));
        return PluginConfig.color(text);
    }
}
