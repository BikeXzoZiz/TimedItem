package com.nexomc.nexo.api;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/** Fake Nexo static API; items carry the marker display name "Fake Nexo <id>". */
public class NexoItems {

    private static final String PREFIX = "Fake Nexo ";
    private static final Map<String, ItemStack> REGISTRY = new HashMap<String, ItemStack>();

    public static void registerDefaults() {
        REGISTRY.put("ruby_sword", make(Material.DIAMOND_SWORD, "ruby_sword"));
    }

    private static ItemStack make(Material material, String id) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + PREFIX + id);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Null for non-Nexo stacks. */
    public static String idFromItem(ItemStack item) {
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

    /** Null for unknown ids. */
    public static ItemStack itemFromId(String id) {
        ItemStack stack = REGISTRY.get(id);
        return stack == null ? null : stack.clone();
    }
}
