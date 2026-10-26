package com.ssomar.executableitems.executableitems;

import com.ssomar.executableitems.executableitems.manager.ExecutableItemsManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake ExecutableItems API. Items carry the marker display name
 * "Fake EI <id>"; lookups return Optionals like current versions.
 */
public class ExecutableItemsAPI {

    private static final String PREFIX = "Fake EI ";
    private static final Map<String, ItemStack> REGISTRY = new HashMap<String, ItemStack>();
    private static ExecutableItemsManager manager;

    public static void registerDefaults() {
        REGISTRY.put("gkit", make(Material.DIAMOND_CHESTPLATE, "gkit"));
        manager = new ExecutableItemsManager();
    }

    private static ItemStack make(Material material, String id) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + PREFIX + id);
        stack.setItemMeta(meta);
        return stack;
    }

    public static ExecutableItemsManager getExecutableItemsManager() {
        return manager;
    }

    public static String idOf(ItemStack item) {
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

    public static ItemStack itemOf(String id) {
        ItemStack stack = REGISTRY.get(id);
        return stack == null ? null : stack.clone();
    }

    public static class ExecutableItem {
        private final String id;

        public ExecutableItem(String id) {
            this.id = id;
        }

        public String getID() {
            return id;
        }

        public ItemStack buildItem() {
            return itemOf(id);
        }
    }
}
