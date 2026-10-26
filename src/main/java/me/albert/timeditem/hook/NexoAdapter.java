package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Nexo (the Oraxen successor) exposes a tiny static API:
 * NexoItems.idFromItem(ItemStack) returns null for non-Nexo stacks and
 * NexoItems.itemFromId(String) rebuilds one.
 */
final class NexoAdapter implements CustomItemAdapter {

    private final Class<?> items;
    private final Method idFromItem;
    private final Method itemFromId;

    NexoAdapter() {
        this.items = HookReflection.findClass("com.nexomc.nexo.api.NexoItems");
        this.idFromItem = items == null ? null
                : HookReflection.findMethod(items, "idFromItem", ItemStack.class);
        this.itemFromId = items == null ? null
                : HookReflection.findMethod(items, "itemFromId", String.class);
    }

    @Override
    public String id() {
        return "nexo";
    }

    @Override
    public boolean isAvailable() {
        return items != null && idFromItem != null && itemFromId != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            String id = (String) HookReflection.invoke(idFromItem, null, item);
            return id == null || id.isEmpty() ? null : id;
        } catch (Throwable t) {
            DebugLog.verbose("Nexo identify failed: " + t);
            return null;
        }
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length != 1 || idParts[0].isEmpty()) {
            return null;
        }
        try {
            Object stack = HookReflection.invoke(itemFromId, null, idParts[0]);
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (Throwable t) {
            DebugLog.verbose("Nexo build failed: " + t);
            return null;
        }
    }
}
