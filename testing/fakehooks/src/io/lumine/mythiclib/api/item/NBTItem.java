package io.lumine.mythiclib.api.item;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Fake MythicLib NBT wrapper. MMOItems tags are read from the marker
 * display name ("Fake MMO <TYPE> <ID>") the fake stamps on its items.
 */
public class NBTItem {

    private static final String PREFIX = "Fake MMO ";
    private final ItemStack item;

    private NBTItem(ItemStack item) {
        this.item = item;
    }

    public static NBTItem get(ItemStack item) {
        return new NBTItem(item);
    }

    public boolean hasTag(String tag) {
        return readTag(tag) != null;
    }

    public String getString(String tag) {
        return readTag(tag);
    }

    private String readTag(String tag) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        String name = ChatColor.stripColor(meta.getDisplayName());
        if (!name.startsWith(PREFIX)) {
            return null;
        }
        String rest = name.substring(PREFIX.length());
        if ("MMOITEMS_ITEM_TYPE".equals(tag)) {
            return rest.contains(" ") ? rest.substring(0, rest.indexOf(' ')) : rest;
        }
        if ("MMOITEMS_ITEM_ID".equals(tag)) {
            return rest.contains(" ") ? rest.substring(rest.indexOf(' ') + 1) : "";
        }
        return null;
    }
}
