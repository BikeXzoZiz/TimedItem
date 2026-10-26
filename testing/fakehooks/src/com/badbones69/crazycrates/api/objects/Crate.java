package com.badbones69.crazycrates.api.objects;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/** Fake crate; the adapter finds getPrize via its "Prize" return type. */
public class Crate {

    private final String name;
    private final Map<String, ItemStack> prizes = new HashMap<String, ItemStack>();

    public Crate(String name) {
        this.name = name;
        ItemStack prize = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = prize.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Fake Prize");
        prize.setItemMeta(meta);
        prizes.put("prize", prize);
    }

    public String getName() {
        return name;
    }

    /** Null for unknown prize ids. */
    public Prize getPrize(String id) {
        ItemStack stack = prizes.get(id);
        return stack == null ? null : new Prize(stack);
    }

    public static class Prize {
        private final ItemStack stack;

        Prize(ItemStack stack) {
            this.stack = stack;
        }

        public ItemStack getItemStack() {
            return stack.clone();
        }
    }
}
