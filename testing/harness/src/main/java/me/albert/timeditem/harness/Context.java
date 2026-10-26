package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * State one scenario runs against: assertion collection, the unique
 * base location for this scenario (spawn area, offset per scenario so
 * scenarios never interfere), region-safe dispatch helpers and
 * best-effort cleanup.
 */
public final class Context {

    private final TimedItemPlugin plugin;
    private final Location base;
    private final List<String> failures = new ArrayList<String>();
    private final List<Runnable> cleanups = new ArrayList<Runnable>();
    private final Logger logger;
    private String result;
    private String detail;
    private final long startMillis = System.currentTimeMillis();

    Context(TimedItemPlugin plugin, Location base, Logger logger) {
        this.plugin = plugin;
        this.base = base;
        this.logger = logger;
    }

    TimedItemPlugin plugin() {
        return plugin;
    }

    /** The scenario's private area: spawn + a per-scenario offset. */
    Location base() {
        return base.clone();
    }

    Location offset(int dx, int dy, int dz) {
        return base.clone().add(dx, dy, dz);
    }

    long elapsedSeconds() {
        return (System.currentTimeMillis() - startMillis) / 1000L;
    }

    /** Records a failed expectation (does not end the scenario). */
    public void check(boolean condition, String what) {
        if (!condition) {
            failures.add(what);
        }
    }

    /** Ends the scenario with a verdict; the first call wins. */
    public void finish(boolean pass, String why) {
        if (result != null) {
            return;
        }
        result = pass && failures.isEmpty() ? "PASS" : "FAIL";
        StringBuilder text = new StringBuilder(why == null ? "" : why);
        for (String failure : failures) {
            if (text.length() > 0) {
                text.append("; ");
            }
            text.append("expected ").append(failure);
        }
        detail = text.toString();
    }

    /** Ends the scenario, deriving PASS/FAIL from the collected checks. */
    public void finish(String why) {
        finish(true, why);
    }

    boolean finished() {
        return result != null;
    }

    String result() {
        return result;
    }

    String detail() {
        return detail;
    }

    /** Logs a progress note into the server console/log. */
    public void note(String message) {
        logger.info("[TITEST]     " + message);
    }

    /** Delayed run anywhere; a null location means the global region. */
    public void later(long ticks, Runnable body) {
        F.later(plugin, null, ticks, body);
    }

    /** Delayed run anchored at a location (region-safe on Folia). */
    public void laterAt(Location loc, long ticks, Runnable body) {
        F.later(plugin, loc, ticks, body);
    }

    /** Runs the body on the region owning the location, now-ish. */
    public void at(Location loc, Runnable body) {
        F.at(plugin, loc, body);
    }

    /** Registers best-effort cleanup, run after the scenario ends. */
    public void cleanup(Runnable body) {
        cleanups.add(body);
    }

    void runCleanup() {
        for (Runnable body : cleanups) {
            try {
                body.run();
            } catch (Throwable t) {
                logger.warning("[TITEST] cleanup failed: " + t);
            }
        }
        cleanups.clear();
    }

    /**
     * Entities within the radius of the location, read on the owning
     * region (classic: world lookup; Folia: the location's chunk). The
     * consumer runs synchronously on classic and via the region
     * scheduler on Folia - scenarios only read inside it.
     */
    public void nearbyEntities(final Location center, final double radius,
                               final java.util.function.Consumer<List<Entity>> consumer) {
        at(center, new Runnable() {
            @Override
            public void run() {
                List<Entity> found = new ArrayList<Entity>();
                try {
                    if (!F.isFolia()) {
                        for (Entity entity : center.getWorld().getNearbyEntities(center,
                                radius, radius, radius)) {
                            found.add(entity);
                        }
                    } else {
                        // on Folia only the owning region may touch chunk entities
                        for (Entity entity : center.getChunk().getEntities()) {
                            if (entity.getWorld().equals(center.getWorld())
                                    && entity.getLocation().distanceSquared(center)
                                    <= radius * radius) {
                                found.add(entity);
                            }
                        }
                    }
                } catch (Throwable t) {
                    // world unloading etc. - report an empty snapshot
                }
                consumer.accept(found);
            }
        });
    }

    /** Drops an item at the location on the owning region. */
    public void dropAt(final Location loc, final org.bukkit.inventory.ItemStack stack,
                       final java.util.function.Consumer<Entity> consumer) {
        at(loc, new Runnable() {
            @Override
            public void run() {
                try {
                    Entity dropped = loc.getWorld().dropItem(loc, stack);
                    consumer.accept(dropped);
                } catch (Throwable t) {
                    finish(false, "could not drop item: " + t);
                }
            }
        });
    }

    /** Convenience: default-world spawn grounded to a block top. */
    public static Location defaultBase() {
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        return spawn.clone();
    }
}
