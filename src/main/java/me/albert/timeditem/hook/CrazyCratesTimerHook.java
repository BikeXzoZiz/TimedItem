package me.albert.timeditem.hook;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

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
        return ReflectiveEventHook.register(plugin, "CrazyCrates auto-timer", EVENT_CLASSES,
                EventPriority.NORMAL, true, event -> {
                    ItemStack item = prizeItem(event);
                    if (item == null || item.getType() == Material.AIR) {
                        return;
                    }
                    AutoTimer.apply(item, plugin, seconds);
                    callSetter(event, item);
                });
    }

    /** Direct event getter first, then the prize object it may hand out. */
    private static ItemStack prizeItem(Object event) {
        Method getter = HookReflection.findGetter(event.getClass(), "item", ItemStack.class);
        Object item = getter == null ? null : HookReflection.invoke(getter, event);
        if (item instanceof ItemStack) {
            return (ItemStack) item;
        }
        // fall back: event.getPrize() -> prize.getItemStack()-style getter
        Object prize = null;
        for (Method method : event.getClass().getMethods()) {
            if (method.getParameterCount() == 0
                    && method.getReturnType().getSimpleName().equals("Prize")) {
                prize = HookReflection.invoke(method, event);
                break;
            }
        }
        if (prize != null) {
            Method itemGetter = HookReflection.findGetter(prize.getClass(), "item", ItemStack.class);
            Object stack = itemGetter == null ? null : HookReflection.invoke(itemGetter, prize);
            if (stack instanceof ItemStack) {
                return (ItemStack) stack;
            }
        }
        return null;
    }

    /** Pushes the mutated stack back when the event offers a setter. */
    private static void callSetter(Object event, ItemStack item) {
        for (Method method : event.getClass().getMethods()) {
            if (method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == ItemStack.class
                    && method.getName().toLowerCase(java.util.Locale.ROOT).startsWith("set")) {
                HookReflection.invoke(method, event, item);
                return;
            }
        }
    }
}
