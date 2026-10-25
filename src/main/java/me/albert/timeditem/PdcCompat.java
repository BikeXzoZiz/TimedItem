package me.albert.timeditem;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * PersistentDataContainer access for 1.14+ servers through reflection
 * (the compile target is 1.13, where the API does not exist yet). On
 * 1.13 every method silently no-ops and TimeLore falls back to lore.
 *
 * Stores two keys on the item: "type" (expire|lifetime|paused) and
 * "value" (epoch millis for expire, seconds otherwise).
 */
public final class PdcCompat {

    private static boolean attempted;
    private static boolean available;
    private static NamespacedKey typeKey;
    private static NamespacedKey valueKey;
    private static Object stringType;
    private static Object longType;
    private static Method getPdcMethod;
    private static Class<?> dataTypeClass;

    private PdcCompat() {
    }

    public static void init(Plugin plugin) {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            dataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            stringType = dataTypeClass.getField("STRING").get(null);
            longType = dataTypeClass.getField("LONG").get(null);
            typeKey = new NamespacedKey(plugin, "type");
            valueKey = new NamespacedKey(plugin, "value");
            getPdcMethod = ItemMeta.class.getMethod("getPersistentDataContainer");
            available = true;
        } catch (Throwable t) {
            available = false; // 1.13 server: lore stays the only storage
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void write(ItemStack item, String type, long value) {
        if (!available) {
            return;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return;
            }
            Object container = getPdcMethod.invoke(meta);
            Method set = container.getClass().getMethod("set", NamespacedKey.class, dataTypeClass, Object.class);
            set.invoke(container, typeKey, stringType, type);
            set.invoke(container, valueKey, longType, value);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
            // keep lore as the storage of record
        }
    }

    /** Returns {"expire"|"lifetime"|"paused", value-as-string} or null. */
    public static String[] read(ItemStack item) {
        if (!available) {
            return null;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            }
            Object container = getPdcMethod.invoke(meta);
            Method get = container.getClass().getMethod("get", NamespacedKey.class, dataTypeClass);
            String type = (String) get.invoke(container, typeKey, stringType);
            if (type == null) {
                return null;
            }
            Long value = (Long) get.invoke(container, valueKey, longType);
            if (value == null) {
                return null;
            }
            return new String[] {type, value.toString()};
        } catch (Throwable t) {
            return null;
        }
    }

    public static void clear(ItemStack item) {
        if (!available) {
            return;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return;
            }
            Object container = getPdcMethod.invoke(meta);
            Method remove = container.getClass().getMethod("remove", NamespacedKey.class);
            remove.invoke(container, typeKey);
            remove.invoke(container, valueKey);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
            // nothing stored, nothing to do
        }
    }
}
