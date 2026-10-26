package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import java.time.LocalDateTime;

/**
 * Shulker box items hold items inside their block state (3.1+): the
 * container scans, /ti find and the anti-hide check treat them as
 * containers one level deep. Uses the clean BlockStateMeta API (1.13+)
 * - getBlockState() hands out a live ShulkerBox whose inventory is
 * readable and writable, and setBlockState() writes the contents back
 * into the outer item. Vanilla shulkers cannot nest, so one level is
 * complete coverage; entry is gated on the material name so ordinary
 * items never pay for the check.
 */
public final class Shulkers {

    private Shulkers() {
    }

    /** True when the stack is a shulker box item (any color). */
    public static boolean isShulker(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return item.getType().name().endsWith("SHULKER_BOX");
    }

    /**
     * Visits every item inside a shulker box item. The consumer may
     * mutate the inner inventory freely; call {@link #writeBack} with
     * the same outer stack afterwards to persist the changes - without
     * it the mutations live only in the temporary block state copy.
     */
    public static void forEachInnerItem(ItemStack outer, java.util.function.BiConsumer<Integer, ItemStack> consumer) {
        Inventory inner = innerInventory(outer);
        if (inner == null) {
            return;
        }
        for (int slot = 0; slot < inner.getSize(); slot++) {
            ItemStack item = inner.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            consumer.accept(slot, item);
        }
    }

    /**
     * The temporary ShulkerBox state of a shulker box item, or null.
     * Mutations on it persist only after {@link #writeBack}.
     */
    public static Inventory innerInventory(ItemStack outer) {
        if (!isShulker(outer)) {
            return null;
        }
        ItemMeta meta = outer.getItemMeta();
        if (!(meta instanceof BlockStateMeta)) {
            return null;
        }
        org.bukkit.block.BlockState state = ((BlockStateMeta) meta).getBlockState();
        if (!(state instanceof ShulkerBox)) {
            return null;
        }
        return ((ShulkerBox) state).getInventory();
    }

    /**
     * Persists changes made to {@link #innerInventory}'s inventory into
     * the outer shulker item. Must run after mutating inner slots.
     */
    public static void writeBack(ItemStack outer) {
        if (!isShulker(outer)) {
            return;
        }
        ItemMeta meta = outer.getItemMeta();
        if (!(meta instanceof BlockStateMeta)) {
            return;
        }
        org.bukkit.block.BlockState state = ((BlockStateMeta) meta).getBlockState();
        if (!(state instanceof ShulkerBox)) {
            return;
        }
        ((BlockStateMeta) meta).setBlockState(state);
        outer.setItemMeta(meta);
    }

    /**
     * True when the stack is a shulker whose contents hold an EXPIRE
     * timer with less than the threshold left - used by anti-hide, so a
     * shulker of expiring items cannot bypass the container block.
     */
    public static boolean containsBelowThreshold(ItemStack outer, PluginConfig config, long threshold) {
        if (!isShulker(outer)) {
            return false;
        }
        LocalDateTime now = java.time.LocalDateTime.now();
        boolean[] found = {false};
        forEachInnerItem(outer, (slot, inner) -> {
            if (found[0]) {
                return;
            }
            TimeLore.TimerInfo info = TimeLore.analyze(inner, config);
            if (info == null || info.getType() != TimeLore.Type.EXPIRE || info.getExpiry() == null) {
                return;
            }
            if (java.time.Duration.between(now, info.getExpiry()).getSeconds() <= threshold) {
                found[0] = true;
            }
        });
        return found[0];
    }
}
