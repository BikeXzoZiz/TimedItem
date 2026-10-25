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
 *
 * Every output carries a format tag ({@link #FORMAT_NBT} or
 * {@link #FORMAT_LEGACY}) so the recovery log can record which encoder
 * wrote it and migrate entries after a Minecraft upgrade.
 */
public final class ItemSerializer {

    /** ItemStack.serializeAsBytes() encoding (1.15+). */
    public static final String FORMAT_NBT = "nbt";
    /** BukkitObjectStream encoding (1.13/1.14 fallback). */
    public static final String FORMAT_LEGACY = "bos";

    private static Method serializeAsBytes;
    private static Method deserializeBytes;
    private static boolean reflectionChecked;

    private ItemSerializer() {
    }

    /** Serialized item plus the encoder that produced it. */
    public static final class Encoded {
        public final String base64;
        public final String format;

        Encoded(String base64, String format) {
            this.base64 = base64;
            this.format = format;
        }
    }

    /** Serializes an item with its format tag, or null when it cannot be encoded. */
    public static Encoded encode(ItemStack item) {
        if (item == null) {
            return null;
        }
        if (useModernApi()) {
            try {
                return new Encoded(Base64.getEncoder()
                        .encodeToString((byte[]) serializeAsBytes.invoke(item)), FORMAT_NBT);
            } catch (Exception ignored) {
                // fall through to the legacy path
            }
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes);
            out.writeObject(item);
            out.close();
            return new Encoded(Base64.getEncoder().encodeToString(bytes.toByteArray()), FORMAT_LEGACY);
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the Base64 form of the item, or null when serialization fails. */
    public static String toBase64(ItemStack item) {
        Encoded encoded = encode(item);
        return encoded == null ? null : encoded.base64;
    }

    /** Format tag describing what {@link #toBase64} produces on this server. */
    public static String currentFormat() {
        return useModernApi() ? FORMAT_NBT : FORMAT_LEGACY;
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
