package me.albert.timeditem.hook;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

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
        return ReflectiveEventHook.register(plugin, "Shopkeepers auto-timer", EVENT_CLASSES,
                EventPriority.NORMAL, true, event -> {
                    Class<?> eventClass = event.getClass();
                    Method getter = HookReflection.findGetter(eventClass, "item", ItemStack.class);
                    Method setter = findSetter(eventClass);
                    if (getter == null) {
                        return; // event shape unknown on this version
                    }
                    ItemStack item = (ItemStack) HookReflection.invoke(getter, event);
                    if (item == null || item.getType() == Material.AIR) {
                        return;
                    }
                    AutoTimer.apply(item, plugin, seconds);
                    if (setter != null) {
                        HookReflection.invoke(setter, event, item);
                    }
                });
    }

    /** The first public method accepting a single ItemStack named like a setter. */
    private static Method findSetter(Class<?> eventClass) {
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
