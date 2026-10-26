package com.willfp.ecoitems.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake EcoItems registry (Kotlin-object shape: static INSTANCE with
 * getByID/getValues). Items carry the marker display name
 * "Fake Eco <id>"; identification compares material + stripped name.
 */
public class EcoItems {

    public static EcoItems INSTANCE;
    private static final String PREFIX = "Fake Eco ";
    private final Map<String, ItemStack> registry = new LinkedHashMap<String, ItemStack>();

    public static void registerDefaults() {
        EcoItems instance = new EcoItems();
        instance.registry.put("test_sword", make(Material.DIAMOND_SWORD, "test_sword"));
        INSTANCE = instance;
    }

    private static ItemStack make(Material material, String id) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + PREFIX + id);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Null for unknown ids. */
    public EcoItem getByID(String id) {
        ItemStack stack = registry.get(id);
        return stack == null ? null : new EcoItem(id, stack);
    }

    /** The registry walk used for identification. */
    public List<EcoItem> getValues() {
        List<EcoItem> out = new ArrayList<EcoItem>();
        for (Map.Entry<String, ItemStack> entry : registry.entrySet()) {
            out.add(new EcoItem(entry.getKey(), entry.getValue()));
        }
        return out;
    }
}
