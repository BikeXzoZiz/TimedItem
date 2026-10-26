package me.albert.timeditem.hook;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * One integration with a plugin that creates or distributes custom
 * items. Implementations resolve foreign items through reflection only
 * (see {@link HookReflection}) and must never reference the target
 * plugin's classes, so this package compiles against plain Bukkit.
 *
 * Everything a custom item needs from TimedItem flows through here:
 * identify an ItemStack ({@link #itemId}) for messages and
 * %timeditem_itemid%, and build one from a give token such as
 * mmoitems:SWORD:CUTLASS ({@link #build}).
 */
public interface CustomItemAdapter {

    /** Lowercase token used in give commands and the Hooks config key. */
    String id();

    /** Short alternative tokens accepted by /ti give (e.g. ei). */
    default List<String> aliases() {
        return Collections.emptyList();
    }

    /** True when the target plugin is installed and its API probed OK. */
    boolean isAvailable();

    /**
     * The item's id inside the target plugin (without the plugin
     * prefix), or null when this stack is not one of the plugin's
     * items. May be an empty string when the plugin's items cannot be
     * identified on this version - build() still works then.
     */
    String itemId(ItemStack item);

    /**
     * Builds the item from id parts (the token after the plugin prefix,
     * split on ':'), or null when the id is unknown or the API mismatched.
     */
    ItemStack build(String... idParts);
}
