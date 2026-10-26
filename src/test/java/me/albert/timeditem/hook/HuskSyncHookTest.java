package me.albert.timeditem.hook;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * HuskSync (net.william278.husksync) is never on the compile or test
 * classpath - these stubs mirror the API shapes the sync hook probes:
 * BukkitSyncCompleteEvent extends BukkitPlayerEvent, whose getUser()
 * returns an OnlineUser (a common class - the Bukkit accessors live on
 * its subclass), so the player must come from the user getter's return
 * type or its getUuid(). A wider stub pins the event-level getPlayer
 * probe in case a future build promotes the getter onto the event.
 */
class HuskSyncHookTest {

    /** HuskSync: BukkitEvent - shared static HandlerList (inherited resolution). */
    public static class BukkitEventStub extends Event {
        private static final HandlerList handlers = new HandlerList();

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    /** HuskSync: OnlineUser - common class; getPlayer/getUuid resolve below it. */
    public static class OnlineUserStub {
        public Player getPlayer() {
            return null;
        }

        public UUID getUuid() {
            return UUID.randomUUID();
        }
    }

    /** HuskSync: BukkitSyncCompleteEvent - getUser() is the only accessor. */
    public static class SyncCompleteEventStub extends BukkitEventStub {
        public OnlineUserStub getUser() {
            return new OnlineUserStub();
        }
    }

    /** A future build might expose the player directly on the event. */
    public static class SyncCompleteEventWithPlayerStub extends BukkitEventStub {
        public Player getPlayer() {
            return null;
        }
    }

    @Test
    void probesResolveHuskSyncShapedMembers() {
        // findAnyMethod matches the exact name (unlike findGetter, which
        // lowercases) - the camelCase here is load-bearing
        Method userGetter = HookReflection.findAnyMethod(SyncCompleteEventStub.class, "getUser", 0);
        assertNotNull(userGetter);
        Class<?> userClass = userGetter.getReturnType();
        assertNotNull(HookReflection.findGetter(userClass, "getplayer", Player.class));
        assertNotNull(HookReflection.findGetter(userClass, "getuuid", UUID.class));
        assertNotNull(HookReflection.findGetter(
                SyncCompleteEventWithPlayerStub.class, "getplayer", Player.class));
    }

    @Test
    void registersAgainstUserShapedEvent() {
        // plugin is null here: RegisteredListener stores it untouched and the
        // handler never fires in a unit test - this pins the registration path
        // (event class + getUser probe + HandlerList lookup) for the real shape
        ReflectiveEventHook.Registration registration = HuskSyncHook.register(
                null, "HuskSync", SyncCompleteEventStub.class);
        assertNotNull(registration);
        registration.unregister();
    }

    @Test
    void registersAgainstEventPlayerShapedEvent() {
        ReflectiveEventHook.Registration registration = HuskSyncHook.register(
                null, "HuskSync", SyncCompleteEventWithPlayerStub.class);
        assertNotNull(registration);
        registration.unregister();
    }
}
