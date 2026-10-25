package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ground expiry for Folia. Entities cannot be listed across regions, so
 * instead of walking every world each interval, every relevant entity
 * schedules its own repeating check on its entity scheduler: the body
 * runs on the region owning the entity and the task retires itself when
 * the entity is gone.
 *
 * Tracked entities: dropped items (ItemSpawnEvent, plus ChunkLoadEvent
 * for items restored from disk) and the contents of item frames / armor
 * stands (checked on chunk load and when a player interacts with them,
 * since their contents change without a spawn event). A tracking task
 * stops itself as soon as the entity holds no timed item, so the cost
 * stays proportional to the timed ground items actually out there.
 */
public final class FoliaGroundExpiry implements Listener {

    private final TimedItemPlugin plugin;
    /** Entity UUID -> its repeating expiry check. */
    private final Map<UUID, SchedulerCompat.Handle> tracked = new ConcurrentHashMap<UUID, SchedulerCompat.Handle>();
    private long periodTicks = 200L;
    private volatile boolean running;

    public FoliaGroundExpiry(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Registers the listeners and (re)starts tracking with the given period. */
    public void start(long periodTicks) {
        this.periodTicks = Math.max(20L, periodTicks);
        if (!running) {
            running = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    /** Cancels every tracking task and stops listening. */
    public void stop() {
        running = false;
        for (SchedulerCompat.Handle handle : tracked.values()) {
            handle.cancel();
        }
        tracked.clear();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        scheduleCheck(event.getEntity());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        // fires on the region owning the chunk - reading its entities is safe
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item || entity instanceof ItemFrame
                    || entity instanceof ArmorStand) {
                scheduleCheck(entity);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame || entity instanceof ArmorStand) {
            // the frame's/stand's contents may have just changed
            scheduleCheck(entity);
        }
    }

    /** Runs one analysis on the entity's region and starts tracking if timed. */
    private void scheduleCheck(Entity entity) {
        if (!running) {
            return;
        }
        SchedulerCompat.runAtEntity(plugin, entity, () -> {
            if (running && entity.isValid() && holdsTimed(entity, plugin.pluginConfig())) {
                startTracking(entity);
            }
        });
    }

    private void startTracking(Entity entity) {
        final UUID uuid = entity.getUniqueId();
        tracked.computeIfAbsent(uuid, key -> SchedulerCompat.runEntityTimer(
                plugin, entity, periodTicks, periodTicks,
                () -> check(entity),
                // entity removed: drop the tracking entry
                () -> tracked.remove(uuid)));
    }

    private void stopTracking(Entity entity) {
        SchedulerCompat.Handle handle = tracked.remove(entity.getUniqueId());
        if (handle != null) {
            handle.cancel();
        }
    }

    /** One periodic run: expire the timed content or stop tracking. */
    private void check(Entity entity) {
        if (!running) {
            return;
        }
        PluginConfig config = plugin.pluginConfig();
        if (!entity.isValid()) {
            stopTracking(entity);
            return;
        }
        if (entity instanceof Item) {
            ItemStack stack = ((Item) entity).getItemStack();
            if (isExpired(stack, config)) {
                expire(entity, stack, config, entity::remove);
                stopTracking(entity);
            } else if (!isTimed(stack, config)) {
                stopTracking(entity);
            }
            return;
        }
        if (entity instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) entity;
            ItemStack stack = frame.getItem();
            if (stack.getType() != Material.AIR && isExpired(stack, config)) {
                expire(entity, stack, config,
                        () -> frame.setItem(new ItemStack(Material.AIR)));
                stopTracking(entity);
            } else if (!isTimed(stack, config)) {
                stopTracking(entity);
            }
            return;
        }
        if (entity instanceof ArmorStand) {
            EntityEquipment equipment = ((ArmorStand) entity).getEquipment();
            if (equipment == null) {
                stopTracking(entity);
                return;
            }
            ItemStack[] armor = equipment.getArmorContents();
            boolean changed = false;
            boolean anyTimed = false;
            for (int i = 0; i < armor.length; i++) {
                ItemStack piece = armor[i];
                if (piece == null || piece.getType() == Material.AIR) {
                    continue;
                }
                final int index = i;
                if (isExpired(piece, config)) {
                    expire(entity, piece, config, () -> armor[index] = null);
                    changed = true;
                } else if (isTimed(piece, config)) {
                    anyTimed = true;
                }
            }
            if (changed) {
                equipment.setArmorContents(armor);
            }
            if (!anyTimed) {
                stopTracking(entity);
            }
        }
    }

    private void expire(Entity entity, ItemStack stack, PluginConfig config, Runnable removal) {
        plugin.expiryService().expire(stack, null, entity.getLocation(), entity.getWorld(),
                removal, false, config);
    }

    /** True when the entity carries at least one expiring (EXPIRE-type) item. */
    private boolean holdsTimed(Entity entity, PluginConfig config) {
        if (entity instanceof Item) {
            return isTimed(((Item) entity).getItemStack(), config);
        }
        if (entity instanceof ItemFrame) {
            return isTimed(((ItemFrame) entity).getItem(), config);
        }
        if (entity instanceof ArmorStand) {
            EntityEquipment equipment = ((ArmorStand) entity).getEquipment();
            if (equipment == null) {
                return false;
            }
            for (ItemStack piece : equipment.getArmorContents()) {
                if (isTimed(piece, config)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ground items only ever expire by their concrete expiry date. */
    private static boolean isTimed(ItemStack stack, PluginConfig config) {
        TimeLore.TimerInfo info = TimeLore.analyze(stack, config);
        return info != null && info.getType() == TimeLore.Type.EXPIRE;
    }

    private static boolean isExpired(ItemStack stack, PluginConfig config) {
        TimeLore.TimerInfo info = TimeLore.analyze(stack, config);
        return info != null && info.getType() == TimeLore.Type.EXPIRE
                && info.getExpiry() != null && info.getExpiry().isBefore(java.time.LocalDateTime.now());
    }
}
