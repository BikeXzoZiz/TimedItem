package io.th0rgal.oraxen.api;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake Oraxen static API (both the 1.x and 2.x package locations expose
 * this surface). Items are recognized by the marker display name
 * "Fake Oraxen <id>".
 */
public class OraxenItems {

    private static final String PREFIX = "Fake Oraxen ";
    private static final Map<String, ItemStack> REGISTRY = new HashMap<String, ItemStack>();

    public static void registerDefaults() {
        REGISTRY.put("ruby_sword", make(Material.DIAMOND_SWORD, "ruby_sword"));
    }

    private static ItemStack make(Material material, String id) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + PREFIX + id);
        stack.setItemMeta(meta);
        return stack;
    }

    public static String getIdByItem(ItemStack item) {
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

    public static OraxenItem getItemById(String id) {
        return REGISTRY.containsKey(id) ? new OraxenItem(REGISTRY.get(id)) : null;
    }

    public static class OraxenItem {
        private final ItemStack stack;

        OraxenItem(ItemStack stack) {
            this.stack = stack;
        }

        public ItemStack build() {
            return stack.clone();
        }
    }
}
