package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import me.albert.timeditem.SchedulerCompat;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Keeps TimedItem in step with HuskSync (WiIIiam278, API v3 as shipped
 * by 3.x and 4.x). HuskSync applies the synced inventory some ticks
 * after PlayerJoinEvent, while the player is still locked - the join
 * scan sees an empty inventory - so this hook re-runs the scan once the
 * sync completed. SyncCompleteEvent also fires for admin rollbacks,
 * /invsee edits and API updates on an online player, so those land in
 * the scans too.
 *
 * No timer state has to move through this hook: timed item data (PDC +
 * lore) survives the sync untouched because HuskSync serializes item
 * stacks as raw NBT. The event is fired on the main thread; the rescan
 * is deferred one tick through SchedulerCompat so it lands on the
 * player's own region thread on Folia.
 *
 * Passive by design - nothing is ever cancelled here.
 */
final class HuskSyncHook {

    private HuskSyncHook() {
    }

    /** HuskSync v3+/v4 Bukkit API event (v2 is EOL and shaped differently). */
    private static final String[] EVENT_CANDIDATES = {
            "net.william278.husksync.event.BukkitSyncCompleteEvent"};

    static ReflectiveEventHook.Registration register(TimedItemPlugin plugin, String hookName) {
        final Class<?> eventClass = HookReflection.findClass(EVENT_CANDIDATES);
        if (eventClass == null) {
            DebugLog.verbose(hookName + ": no known sync event class on this server version");
            return null;
        }
        return register(plugin, hookName, eventClass);
    }

    /** Overload taking a resolved event class - also the unit-test seam. */
    static ReflectiveEventHook.Registration register(TimedItemPlugin plugin, String hookName,
                                                     Class<?> eventClass) {
        final Method eventPlayer = HookReflection.findGetter(eventClass, "getplayer", Player.class);
        // findAnyMethod matches the exact name (unlike findGetter, which
        // lowercases) - getUser is camelCase on BukkitPlayerEvent
        final Method userGetter = HookReflection.findAnyMethod(eventClass, "getUser", 0);
        // OnlineUser is the common abstract class - getPlayer() lives on the
        // Bukkit subclass, so probe it off the getter's declared return type
        final Class<?> userClass = userGetter == null ? null : userGetter.getReturnType();
        final Method userPlayer = userClass == null ? null
                : HookReflection.findGetter(userClass, "getplayer", Player.class);
        final Method userUuid = userClass == null ? null
                : HookReflection.findGetter(userClass, "getuuid", UUID.class);
        if (eventPlayer == null && userPlayer == null && userUuid == null) {
            DebugLog.verbose(hookName + ": no player/uuid getter on the sync event - hook disabled");
            return null;
        }
        return ReflectiveEventHook.register(plugin, hookName, new String[]{eventClass.getName()},
                EventPriority.MONITOR, false, event -> {
                    Player player = playerOf(event, eventPlayer, userGetter, userPlayer, userUuid);
                    if (player == null) {
                        return;
                    }
                    SchedulerCompat.runAtPlayerLater(plugin, player, () -> {
                        if (player.isOnline()) {
                            plugin.expiryListener().scanInventory(player, true);
                        }
                    }, 1L);
                });
    }

    /** Resolves the event's user to a live Bukkit player, or null. */
    private static Player playerOf(Object event, Method eventPlayer, Method userGetter,
                                   Method userPlayer, Method userUuid) {
        if (eventPlayer != null) {
            Object direct = HookReflection.invoke(eventPlayer, event);
            if (direct instanceof Player) {
                return (Player) direct;
            }
        }
        if (userGetter == null) {
            return null;
        }
        Object user = HookReflection.invoke(userGetter, event);
        if (user == null) {
            return null;
        }
        if (userPlayer != null) {
            Object direct = HookReflection.invoke(userPlayer, user);
            if (direct instanceof Player) {
                return (Player) direct;
            }
        }
        if (userUuid != null) {
            Object id = HookReflection.invoke(userUuid, user);
            if (id instanceof UUID) {
                return Bukkit.getPlayer((UUID) id);
            }
        }
        return null;
    }
}
