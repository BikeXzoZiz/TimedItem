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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Expiry transformation (2.8+): the lane config enables
 * Settings.expiry_transform with the default profile "ash"
 * (GUNPOWDER, name "&8Ash"), so an expired dropped item must leave a
 * gunpowder replacement on the ground and the event must carry it.
 */
final class TransformScenario implements Scenario {

    private final AtomicReference<ItemExpireEvent> lastEvent = new AtomicReference<ItemExpireEvent>();
    private volatile Entity replacement;
    private volatile Entity original;
    private Listener listener;

    @Override
    public String name() {
        return "transform";
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
        final ItemStack expired = new ItemStack(Material.IRON_INGOT);
        TimedItemAPI.setExpiryFromNow(expired, 1L);
        listener = new Listener() {
            @EventHandler
            public void onExpire(ItemExpireEvent event) {
                if (event.getItem().getType() == Material.IRON_INGOT) {
                    lastEvent.set(event);
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
        ctx.cleanup(new Runnable() {
            @Override
            public void run() {
                removeQuietly(ctx, original);
                removeQuietly(ctx, replacement);
            }
        });
        ctx.laterAt(ctx.base(), 40L, new Runnable() {
            @Override
            public void run() {
                ctx.dropAt(ctx.offset(0, 1, 0), expired, new java.util.function.Consumer<Entity>() {
                    @Override
                    public void accept(Entity entity) {
                        original = entity;
                    }
                });
            }
        });
    }

    private static void removeQuietly(final Context ctx, final Entity entity) {
        if (entity != null && entity.isValid()) {
            ctx.at(entity.getLocation(), new Runnable() {
                @Override
                public void run() {
                    entity.remove();
                }
            });
        }
    }

    @Override
    public void poll(final Context ctx) {
        final ItemExpireEvent event = lastEvent.get();
        if (event == null) {
            return;
        }
        ctx.check(event.getReplacement() != null, "ItemExpireEvent carries the ash replacement");
        if (event.getReplacement() == null) {
            ctx.finish(false, "no replacement in ItemExpireEvent");
            return;
        }
        ctx.nearbyEntities(ctx.base(), 8.0, new java.util.function.Consumer<java.util.List<Entity>>() {
            @Override
            public void accept(java.util.List<Entity> entities) {
                boolean found = false;
                for (Entity entity : entities) {
                    if (entity instanceof Item
                            && ((Item) entity).getItemStack().getType() == Material.GUNPOWDER) {
                        found = true;
                        replacement = entity;
                    }
                }
                if (found) {
                    ctx.finish("expired iron ingot turned into GUNPOWDER (profile ash) on the ground");
                } else if (F.isFolia() && ctx.elapsedSeconds() >= 20) {
                    // Folia finding: the event carries the replacement but the
                    // dropped entity never becomes visible to the owning chunk
                    // read - report precisely instead of timing out
                    ctx.finish(false, "event carried GUNPOWDER replacement but the dropped "
                            + "entity never appeared on Folia (ground-expiry transformation "
                            + "delivery needs investigation - works on Paper/Purpur)");
                } else if (ctx.elapsedSeconds() % 10 == 0) {
                    ctx.note("event seen, replacement=" + event.getReplacement().getType()
                            + ", gunpowder entity not found yet");
                }
            }
        });
    }
}
