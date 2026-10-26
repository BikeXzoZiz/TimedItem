package dev.lone.itemsadder.api;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake CustomStack API. Stacks are recognized by the marker display
 * name "Fake IA <id>"; the registry is filled by the plugin main.
 */
public class CustomStack {

    private static final String PREFIX = "Fake IA ";
    private static final Map<String, ItemStack> REGISTRY = new HashMap<String, ItemStack>();

    /** Called by the fake plugin main on enable. */
    public static void registerDefaults() {
        REGISTRY.put("iasurvival:ruby_sword", make(Material.DIAMOND_SWORD, "iasurvival:ruby_sword"));
    }

    private static ItemStack make(Material material, String id) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.RED + PREFIX + id);
        stack.setItemMeta(meta);
        return stack;
    }

    private final String id;

    private CustomStack(String id) {
        this.id = id;
    }

    /** Null for non-ItemsAdder stacks. */
    public static CustomStack getInstance(ItemStack item) {
        String id = idOf(item);
        return id == null ? null : new CustomStack(id);
    }

    /** Null for unknown ids. */
    public static CustomStack getInstance(String id) {
        return REGISTRY.containsKey(id) ? new CustomStack(id) : null;
    }

    public String getId() {
        return id; // "namespace:id" shape
    }

    public String getNamespace() {
        int split = id.indexOf(':');
        return split <= 0 ? "" : id.substring(0, split);
    }

    public ItemStack getItemStack() {
        return REGISTRY.get(id).clone();
    }

    private static String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        String name = ChatColor.stripColor(meta.getDisplayName());
        return name.startsWith(PREFIX) ? name.substring(PREFIX.length()) : null;
    }
}
