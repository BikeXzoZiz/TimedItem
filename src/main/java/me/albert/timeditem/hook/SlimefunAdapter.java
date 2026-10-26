package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Slimefun items resolve through SlimefunItem.getByItem / getById.
 * The class moved between packages over the years
 * (me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem in the
 * legacy layout), both are probed.
 */
final class SlimefunAdapter implements CustomItemAdapter {

    private final Class<?> slimefunItem;
    private final Method getByItem;
    private final Method getById;
    private final Method getItem;

    SlimefunAdapter() {
        this.slimefunItem = HookReflection.findClass(
                "io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem",
                "me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem");
        this.getByItem = slimefunItem == null ? null
                : HookReflection.findMethod(slimefunItem, "getByItem", ItemStack.class);
        this.getById = slimefunItem == null ? null
                : HookReflection.findMethod(slimefunItem, "getById", String.class);
        this.getItem = slimefunItem == null ? null
                : HookReflection.findMethod(slimefunItem, "getItem");
    }

    @Override
    public String id() {
        return "slimefun";
    }

    @Override
    public boolean isAvailable() {
        return slimefunItem != null && getByItem != null && getById != null && getItem != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            Object sfItem = HookReflection.invoke(getByItem, null, item);
            if (sfItem == null) {
                return null;
            }
            Method getId = HookReflection.findAnyMethod(sfItem.getClass(), "getID", 0);
            if (getId == null) {
                getId = HookReflection.findAnyMethod(sfItem.getClass(), "getId", 0);
            }
            String id = getId == null ? null : (String) HookReflection.invoke(getId, sfItem);
            return id == null || id.isEmpty() ? null : id;
        } catch (Throwable t) {
            DebugLog.verbose("Slimefun identify failed: " + t);
            return null;
        }
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length != 1 || idParts[0].isEmpty()) {
            return null;
        }
        try {
            Object sfItem = HookReflection.invoke(getById, null, idParts[0]);
            if (sfItem == null) {
                return null;
            }
            Object item = HookReflection.invoke(getItem, sfItem);
            return item instanceof ItemStack ? ((ItemStack) item).clone() : null;
        } catch (Throwable t) {
            DebugLog.verbose("Slimefun build failed: " + t);
            return null;
        }
    }
}
