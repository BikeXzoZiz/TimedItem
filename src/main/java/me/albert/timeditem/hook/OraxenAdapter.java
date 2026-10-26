package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Oraxen custom items. Oraxen 2.x moved its API to
 * io.th0rgal.oraxen.api.OraxenItems while older builds keep it in
 * io.th0rgal.oraxen.items.OraxenItems - both expose the same
 * getIdByItem / getItemById surface, so probe both.
 */
final class OraxenAdapter implements CustomItemAdapter {

    private final Class<?> items;
    private final Method getIdByItem;
    private final Method getItemById;

    OraxenAdapter() {
        this.items = HookReflection.findClass(
                "io.th0rgal.oraxen.api.OraxenItems",
                "io.th0rgal.oraxen.items.OraxenItems");
        this.getIdByItem = items == null ? null
                : HookReflection.findMethod(items, "getIdByItem", ItemStack.class);
        this.getItemById = items == null ? null
                : HookReflection.findMethod(items, "getItemById", String.class);
    }

    @Override
    public String id() {
        return "oraxen";
    }

    @Override
    public boolean isAvailable() {
        return items != null && getIdByItem != null && getItemById != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            String id = (String) HookReflection.invoke(getIdByItem, null, item);
            return id == null || id.isEmpty() ? null : id;
        } catch (Throwable t) {
            DebugLog.verbose("Oraxen identify failed: " + t);
            return null;
        }
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length != 1 || idParts[0].isEmpty()) {
            return null;
        }
        try {
            Object oraxenItem = HookReflection.invoke(getItemById, null, idParts[0]);
            if (oraxenItem == null) {
                return null;
            }
            Method build = HookReflection.findAnyMethod(oraxenItem.getClass(), "build", 0);
            Object stack = build == null ? null : HookReflection.invoke(build, oraxenItem);
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (Throwable t) {
            DebugLog.verbose("Oraxen build failed: " + t);
            return null;
        }
    }
}
