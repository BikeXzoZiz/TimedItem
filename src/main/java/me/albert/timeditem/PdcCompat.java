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
 * Stores four keys on the item: "type" (expire|lifetime|paused),
 * "value" (epoch millis for expire, seconds otherwise), "uid" (a
 * random UUID assigned with the first timer write, so audit entries,
 * /ti find and cross-server tracking can name an individual item) and
 * "transform" (the expiry transformation profile key chosen by admins,
 * 2.8+). All methods are resolved once in init - this is the hottest
 * path in the plugin (every click, scan and actionbar tick), so
 * per-call lookups are out.
 */
public final class PdcCompat {

    private static volatile boolean attempted;
    private static volatile boolean available;
    private static NamespacedKey typeKey;
    private static NamespacedKey valueKey;
    private static NamespacedKey uidKey;
    private static NamespacedKey transformKey;
    private static Object stringType;
    private static Object longType;
    private static volatile Method getPdcMethod;
    private static volatile Method setMethod;
    private static volatile Method getMethod;
    private static volatile Method removeMethod;

    private PdcCompat() {
    }

    public static void init(Plugin plugin) {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            Class<?> dataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            stringType = dataTypeClass.getField("STRING").get(null);
            longType = dataTypeClass.getField("LONG").get(null);
            typeKey = new NamespacedKey(plugin, "type");
            valueKey = new NamespacedKey(plugin, "value");
            uidKey = new NamespacedKey(plugin, "uid");
            transformKey = new NamespacedKey(plugin, "transform");
            getPdcMethod = ItemMeta.class.getMethod("getPersistentDataContainer");
            Class<?> containerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");
            setMethod = containerClass.getMethod("set", NamespacedKey.class, dataTypeClass, Object.class);
            getMethod = containerClass.getMethod("get", NamespacedKey.class, dataTypeClass);
            removeMethod = containerClass.getMethod("remove", NamespacedKey.class);
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
            setMethod.invoke(container, typeKey, stringType, type);
            setMethod.invoke(container, valueKey, longType, value);
            if (getMethod.invoke(container, uidKey, stringType) == null) {
                setMethod.invoke(container, uidKey, stringType, java.util.UUID.randomUUID().toString());
            }
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
            DebugLog.verbose("PDC write failed (lore stays the storage of record): " + ignored);
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
            String type = (String) getMethod.invoke(container, typeKey, stringType);
            if (type == null) {
                return null;
            }
            Long value = (Long) getMethod.invoke(container, valueKey, longType);
            if (value == null) {
                return null;
            }
            return new String[] {type, value.toString()};
        } catch (Throwable t) {
            DebugLog.verbose("PDC read failed: " + t);
            return null;
        }
    }

    /** The item's TimedItem uid, or null when it has none (1.13 or lore-only items). */
    public static String readUid(ItemStack item) {
        if (!available) {
            return null;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            }
            Object container = getPdcMethod.invoke(meta);
            return (String) getMethod.invoke(container, uidKey, stringType);
        } catch (Throwable t) {
            DebugLog.verbose("PDC uid read failed: " + t);
            return null;
        }
    }

    /** Tags the item with an expiry transformation profile key (2.8+). */
    public static void writeTransform(ItemStack item, String profileKey) {
        if (!available || profileKey == null || profileKey.isEmpty()) {
            return;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return;
            }
            Object container = getPdcMethod.invoke(meta);
            setMethod.invoke(container, transformKey, stringType, profileKey);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
            DebugLog.verbose("PDC transform write failed: " + ignored);
        }
    }

    /** The expiry transformation profile key of the item, or null. */
    public static String readTransform(ItemStack item) {
        if (!available) {
            return null;
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            }
            Object container = getPdcMethod.invoke(meta);
            return (String) getMethod.invoke(container, transformKey, stringType);
        } catch (Throwable t) {
            DebugLog.verbose("PDC transform read failed: " + t);
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
            removeMethod.invoke(container, typeKey);
            removeMethod.invoke(container, valueKey);
            removeMethod.invoke(container, uidKey);
            removeMethod.invoke(container, transformKey);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
            DebugLog.verbose("PDC clear failed: " + ignored);
        }
    }
}
