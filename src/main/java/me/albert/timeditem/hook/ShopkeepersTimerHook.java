package me.albert.timeditem.hook;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stamps the configured expiry onto items bought from Shopkeepers
 * shops. Since Shopkeepers 2.19 the trade event lets plugins alter the
 * result item the buying player receives; older versions ignore the
 * change, so the hook reports itself unavailable when no setter exists.
 */
final class ShopkeepersTimerHook {

    private static final String[] EVENT_CLASSES = {
            "com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent"
    };

    private ShopkeepersTimerHook() {
    }

    static ReflectiveEventHook.Registration register(TimedItemPlugin plugin, long seconds) {
        if (seconds <= 0) {
            return null;
        }
        // the event class is fixed per server - resolve its accessors on
        // the first trade instead of scanning getMethods() on every event
        Map<Class<?>, Accessors> accessors = new ConcurrentHashMap<Class<?>, Accessors>();
        return ReflectiveEventHook.register(plugin, "Shopkeepers auto-timer", EVENT_CLASSES,
                EventPriority.NORMAL, true, event -> {
                    Accessors acc = accessors.computeIfAbsent(event.getClass(), Accessors::of);
                    if (acc.getter == null) {
                        return; // event shape unknown on this version
                    }
                    ItemStack item = (ItemStack) HookReflection.invoke(acc.getter, event);
                    if (item == null || item.getType() == Material.AIR) {
                        return;
                    }
                    AutoTimer.apply(item, plugin, seconds);
                    if (acc.setter != null) {
                        HookReflection.invoke(acc.setter, event, item);
                    }
                });
    }

    /** Resolved once per event class; a null getter means unknown shape. */
    private static final class Accessors {
        final Method getter;
        final Method setter;

        private Accessors(Method getter, Method setter) {
            this.getter = getter;
            this.setter = setter;
        }

        static Accessors of(Class<?> eventClass) {
            return new Accessors(
                    HookReflection.findGetter(eventClass, "item", ItemStack.class),
                    setter(eventClass));
        }

        /** The first public method accepting a single ItemStack named like a setter. */
        private static Method setter(Class<?> eventClass) {
            for (Method method : eventClass.getMethods()) {
                if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == ItemStack.class
                        && method.getName().toLowerCase(java.util.Locale.ROOT).startsWith("set")) {
                    return method;
                }
            }
            return null;
        }
    }
}
