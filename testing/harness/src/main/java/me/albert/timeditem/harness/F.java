package me.albert.timeditem.harness;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Scheduler access for the harness itself, mirroring the plugin's
 * SchedulerCompat: on Folia every location-bound interaction is
 * dispatched to the owning region, everything else to the global
 * region. Classic servers use the Bukkit scheduler. All Folia access is
 * reflective because the compile target is spigot-api 1.13.
 */
public final class F {

    private static final boolean FOLIA;
    private static final Method REGION_RUN;
    private static final Method REGION_RUN_DELAYED;
    private static final Method REGION_RUN_TIMER;
    private static final Method GLOBAL_RUN;
    private static final Method GLOBAL_RUN_DELAYED;
    private static final Method GLOBAL_RUN_TIMER;
    private static final Method ASYNC_RUN;
    private static final Method ENTITY_SCHEDULER;
    private static final Method ENTITY_RUN;

    static {
        boolean folia = false;
        Method regionRun = null;
        Method regionRunDelayed = null;
        Method regionRunTimer = null;
        Method globalRun = null;
        Method globalRunDelayed = null;
        Method globalRunTimer = null;
        Method asyncRun = null;
        Method entityScheduler = null;
        Method entityRun = null;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Class<?> regionClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            regionRun = regionClass.getMethod("execute", Plugin.class, Location.class, Runnable.class);
            regionRunDelayed = regionClass.getMethod("runDelayed", Plugin.class,
                    Location.class, Consumer.class, long.class);
            regionRunTimer = regionClass.getMethod("runAtFixedRate", Plugin.class,
                    Location.class, Consumer.class, long.class, long.class);
            Class<?> globalClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            globalRun = globalClass.getMethod("execute", Plugin.class, Runnable.class);
            globalRunDelayed = globalClass.getMethod("runDelayed", Plugin.class,
                    Consumer.class, long.class);
            globalRunTimer = globalClass.getMethod("runAtFixedRate", Plugin.class,
                    Consumer.class, long.class, long.class);
            asyncRun = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler")
                    .getMethod("runNow", Plugin.class, Consumer.class);
            entityScheduler = Class.forName("org.bukkit.entity.Entity")
                    .getMethod("getScheduler");
            entityRun = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.EntityScheduler")
                    .getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
            Object region = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Object global = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Object async = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            // stash the instances in static finals via a holder
            Holder.region = region;
            Holder.global = global;
            Holder.async = async;
            folia = true;
        } catch (Throwable ignored) {
            folia = false;
        }
        FOLIA = folia;
        REGION_RUN = regionRun;
        REGION_RUN_DELAYED = regionRunDelayed;
        REGION_RUN_TIMER = regionRunTimer;
        GLOBAL_RUN = globalRun;
        GLOBAL_RUN_DELAYED = globalRunDelayed;
        GLOBAL_RUN_TIMER = globalRunTimer;
        ASYNC_RUN = asyncRun;
        ENTITY_SCHEDULER = entityScheduler;
        ENTITY_RUN = entityRun;
    }

    /** Statically initialized holder for the Folia scheduler instances. */
    private static final class Holder {
        static Object region;
        static Object global;
        static Object async;
    }

    private F() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public interface Handle {
        void cancel();
    }

    private static Consumer<Object> wrap(final Runnable body) {
        return task -> body.run();
    }

    /** Runs the body on the region owning the location (next tick on Bukkit). */
    public static void at(Plugin plugin, Location loc, Runnable body) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, body);
            return;
        }
        try {
            REGION_RUN.invoke(Holder.region, plugin, loc, body);
        } catch (Throwable t) {
            body.run(); // best effort
        }
    }

    /** Delayed run; the location selects the region on Folia (may be null for global). */
    public static void later(Plugin plugin, Location loc, long ticks, Runnable body) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskLater(plugin, body, ticks);
            return;
        }
        try {
            if (loc != null) {
                REGION_RUN_DELAYED.invoke(Holder.region, plugin, loc, wrap(body), ticks);
            } else {
                GLOBAL_RUN_DELAYED.invoke(Holder.global, plugin, wrap(body), ticks);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Folia dispatch failed: " + t, t);
        }
    }

    /** Repeating timer anchored at the location (classic: main thread). */
    public static Handle timer(Plugin plugin, Location loc, long delayTicks, long periodTicks,
                               Runnable body) {
        if (!FOLIA) {
            final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, body,
                    delayTicks, periodTicks);
            return task::cancel;
        }
        try {
            Object task = loc != null
                    ? REGION_RUN_TIMER.invoke(Holder.region, plugin, loc, wrap(body), delayTicks, periodTicks)
                    : GLOBAL_RUN_TIMER.invoke(Holder.global, plugin, wrap(body), delayTicks, periodTicks);
            final Object handle = task;
            return () -> {
                try {
                    Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask")
                            .getMethod("cancel").invoke(handle);
                } catch (Throwable ignored) {
                }
            };
        } catch (Throwable t) {
            throw new IllegalStateException("Folia dispatch failed: " + t, t);
        }
    }

    /** Runs immediately on the entity's owning region (Bukkit: main thread). */
    public static void atEntity(Plugin plugin, Entity entity, Runnable body) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, body);
            return;
        }
        try {
            Object scheduler = ENTITY_SCHEDULER.invoke(entity);
            ENTITY_RUN.invoke(scheduler, plugin, body, null, 0L);
        } catch (Throwable t) {
            throw new IllegalStateException("Folia entity dispatch failed: " + t, t);
        }
    }

    /** Runs off any region thread. */
    public static void async(Plugin plugin, Runnable body) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, body);
            return;
        }
        try {
            ASYNC_RUN.invoke(Holder.async, plugin, wrap(body));
        } catch (Throwable t) {
            throw new IllegalStateException("Folia async dispatch failed: " + t, t);
        }
    }
}
