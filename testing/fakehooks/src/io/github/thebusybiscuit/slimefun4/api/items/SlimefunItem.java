package io.github.thebusybiscuit.slimefun4.api.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake SlimefunItem registry (the modern class location). Items carry
 * the marker display name "Fake Slimefun <id>".
 */
public class SlimefunItem {

    private static final String PREFIX = "Fake Slimefun ";
    private static final Map<String, ItemStack> REGISTRY = new HashMap<String, ItemStack>();

    public static void registerDefaults() {
        REGISTRY.put("test_item", make(Material.GOLDEN_PICKAXE, "test_item"));
    }

    private static ItemStack make(Material material, String id) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + PREFIX + id);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Null for non-Slimefun stacks. */
    public static SlimefunItem getByItem(ItemStack item) {
        String id = idOf(item);
        return id == null ? null : getById(id);
    }

    /** Null for unknown ids. */
    public static SlimefunItem getById(String id) {
        return REGISTRY.containsKey(id) ? new SlimefunItem(id) : null;
    }

    private final String id;

    private SlimefunItem(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
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
