package me.albert.timeditem.harness;

import me.albert.timeditem.SchedulerCompat;
import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.api.ItemExpireEvent;
import me.albert.timeditem.api.TimedItemAPI;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Folia lane (skipped everywhere else): a dropped timed item must be
 * removed by the per-entity ground tracking, and - because the
 * hologram feature is designed to stay off on Folia - no TextDisplay
 * may ever appear.
 */
final class FoliaGatesScenario implements Scenario {

    private final AtomicBoolean expireSeen = new AtomicBoolean(false);
    private final AtomicBoolean hologramSeen = new AtomicBoolean(false);
    private volatile Entity dropped;
    private Listener listener;

    @Override
    public String name() {
        return "folia_gates";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        if (!SchedulerCompat.isFolia()) {
            return "Folia only - on classic servers the hologram/container-scan paths run instead";
        }
        return null;
    }

    @Override
    public int timeoutSeconds() {
        return 40;
    }

    @Override
    public void start(final Context ctx) {
        final ItemStack expired = new ItemStack(Material.EMERALD);
        TimedItemAPI.setExpiryFromNow(expired, 1L);
        listener = new Listener() {
            @EventHandler
            public void onExpire(ItemExpireEvent event) {
                if (event.getItem().getType() == Material.EMERALD) {
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
        ctx.laterAt(ctx.base(), 40L, new Runnable() {
            @Override
            public void run() {
                ctx.dropAt(ctx.offset(0, 1, 0), expired, new java.util.function.Consumer<Entity>() {
                    @Override
                    public void accept(Entity entity) {
                        dropped = entity;
                    }
                });
            }
        });
    }

    @Override
    public void poll(final Context ctx) {
        if (dropped == null) {
            return;
        }
        ctx.nearbyEntities(ctx.base(), 6.0, new java.util.function.Consumer<java.util.List<Entity>>() {
            @Override
            public void accept(java.util.List<Entity> entities) {
                boolean stillThere = false;
                for (Entity entity : entities) {
                    try {
                        if (Class.forName("org.bukkit.entity.TextDisplay").isInstance(entity)) {
                            hologramSeen.set(true);
                        }
                    } catch (Throwable ignored) {
                    }
                    if (entity.equals(dropped) && entity.isValid()) {
                        stillThere = true;
                    }
                }
                if (expireSeen.get() && !stillThere) {
                    ctx.check(!hologramSeen.get(), "no TextDisplay on Folia (holograms off by design)");
                    ctx.finish("Folia ground expiry removed the item; holograms stayed off");
                }
            }
        });
    }
}
