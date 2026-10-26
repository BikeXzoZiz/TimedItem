package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Blocks listing/selling timed items on shop and auction plugins (3.1+).
 * One generic hook covers all supported plugins: it probes the plugin's
 * sell/create event class from a candidate list (packages change between
 * versions and forks), reads the listed item through the usual getter
 * names and cancels the event when the item carries any TimedItem timer.
 *
 * Events whose sold items sit somewhere the getter probe cannot reach
 * (zShop wraps them in a list of shop actions) supply their own
 * {@link ItemsOf} extractor instead - the cancel/message/sound path
 * below stays shared.
 *
 * Like every integration this is reflection only: a plugin version with
 * different event or getter names simply degrades to "not available"
 * instead of crashing - the verbose log notes it when debug is on.
 */
final class ShopSellBlockHook {

    private ShopSellBlockHook() {
    }

    /** Pulls every ItemStack the foreign event is about to sell. */
    interface ItemsOf {
        /** The stacks to analyze; null and an empty list both mean "nothing sellable". */
        List<ItemStack> items(Object event);
    }

    /**
     * Registers the block hook for one shop plugin, or returns null when
     * none of the candidate events exists on this server version.
     */
    static ReflectiveEventHook.Registration register(TimedItemPlugin plugin, String hookName,
                                                     String[] eventCandidates) {
        final Class<?> eventClass = HookReflection.findClass(eventCandidates);
        if (eventClass == null) {
            DebugLog.verbose(hookName + ": no known sell event class on this server version");
            return null;
        }
        // the listed item and acting player differ per event/version
        final Method[] itemGetters = {
                HookReflection.findGetter(eventClass, "getitemstack", ItemStack.class),
                HookReflection.findGetter(eventClass, "getitem", ItemStack.class),
                HookReflection.findGetter(eventClass, "getstack", ItemStack.class),
        };
        final Method playerGetter = HookReflection.findGetter(eventClass, "getplayer", Player.class);
        return register(plugin, hookName, eventClass, playerGetter, new ItemsOf() {
            @Override
            public List<ItemStack> items(Object event) {
                ItemStack item = firstItem(itemGetters, event);
                return item == null
                        ? Collections.<ItemStack>emptyList()
                        : Collections.singletonList(item);
            }
        });
    }

    /**
     * Registers the block hook with a custom item extractor: every stack
     * it returns is analyzed and the first timed item cancels the event.
     */
    static ReflectiveEventHook.Registration register(TimedItemPlugin plugin, String hookName,
                                                     Class<?> eventClass, Method playerGetter,
                                                     ItemsOf itemsOf) {
        final Method cancel = HookReflection.findMethod(eventClass, "setCancelled", boolean.class);
        if (cancel == null) {
            DebugLog.verbose(hookName + ": event is not cancellable - hook disabled");
            return null;
        }
        return ReflectiveEventHook.register(plugin, hookName,
                new String[]{eventClass.getName()}, EventPriority.HIGHEST, true, event -> {
                    List<ItemStack> items = itemsOf.items(event);
                    if (items == null || items.isEmpty()) {
                        return;
                    }
                    PluginConfig config = plugin.pluginConfig();
                    TimeLore.TimerInfo timed = null;
                    for (ItemStack item : items) {
                        if (item == null) {
                            continue;
                        }
                        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
                        if (info != null) {
                            timed = info;
                            break;
                        }
                    }
                    if (timed == null) {
                        return; // no timed item in the sale - selling is fine
                    }
                    try {
                        cancel.invoke(event, true);
                    } catch (Throwable ignored) {
                        return;
                    }
                    Player player = playerGetter == null ? null
                            : (Player) HookReflection.invoke(playerGetter, event);
                    long remaining = remainingOf(timed);
                    if (player != null) {
                        player.sendMessage(config.getPrefix() + config.getSellBlocked()
                                .replace("{time}",
                                        TimeLore.formatDuration(Math.max(0L, remaining), config)));
                        player.playSound(player.getLocation(), config.getWarningSound(),
                                config.getWarningVolume(), config.getWarningPitch());
                    }
                });
    }

    private static ItemStack firstItem(Method[] getters, Object event) {
        for (Method getter : getters) {
            if (getter == null) {
                continue;
            }
            Object result = HookReflection.invoke(getter, event);
            if (result instanceof ItemStack) {
                return (ItemStack) result;
            }
        }
        return null;
    }

    private static long remainingOf(TimeLore.TimerInfo info) {
        if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            return java.time.Duration.between(java.time.LocalDateTime.now(),
                    info.getExpiry()).getSeconds();
        }
        return info.getSeconds();
    }
}
