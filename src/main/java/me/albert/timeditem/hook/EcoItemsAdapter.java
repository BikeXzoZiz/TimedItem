package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;

/**
 * EcoItems (Auxilor) registry access. The registry is a Kotlin object
 * (EcoItems.INSTANCE); building by id is straightforward, but the
 * plugin does not stamp a documented id tag on its stacks, so
 * identifying an ItemStack walks the registry and compares material and
 * display name (the timer lore TimedItem adds would break isSimilar).
 */
final class EcoItemsAdapter implements CustomItemAdapter {

    private final Object registry;
    private final Method getById;
    private final Method values;

    // resolved lazily on the first walked entry - the EcoItem impl class
    // is only known once the registry is iterated, and identifying walks
    // every entry, so a per-entry getMethods() scan multiplies fast
    private volatile Method itemGetter;
    private volatile Method idGetter;

    EcoItemsAdapter() {
        Class<?> registryClass = HookReflection.findClass("com.willfp.ecoitems.items.EcoItems");
        Object registry = registryClass == null ? null
                : HookReflection.staticField(registryClass, "INSTANCE");
        if (registry == null && registryClass != null) {
            // some builds only expose the Kotlin Companion
            Object companion = HookReflection.staticField(registryClass, "Companion");
            if (companion != null) {
                Method byId = HookReflection.findMethod(companion.getClass(), "getByID", String.class);
                if (byId != null) {
                    registry = companion;
                }
            }
        }
        this.registry = registry;
        this.getById = registry == null ? null
                : HookReflection.findMethod(registry.getClass(), "getByID", String.class);
        this.values = registry == null ? null
                : HookReflection.findCollectionGetter(registry.getClass(), "value");
    }

    @Override
    public String id() {
        return "ecoitems";
    }

    @Override
    public boolean isAvailable() {
        return registry != null && getById != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (item == null || values == null) {
            return null;
        }
        try {
            Object all = HookReflection.invoke(values, registry);
            if (!(all instanceof Iterable)) {
                return null;
            }
            String name = displayName(item);
            for (Object entry : (Iterable<?>) all) {
                ItemStack stack;
                if (entry instanceof ItemStack) {
                    stack = (ItemStack) entry;
                } else {
                    Method getItem = cachedItemGetter(entry);
                    Object got = getItem == null ? null : HookReflection.invoke(getItem, entry);
                    stack = got instanceof ItemStack ? (ItemStack) got : null;
                }
                if (stack == null) {
                    continue;
                }
                if (stack.getType() == item.getType()
                        && name != null && name.equals(displayName(stack))) {
                    Method getId = cachedIdGetter(entry);
                    String found = getId == null ? null : (String) HookReflection.invoke(getId, entry);
                    if (found != null && !found.isEmpty()) {
                        return found;
                    }
                }
            }
            return null;
        } catch (Throwable t) {
            DebugLog.verbose("EcoItems identify failed: " + t);
            return null;
        }
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length != 1 || idParts[0].isEmpty()) {
            return null;
        }
        try {
            Object ecoItem = HookReflection.invoke(getById, registry, idParts[0]);
            if (ecoItem == null) {
                return null;
            }
            Method getItem = cachedItemGetter(ecoItem);
            Object stack = getItem == null ? null : HookReflection.invoke(getItem, ecoItem);
            return stack instanceof ItemStack ? ((ItemStack) stack).clone() : null;
        } catch (Throwable t) {
            DebugLog.verbose("EcoItems build failed: " + t);
            return null;
        }
    }

    /** Cached EcoItem#item getter - the impl class is stable across entries. */
    private Method cachedItemGetter(Object entry) {
        Method cached = itemGetter;
        if (cached == null) {
            cached = HookReflection.findGetter(entry.getClass(), "item", ItemStack.class);
            if (cached != null) {
                itemGetter = cached;
            }
        }
        return cached;
    }

    /** Cached EcoItem#id getter, same lazy pattern as {@link #cachedItemGetter(Object)}. */
    private Method cachedIdGetter(Object entry) {
        Method cached = idGetter;
        if (cached == null) {
            cached = HookReflection.findGetter(entry.getClass(), "id", String.class);
            if (cached != null) {
                idGetter = cached;
            }
        }
        return cached;
    }

    /** Stripped display name, or null when the stack has none. */
    private static String displayName(ItemStack item) {
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        return ChatColor.stripColor(meta.getDisplayName());
    }
}
