package me.albert.timeditem.hook;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stamps the configured expiry onto items won from CrazyCrates crates.
 * The prize event moved between packages and renamed itself across
 * major versions (PlayerPrizeEvent -> PrizeReceiveEvent, badbones69 ->
 * ryderbelserion), so every candidate class is probed and the ItemStack
 * is located by its getter name; the prize object is checked as a
 * fallback when the event exposes no direct stack.
 */
final class CrazyCratesTimerHook {

    private static final String[] EVENT_CLASSES = {
            "com.badbones69.crazycrates.api.events.PlayerPrizeEvent",
            "com.badbones69.crazycrates.api.events.PrizeReceiveEvent",
            "com.ryderbelserion.crazycrates.api.events.PrizeReceiveEvent",
            "com.ryderbelserion.crazycrates.paper.api.events.PrizeReceiveEvent"
    };

    private CrazyCratesTimerHook() {
    }

    static ReflectiveEventHook.Registration register(TimedItemPlugin plugin, long seconds) {
        if (seconds <= 0) {
            return null;
        }
        // only one candidate event class exists per server - resolve all
        // accessors on the first fired event instead of scanning
        // getMethods() two or three times per prize
        Map<Class<?>, Accessors> accessors = new ConcurrentHashMap<Class<?>, Accessors>();
        return ReflectiveEventHook.register(plugin, "CrazyCrates auto-timer", EVENT_CLASSES,
                EventPriority.NORMAL, true, event -> {
                    Accessors acc = accessors.computeIfAbsent(event.getClass(), Accessors::of);
                    ItemStack item = prizeItem(event, acc);
                    if (item == null || item.getType() == Material.AIR) {
                        return;
                    }
                    AutoTimer.apply(item, plugin, seconds);
                    if (acc.setter != null) {
                        HookReflection.invoke(acc.setter, event, item);
                    }
                });
    }

    /** Direct event getter first, then the prize object it may hand out. */
    private static ItemStack prizeItem(Object event, Accessors acc) {
        Object item = acc.itemGetter == null ? null : HookReflection.invoke(acc.itemGetter, event);
        if (item instanceof ItemStack) {
            return (ItemStack) item;
        }
        // fall back: event.getPrize() -> prize.getItemStack()-style getter
        if (acc.prizeGetter != null && acc.prizeItemGetter != null) {
            Object prize = HookReflection.invoke(acc.prizeGetter, event);
            Object stack = prize == null ? null : HookReflection.invoke(acc.prizeItemGetter, prize);
            if (stack instanceof ItemStack) {
                return (ItemStack) stack;
            }
        }
        return null;
    }

    /** Resolved once per event class; prize accessors come from the declared types. */
    private static final class Accessors {
        final Method itemGetter;
        final Method prizeGetter;
        final Method prizeItemGetter;
        final Method setter;

        private Accessors(Method itemGetter, Method prizeGetter,
                          Method prizeItemGetter, Method setter) {
            this.itemGetter = itemGetter;
            this.prizeGetter = prizeGetter;
            this.prizeItemGetter = prizeItemGetter;
            this.setter = setter;
        }

        static Accessors of(Class<?> eventClass) {
            Method itemGetter = HookReflection.findGetter(eventClass, "item", ItemStack.class);
            Method prizeGetter = findPrizeGetter(eventClass);
            Method prizeItemGetter = prizeGetter == null
                    ? null : HookReflection.findGetter(prizeGetter.getReturnType(), "item", ItemStack.class);
            return new Accessors(itemGetter, prizeGetter, prizeItemGetter, setter(eventClass));
        }

        /** Zero-arg method returning the plugin's Prize type, if any. */
        private static Method findPrizeGetter(Class<?> eventClass) {
            for (Method method : eventClass.getMethods()) {
                if (method.getParameterCount() == 0
                        && method.getReturnType().getSimpleName().equals("Prize")) {
                    return method;
                }
            }
            return null;
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
