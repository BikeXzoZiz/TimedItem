package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Scheduler access across Bukkit/Paper and Folia, resolved through
 * reflection because the compile target (spigot-api 1.13) has no Folia
 * classes. Folia is detected by the Folia-only class
 * io.papermc.paper.threadedregions.RegionizedServer.
 *
 * On Folia: "global" tasks run on the global region scheduler, player
 * tasks on the player's entity scheduler (so their inventory is always
 * touched from the owning region thread) and async work on the async
 * scheduler. The periodic tasks therefore enqueue one entity-scheduler
 * run per online player instead of touching every inventory inline (see
 * ExpiryCheckTask and ActionbarTask). Console commands are dispatched on
 * the global region; expiry_commands does this via {@link #runGlobal}.
 */
public final class SchedulerCompat {

    /** Handle for cancelling a repeating task, whatever backend runs it. */
    public interface Handle {
        void cancel();
    }

    private static boolean folia;
    private static boolean resolved;
    private static boolean warned;

    private static Object globalScheduler;
    private static Object asyncScheduler;
    private static Method globalExecute;
    private static Method globalRunAtFixedRate;
    private static Method asyncRunNow;
    private static Method entityScheduler;
    private static Method entityExecute;
    private static Method taskCancel;

    private SchedulerCompat() {
    }

    public static boolean isFolia() {
        resolve();
        return folia;
    }

    /** Runs on the main thread (Bukkit) or the global region (Folia). */
    public static void runGlobal(Plugin plugin, Runnable body) {
        resolve();
        if (folia) {
            try {
                globalExecute.invoke(globalScheduler, plugin, body);
            } catch (Throwable t) {
                warnOnce(t);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, body);
        }
    }

    /**
     * Repeating global task (periodic scans). On Folia the body runs on
     * the global region and must enqueue player work itself.
     */
    public static Handle runGlobalTimer(Plugin plugin, long delayTicks, long periodTicks,
                                        Runnable body) {
        resolve();
        if (folia) {
            try {
                Object task = globalRunAtFixedRate.invoke(globalScheduler, plugin,
                        consumer(body), delayTicks, periodTicks);
                return () -> cancelTask(task);
            } catch (Throwable t) {
                warnOnce(t);
                return () -> {
                };
            }
        }
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, body, delayTicks, periodTicks);
        return task::cancel;
    }

    /** Runs on the player's region at once (next tick on Bukkit). */
    public static void runAtPlayer(Plugin plugin, Player player, Runnable body) {
        resolve();
        if (folia) {
            try {
                entityExecute.invoke(entityScheduler.invoke(player), plugin, body, null, 0L);
            } catch (Throwable t) {
                warnOnce(t); // retired player (quit) also lands here - ignore
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, body);
        }
    }

    /** Runs on the player's region after the delay (Bukkit: main thread later). */
    public static void runAtPlayerLater(Plugin plugin, Player player, Runnable body, long delayTicks) {
        resolve();
        if (folia) {
            try {
                entityExecute.invoke(entityScheduler.invoke(player), plugin, body, null, delayTicks);
            } catch (Throwable t) {
                warnOnce(t);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, body, delayTicks);
        }
    }

    /** Runs off any region thread (HTTP calls and similar). */
    public static void runAsync(Plugin plugin, Runnable body) {
        resolve();
        if (folia) {
            try {
                asyncRunNow.invoke(asyncScheduler, plugin, consumer(body));
            } catch (Throwable t) {
                warnOnce(t);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, body);
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
            globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Class<?> globalClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            globalExecute = globalClass.getMethod("execute", Plugin.class, Runnable.class);
            globalRunAtFixedRate = globalClass.getMethod("runAtFixedRate", Plugin.class,
                    Consumer.class, long.class, long.class);
            Class<?> asyncClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            asyncRunNow = asyncClass.getMethod("runNow", Plugin.class, Consumer.class);
            Class<?> entityClass = Class.forName("org.bukkit.entity.Entity");
            entityScheduler = entityClass.getMethod("getScheduler");
            Class<?> entitySchedulerClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            entityExecute = entitySchedulerClass.getMethod("execute", Plugin.class,
                    Runnable.class, Runnable.class, long.class);
            Class<?> taskClass = Class.forName(
                    "io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            taskCancel = taskClass.getMethod("cancel");
        } catch (Throwable t) {
            folia = false; // Bukkit/Paper: everything through the classic scheduler
        }
    }

    private static Consumer<Object> consumer(final Runnable body) {
        return task -> body.run();
    }

    private static void cancelTask(Object task) {
        try {
            taskCancel.invoke(task);
        } catch (Throwable ignored) {
        }
    }

    private static void warnOnce(Throwable t) {
        if (!warned) {
            warned = true;
            Bukkit.getLogger().warning("[TimedItem] Scheduler dispatch failed: " + t);
        }
    }
}
