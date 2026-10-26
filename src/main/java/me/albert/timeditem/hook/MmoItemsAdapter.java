package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * MMOItems (PhoenixDevT) items are identified through MythicLib's NBT
 * wrapper: every MMOItem carries the MMOITEMS_ITEM_TYPE / MMOITEMS_ITEM_ID
 * tags. Building uses the template system of MI 6.7+ and falls back to
 * the older ItemManager.getItem chain.
 */
final class MmoItemsAdapter implements CustomItemAdapter {

    private static final String TYPE_TAG = "MMOITEMS_ITEM_TYPE";
    private static final String ID_TAG = "MMOITEMS_ITEM_ID";

    private final Class<?> pluginClass;
    private final Method nbtGet;
    private final Method nbtHasTag;
    private final Method nbtGetString;

    // resolved lazily on the first build() - probing both the 6.7+
    // template chain and the pre-6.7 item chain up front would fail
    // half of them on every version
    private volatile Method typeLookup;
    private volatile Method templateLookup;

    MmoItemsAdapter() {
        this.pluginClass = HookReflection.findClass("net.Indyuce.mmoitems.MMOItems");
        Class<?> nbtClass = HookReflection.findClass("io.lumine.mythic.lib.api.item.NBTItem");
        this.nbtGet = nbtClass == null ? null : HookReflection.findMethod(nbtClass, "get", ItemStack.class);
        this.nbtHasTag = nbtClass == null ? null : HookReflection.findMethod(nbtClass, "hasTag", String.class);
        this.nbtGetString = nbtClass == null ? null : HookReflection.findMethod(nbtClass, "getString", String.class);
    }

    @Override
    public String id() {
        return "mmoitems";
    }

    @Override
    public boolean isAvailable() {
        if (pluginClass == null || nbtGet == null || nbtHasTag == null || nbtGetString == null) {
            return false;
        }
        return pluginInstance() != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            Object nbt = HookReflection.invoke(nbtGet, null, item);
            if (nbt == null) {
                return null;
            }
            Object hasType = HookReflection.invoke(nbtHasTag, nbt, TYPE_TAG);
            if (!Boolean.TRUE.equals(hasType)) {
                return null;
            }
            String type = (String) HookReflection.invoke(nbtGetString, nbt, TYPE_TAG);
            if (type == null || type.isEmpty()) {
                return null;
            }
            String id = null;
            if (Boolean.TRUE.equals(HookReflection.invoke(nbtHasTag, nbt, ID_TAG))) {
                id = (String) HookReflection.invoke(nbtGetString, nbt, ID_TAG);
            }
            return id == null || id.isEmpty() ? type : type + ":" + id;
        } catch (Throwable t) {
            DebugLog.verbose("MMOItems identify failed: " + t);
            return null;
        }
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length < 2) {
            return null; // an MMOItem needs TYPE and ID, e.g. SWORD:CUTLASS
        }
        try {
            Object type = lookupType(idParts[0]);
            if (type == null) {
                return null;
            }
            Object builder = templateBuilder(type, idParts[1]);
            if (builder == null) {
                builder = legacyBuilder(type, idParts[1]);
            }
            if (builder == null) {
                return null;
            }
            Method build = HookReflection.findAnyMethod(builder.getClass(), "build", 0);
            Object stack = build == null ? null : HookReflection.invoke(build, builder);
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (Throwable t) {
            DebugLog.verbose("MMOItems build failed: " + t);
            return null;
        }
    }

    private Object pluginInstance() {
        return HookReflection.staticField(pluginClass, "plugin");
    }

    private Object lookupType(String typeId) {
        Object plugin = pluginInstance();
        if (plugin == null) {
            return null;
        }
        Method getTypes = HookReflection.findMethod(pluginClass, "getTypes");
        Object types = getTypes == null ? null : HookReflection.invoke(getTypes, plugin);
        if (types == null) {
            return null;
        }
        if (typeLookup == null) {
            // Type ids are written uppercase in configs (SWORD, ARMOR, ...)
            typeLookup = HookReflection.findMethod(types.getClass(), "get", String.class);
        }
        return typeLookup == null
                ? null : HookReflection.invoke(typeLookup, types, typeId.toUpperCase(java.util.Locale.ROOT));
    }

    /** MI 6.7+ path: TemplateManager.getTemplate(type, id).newBuilder(...). */
    private Object templateBuilder(Object type, String id) {
        Object plugin = pluginInstance();
        if (plugin == null) {
            return null;
        }
        Method getTemplates = HookReflection.findMethod(pluginClass, "getTemplates");
        Object templates = getTemplates == null ? null : HookReflection.invoke(getTemplates, plugin);
        if (templates == null) {
            return null;
        }
        if (templateLookup == null) {
            Class<?> typeClass = type.getClass();
            templateLookup = HookReflection.findMethod(templates.getClass(), "getTemplate", typeClass, String.class);
        }
        Object template = templateLookup == null
                ? null : HookReflection.invoke(templateLookup, templates, type, id);
        if (template == null) {
            return null;
        }
        // newBuilder(ItemModifier...) - zero varargs or a single option param
        Method newBuilder = HookReflection.findAnyMethod(template.getClass(), "newBuilder", 0);
        if (newBuilder != null) {
            return HookReflection.invoke(newBuilder, template);
        }
        newBuilder = HookReflection.findAnyMethod(template.getClass(), "newBuilder", 1);
        return newBuilder == null ? null : HookReflection.invoke(newBuilder, template, new Object[0]);
    }

    /** Pre-6.7 path: ItemManager.getItem(type, id).newBuilder(). */
    private Object legacyBuilder(Object type, String id) {
        Object plugin = pluginInstance();
        if (plugin == null) {
            return null;
        }
        Method getItems = HookReflection.findMethod(pluginClass, "getItems");
        Object items = getItems == null ? null : HookReflection.invoke(getItems, plugin);
        if (items == null) {
            return null;
        }
        Method getItem = HookReflection.findMethod(items.getClass(), "getItem", type.getClass(), String.class);
        Object mmItem = getItem == null ? null : HookReflection.invoke(getItem, items, type, id);
        if (mmItem == null) {
            return null;
        }
        Method newBuilder = HookReflection.findAnyMethod(mmItem.getClass(), "newBuilder", 0);
        return newBuilder == null ? null : HookReflection.invoke(newBuilder, mmItem);
    }
}
