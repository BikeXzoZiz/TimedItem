package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

/**
 * ItemsAdder (LoneDev) custom items resolve through the CustomStack API:
 * getInstance(ItemStack) returns null for non-IA stacks and one instance
 * per registered "namespace:id".
 */
final class ItemsAdderAdapter implements CustomItemAdapter {

    private final Class<?> customStack;
    private final Method fromStack;
    private final Method fromId;
    private final Method getItemStack;
    private final Method getId;
    private final Method getNamespace;

    ItemsAdderAdapter() {
        this.customStack = HookReflection.findClass("dev.lone.itemsadder.api.CustomStack");
        this.fromStack = customStack == null ? null
                : HookReflection.findMethod(customStack, "getInstance", ItemStack.class);
        this.fromId = customStack == null ? null
                : HookReflection.findMethod(customStack, "getInstance", String.class);
        this.getItemStack = customStack == null ? null
                : HookReflection.findMethod(customStack, "getItemStack");
        // resolved once here instead of scanning getMethods() on every
        // identify - CustomStack.getInstance always hands out the same class
        this.getId = customStack == null ? null
                : HookReflection.findAnyMethod(customStack, "getId", 0);
        this.getNamespace = customStack == null ? null
                : HookReflection.findAnyMethod(customStack, "getNamespace", 0);
    }

    @Override
    public String id() {
        return "itemsadder";
    }

    @Override
    public List<String> aliases() {
        return java.util.Collections.singletonList("ia");
    }

    @Override
    public boolean isAvailable() {
        return customStack != null && fromStack != null && fromId != null && getItemStack != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            Object stack = HookReflection.invoke(fromStack, null, item);
            if (stack == null) {
                return null;
            }
            // getId() returns "namespace:id" on current versions; older
            // builds only carry the bare id next to getNamespace()
            String id = getId == null ? null : (String) HookReflection.invoke(getId, stack);
            if (id != null && id.contains(":")) {
                return id;
            }
            String namespace = getNamespace == null ? null : (String) HookReflection.invoke(getNamespace, stack);
            return namespace == null || namespace.isEmpty() ? id : namespace + ":" + id;
        } catch (Throwable t) {
            DebugLog.verbose("ItemsAdder identify failed: " + t);
            return null;
        }
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length == 0 || idParts[0].isEmpty()) {
            return null;
        }
        try {
            Object stack = HookReflection.invoke(fromId, null, String.join(":", idParts));
            if (stack == null) {
                return null;
            }
            Object item = HookReflection.invoke(getItemStack, stack);
            return item instanceof ItemStack ? ((ItemStack) item).clone() : null;
        } catch (Throwable t) {
            DebugLog.verbose("ItemsAdder build failed: " + t);
            return null;
        }
    }
}
