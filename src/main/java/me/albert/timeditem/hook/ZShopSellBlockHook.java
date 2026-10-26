package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Blocks selling timed items on zShop (GroupeZ, 3.x). Unlike the other
 * shop hooks the sell-all event carries no ItemStack getter on itself:
 * it wraps one {@code ShopAction} per shop entry about to be sold, and
 * only {@code ShopAction.getItemStack()} holds the player's real stack -
 * so this hook supplies its own item extractor to the shared block path.
 *
 * Fired by "/zshop sellall" and when the sell-inventory GUI closes,
 * always before any stock is removed; cancelling is safe (the GUI flow
 * hands leftover items back to the player).
 *
 * The single-sell event (ZShopSellEvent) is deliberately not hooked: it
 * only exposes the shop's configured entry, and zShop removes stock via
 * ItemStack.isSimilar against that entry - a timed item (extra PDC +
 * countdown lore) never matches, so a material based check there would
 * only cancel plain-item sales the shop would never touch.
 */
final class ZShopSellBlockHook {

    private ZShopSellBlockHook() {
    }

    /** zShop 3.x+; earlier premium builds shipped no public event API. */
    private static final String[] EVENT_CANDIDATES = {
            "fr.maxlego08.shop.api.event.events.ZShopSellAllEvent"};
    private static final String ACTION_CLASS = "fr.maxlego08.shop.api.event.ShopAction";

    static ReflectiveEventHook.Registration register(TimedItemPlugin plugin, String hookName) {
        final Class<?> eventClass = HookReflection.findClass(EVENT_CANDIDATES);
        if (eventClass == null) {
            DebugLog.verbose(hookName + ": no known sell event class on this server version");
            return null;
        }
        final Class<?> actionClass = HookReflection.findClass(ACTION_CLASS);
        if (actionClass == null) {
            DebugLog.verbose(hookName + ": ShopAction class not found - hook disabled");
            return null;
        }
        final Method actionItem = HookReflection.findGetter(actionClass, "getitemstack", ItemStack.class);
        if (actionItem == null) {
            DebugLog.verbose(hookName + ": no ShopAction item getter - hook disabled");
            return null;
        }
        final Method playerGetter = HookReflection.findGetter(eventClass, "getplayer", Player.class);
        final Method actionsGetter = HookReflection.findCollectionGetter(eventClass, "getshopactions");
        if (actionsGetter == null) {
            DebugLog.verbose(hookName + ": no shop actions getter - hook disabled");
            return null;
        }
        return ShopSellBlockHook.register(plugin, hookName, eventClass, playerGetter,
                new ShopSellBlockHook.ItemsOf() {
                    @Override
                    public List<ItemStack> items(Object event) {
                        List<ItemStack> out = new ArrayList<ItemStack>();
                        Iterable<?> actions = (Iterable<?>) HookReflection.invoke(actionsGetter, event);
                        if (actions == null) {
                            return out;
                        }
                        for (Object action : actions) {
                            if (!actionClass.isInstance(action)) {
                                continue;
                            }
                            Object stack = HookReflection.invoke(actionItem, action);
                            if (stack instanceof ItemStack) {
                                out.add((ItemStack) stack);
                            }
                        }
                        return out;
                    }
                });
    }
}
