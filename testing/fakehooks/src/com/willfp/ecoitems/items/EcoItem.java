package com.willfp.ecoitems.items;

import org.bukkit.inventory.ItemStack;

/** One EcoItems entry: an id plus its template stack. */
public class EcoItem {

    private final String id;
    private final ItemStack stack;

    public EcoItem(String id, ItemStack stack) {
        this.id = id;
        this.stack = stack;
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
        return stack.clone();
    }
}
