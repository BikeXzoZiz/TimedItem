package me.albert.timeditem;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Base64;

/**
 * ItemStack <-> Base64. Prefers ItemStack.serializeAsBytes() (1.15+,
 * preserves everything including the PDC); falls back to Bukkit's object
 * streams on 1.13/1.14, which keep the lore - the TimedItem timer stays
 * readable through the lore fallback there.
 */
public final class ItemSerializer {

    private static Method serializeAsBytes;
    private static Method deserializeBytes;
    private static boolean reflectionChecked;

    private ItemSerializer() {
    }

    /** Returns the Base64 form of the item, or null when serialization fails. */
    public static String toBase64(ItemStack item) {
        if (item == null) {
            return null;
        }
        if (useModernApi()) {
            try {
                return Base64.getEncoder().encodeToString((byte[]) serializeAsBytes.invoke(item));
            } catch (Exception ignored) {
                // fall through to the legacy path
            }
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes);
            out.writeObject(item);
            out.close();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /** Rebuilds an item from {@link #toBase64} output, or null when broken. */
    public static ItemStack fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        if (deserializeBytes != null) {
            try {
                return (ItemStack) deserializeBytes.invoke(null, Base64.getDecoder().decode(base64));
            } catch (Exception ignored) {
                // fall through to the legacy path
            }
        }
        try {
            BukkitObjectInputStream in = new BukkitObjectInputStream(
                    new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            ItemStack item = (ItemStack) in.readObject();
            in.close();
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean useModernApi() {
        if (!reflectionChecked) {
            reflectionChecked = true;
            try {
                serializeAsBytes = ItemStack.class.getMethod("serializeAsBytes");
                deserializeBytes = ItemStack.class.getMethod("deserializeBytes", byte[].class);
            } catch (NoSuchMethodException e) {
                serializeAsBytes = null;
                deserializeBytes = null;
            }
        }
        return serializeAsBytes != null;
    }
}
