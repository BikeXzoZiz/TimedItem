package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import me.albert.timeditem.api.TimedItemAPI;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * GroundHolograms (3.0+, non-Folia + 1.19.4+): a dropped timed item
 * gets a TextDisplay with the configured countdown format above it, and
 * the display disappears when the timer is cleared and the item
 * removed.
 */
final class HologramScenario implements Scenario {

    private static final String TEXT_DISPLAY = "org.bukkit.entity.TextDisplay";

    private volatile Entity dropped;
    private boolean phaseSeen;
    private boolean hologramSeen;
    private boolean textChecked;

    @Override
    public String name() {
        return "holograms";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        if (F.isFolia()) {
            return "ground holograms are classic-only by design (Folia lane)";
        }
        try {
            Class.forName(TEXT_DISPLAY);
        } catch (Throwable t) {
            return "server predates TextDisplay (<1.19.4)";
        }
        if (!plugin.pluginConfig().isHologramEnabled()) {
            return "hologram feature disabled in lane config";
        }
        return null;
    }

    @Override
    public int timeoutSeconds() {
        return 35;
    }

    @Override
    public void start(final Context ctx) {
        final ItemStack timed = new ItemStack(Material.NETHER_STAR);
        TimedItemAPI.setExpiryFromNow(timed, 600L);
        ctx.cleanup(new Runnable() {
            @Override
            public void run() {
                if (dropped != null && dropped.isValid()) {
                    ctx.at(dropped.getLocation(), new Runnable() {
                        @Override
                        public void run() {
                            dropped.remove();
                        }
                    });
                }
            }
        });
        ctx.dropAt(ctx.offset(0, 1, 0), timed, new java.util.function.Consumer<Entity>() {
            @Override
            public void accept(Entity entity) {
                dropped = entity;
            }
        });
    }

    @Override
    public void poll(final Context ctx) {
        if (dropped == null) {
            return;
        }
        if (!phaseSeen) {
            ctx.nearbyEntities(ctx.base(), 6.0, new java.util.function.Consumer<java.util.List<Entity>>() {
                @Override
                public void accept(java.util.List<Entity> entities) {
                    for (Entity entity : entities) {
                        if (isTextDisplay(entity)) {
                            hologramSeen = true;
                            String text = displayText(entity);
                            if (!textChecked && text != null) {
                                textChecked = true;
                                ctx.check(text.contains("\u231B"),
                                        "hologram text contains the countdown format, got: " + text);
                            }
                        }
                    }
                    if (hologramSeen) {
                        phaseSeen = true;
                        ctx.note("hologram appeared above the dropped item");
                        // now clear the timer and remove the item; the display must go
                        ctx.at(dropped.getLocation(), new Runnable() {
                            @Override
                            public void run() {
                                if (dropped != null && dropped.isValid()) {
                                    ItemStack stack = dropped instanceof org.bukkit.entity.Item
                                            ? ((org.bukkit.entity.Item) dropped).getItemStack() : null;
                                    if (stack != null) {
                                        TimedItemAPI.clear(stack);
                                        ((org.bukkit.entity.Item) dropped).setItemStack(stack);
                                    }
                                    dropped.remove();
                                }
                            }
                        });
                    }
                }
            });
            return;
        }
        // phase 2: the display must disappear
        ctx.nearbyEntities(ctx.base(), 6.0, new java.util.function.Consumer<java.util.List<Entity>>() {
            @Override
            public void accept(java.util.List<Entity> entities) {
                boolean anyDisplay = false;
                for (Entity entity : entities) {
                    if (isTextDisplay(entity)) {
                        anyDisplay = true;
                    }
                }
                if (!anyDisplay) {
                    ctx.finish("TextDisplay spawned above the timed drop and vanished after expiry");
                }
            }
        });
    }

    private static boolean isTextDisplay(Entity entity) {
        try {
            return Class.forName(TEXT_DISPLAY).isInstance(entity);
        } catch (Throwable t) {
            return false;
        }
    }

    private static String displayText(Entity entity) {
        try {
            Method text = entity.getClass().getMethod("getText");
            Object value = text.invoke(entity);
            return value instanceof String ? (String) value : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
