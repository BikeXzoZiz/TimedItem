package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;

/**
 * Event registration for hooks whose event classes are foreign (and
 * whose package names change between plugin versions). A plain
 * {@code @EventHandler} would need the event class at compile time;
 * instead a raw {@link EventExecutor} is registered on the event's
 * HandlerList, so the event class is only ever touched reflectively at
 * runtime and multiple candidate names can be probed.
 */
final class ReflectiveEventHook {

    /** Receives the foreign event as an opaque object. */
    interface Handler {
        void handle(Object event);
    }

    /** A live registration; call {@link #unregister()} on reload/disable. */
    static final class Registration {
        private final HandlerList list;
        private final RegisteredListener listener;

        private Registration(HandlerList list, RegisteredListener listener) {
            this.list = list;
            this.listener = listener;
        }

        void unregister() {
            try {
                list.unregister(listener);
            } catch (Throwable ignored) {
            }
        }
    }

    private ReflectiveEventHook() {
    }

    /**
     * Registers the first candidate event class that resolves, or
     * returns null (with a verbose note) when none does on this server.
     */
    static Registration register(TimedItemPlugin plugin, String hookName, String[] eventClassCandidates,
                                 EventPriority priority, boolean ignoreCancelled, Handler handler) {
        Class<?> eventClass = HookReflection.findClass(eventClassCandidates);
        if (eventClass == null) {
            DebugLog.verbose(hookName + ": no known event class on this server version");
            return null;
        }
        try {
            Method getHandlerList = eventClass.getMethod("getHandlerList");
            HandlerList list = (HandlerList) getHandlerList.invoke(null);
            Listener dummy = new Listener() {
            };
            EventExecutor executor = new EventExecutor() {
                @Override
                public void execute(Listener l, Event event) {
                    if (!eventClass.isInstance(event)) {
                        return;
                    }
                    try {
                        handler.handle(event);
                    } catch (Throwable t) {
                        DebugLog.verbose(hookName + ": handler failed: " + t);
                    }
                }
            };
            RegisteredListener registered = new RegisteredListener(dummy, executor, priority, plugin, ignoreCancelled);
            list.register(registered);
            return new Registration(list, registered);
        } catch (Throwable t) {
            DebugLog.verbose(hookName + ": could not register ("
                    + eventClass.getName() + "): " + t);
            return null;
        }
    }
}
