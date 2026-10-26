package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.api.ItemExpireEvent;
import me.albert.timeditem.api.TimedItemAPI;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Drops an already-expired timed item on the ground and expects the
 * periodic world-item scan (classic) or the per-entity tracking (Folia)
 * to remove it, with a player-less ItemExpireEvent.
 */
final class GroundExpiryScenario implements Scenario {

    private final AtomicBoolean expireSeen = new AtomicBoolean(false);
    private volatile Entity dropped;
    private Listener listener;

    @Override
    public String name() {
        return "ground_expiry";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        return null;
    }

    @Override
    public int timeoutSeconds() {
        return 40;
    }

    @Override
    public void start(final Context ctx) {
        final ItemStack expired = new ItemStack(Material.DIAMOND);
        TimedItemAPI.setExpiryFromNow(expired, 1L);
        listener = new Listener() {
            @EventHandler
            public void onExpire(me.albert.timeditem.api.ItemExpireEvent event) {
                ctx.note("ItemExpireEvent seen: type=" + event.getItem().getType()
                        + " player=" + event.getPlayer());
                if (event.getItem().getType() == Material.DIAMOND) {
                    expireSeen.set(true);
                }
            }
        };
        ctx.plugin().getServer().getPluginManager().registerEvents(listener, ctx.plugin());
        ctx.cleanup(new Runnable() {
            @Override
            public void run() {
                HandlerList.unregisterAll(listener);
            }
        });
        // give the 1s timer time to run out before the item hits the ground
        ctx.laterAt(ctx.base(), 40L, new Runnable() {
            @Override
            public void run() {
                ctx.dropAt(ctx.offset(0, 1, 0), expired, new java.util.function.Consumer<Entity>() {
                    @Override
                    public void accept(Entity entity) {
                        dropped = entity;
                        ctx.note("dropped expired diamond " + entity.getUniqueId());
                    }
                });
            }
        });
    }

    @Override
    public void poll(final Context ctx) {
        if (dropped == null) {
            return; // still waiting for the drop
        }
        if (ctx.elapsedSeconds() > 0 && ctx.elapsedSeconds() % 10 == 0) {
            final boolean[] scanSees = {false};
            final String[] stackState = {"entity-gone"};
            ctx.at(dropped.getLocation(), new Runnable() {
                @Override
                public void run() {
                    org.bukkit.entity.Entity live = org.bukkit.Bukkit.getEntity(dropped.getUniqueId());
                    if (live != null) {
                        stackState[0] = "liveType=" + live.getType()
                                + " liveClass=" + live.getClass().getSimpleName()
                                + " valid=" + live.isValid()
                                + " droppedClass=" + dropped.getClass().getSimpleName();
                        if (live instanceof org.bukkit.entity.Item) {
                            org.bukkit.inventory.ItemStack onEntity = ((org.bukkit.entity.Item) live).getItemStack();
                            stackState[0] += " isTimed=" + me.albert.timeditem.api.TimedItemAPI.isTimed(onEntity)
                                    + " mat=" + onEntity.getType();
                        }
                    }
                    for (org.bukkit.entity.Item item : live.getWorld().getEntitiesByClass(org.bukkit.entity.Item.class)) {
                        if (item.getUniqueId().equals(dropped.getUniqueId())) {
                            scanSees[0] = true;
                        }
                    }
                }
            });
            int owned = 0;
            if (!F.isFolia()) {
                for (org.bukkit.scheduler.BukkitTask task : org.bukkit.Bukkit.getScheduler().getPendingTasks()) {
                    if (task.getOwner().equals(ctx.plugin())) {
                        owned++;
                    }
                }
            }
            ctx.note("poll: expireSeen=" + expireSeen.get()
                    + " droppedValid=" + (dropped != null && dropped.isValid())
                    + " chunkLoaded=" + ctx.base().getChunk().isLoaded()
                    + " " + stackState[0]
                    + " scanSees=" + scanSees[0]
                    + " timedItemTasks=" + owned);
        }
        ctx.nearbyEntities(ctx.base(), 6.0, new java.util.function.Consumer<java.util.List<Entity>>() {
            @Override
            public void accept(java.util.List<Entity> entities) {
                boolean stillThere = false;
                for (Entity entity : entities) {
                    if (entity.equals(dropped) && entity.isValid()) {
                        stillThere = true;
                    }
                }
                if (expireSeen.get() && !stillThere) {
                    ctx.finish("expired ground item removed, ItemExpireEvent(player=null) seen");
                }
            }
        });
    }
}
