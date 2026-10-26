package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * ExecutableItems (Ssomar) resolves through its manager API:
 * ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(...)
 * returns an Optional on current versions and the plain object on older
 * ones - both shapes are unwrapped here.
 */
final class ExecutableItemsAdapter implements CustomItemAdapter {

    private final Class<?> api;
    private final Method getManager;
    private final Method fromStack;
    private final Method fromId;

    ExecutableItemsAdapter() {
        this.api = HookReflection.findClass(
                "com.ssomar.executableitems.executableitems.ExecutableItemsAPI",
                "com.ssomar.executableitems.api.ExecutableItemsAPI");
        if (api == null) {
            this.getManager = null;
            this.fromStack = null;
            this.fromId = null;
            return;
        }
        this.getManager = HookReflection.findMethod(api, "getExecutableItemsManager");
        if (getManager == null) {
            this.fromStack = null;
            this.fromId = null;
            return;
        }
        Class<?> managerClass = getManager.getReturnType();
        // the manager class may not be resolvable from the API class alone
        if (managerClass == Object.class || managerClass.isPrimitive()) {
            managerClass = HookReflection.findClass(
                    "com.ssomar.executableitems.executableitems.manager.ExecutableItemsManager",
                    "com.ssomar.executableitems.executableitems.manager.LoaderManager");
        }
        // getExecutableItem is overloaded: (ItemStack) to identify and
        // (String id) to look up - pick each overload by its parameter
        this.fromStack = managerClass == null ? null
                : HookReflection.findOverload(managerClass, "getExecutableItem", ItemStack.class);
        this.fromId = managerClass == null ? null
                : HookReflection.findOverload(managerClass, "getExecutableItem", String.class);
    }

    @Override
    public String id() {
        return "executableitems";
    }

    @Override
    public java.util.List<String> aliases() {
        return java.util.Collections.singletonList("ei");
    }

    @Override
    public boolean isAvailable() {
        if (api == null || getManager == null || fromStack == null || fromId == null) {
            return false;
        }
        return manager() != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            Object executableItem = resolve(HookReflection.invoke(fromStack, manager(), item));
            if (executableItem == null) {
                return null;
            }
            Method getId = HookReflection.findAnyMethod(executableItem.getClass(), "getID", 0);
            if (getId == null) {
                getId = HookReflection.findAnyMethod(executableItem.getClass(), "getId", 0);
            }
            String id = getId == null ? null : (String) HookReflection.invoke(getId, executableItem);
            return id == null || id.isEmpty() ? null : id;
        } catch (Throwable t) {
            DebugLog.verbose("ExecutableItems identify failed: " + t);
            return null;
        }
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length != 1 || idParts[0].isEmpty()) {
            return null;
        }
        try {
            Object executableItem = resolve(HookReflection.invoke(fromId, manager(), idParts[0]));
            if (executableItem == null) {
                return null;
            }
            // buildItem() on some versions, buildItem(int amount) on others
            Method build = HookReflection.findAnyMethod(executableItem.getClass(), "buildItem", 0);
            Object stack = build == null ? null : HookReflection.invoke(build, executableItem);
            if (stack == null) {
                build = HookReflection.findAnyMethod(executableItem.getClass(), "buildItem", 1);
                stack = build == null ? null : HookReflection.invoke(build, executableItem, 1);
            }
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (Throwable t) {
            DebugLog.verbose("ExecutableItems build failed: " + t);
            return null;
        }
    }

    private Object manager() {
        return HookReflection.invoke(getManager, null);
    }

    /** Unwraps the Optional some versions wrap their result in. */
    private static Object resolve(Object result) {
        if (result instanceof java.util.Optional) {
            return ((java.util.Optional<?>) result).orElse(null);
        }
        return result;
    }
}
