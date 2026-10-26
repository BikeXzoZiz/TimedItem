package net.Indyuce.mmoitems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake MMOItems: the API class doubles as the plugin main so the static
 * "plugin" field the adapter probes exists. Registered item:
 * SWORD:CUTLASS identified through the MythicLib-style NBT wrapper
 * (io.lumine.mythiclib.api.item.NBTItem, shipped in the separate
 * FakeMythicLib plugin like in production), which reads the marker from
 * the item's display name.
 */
public class MMOItems extends JavaPlugin {

    public static MMOItems plugin;
    private static final String PREFIX = "Fake MMO ";

    private static TypeCollection types;
    private static TemplateManager templates;

    @Override
    public void onEnable() {
        plugin = this;
        types = new TypeCollection();
        templates = new TemplateManager();
        templates.register("SWORD", "CUTLASS", named(Material.DIAMOND_SWORD, PREFIX + "SWORD CUTLASS"));
        getLogger().info("Fake MMOItems ready (SWORD:CUTLASS)");
    }

    public static TypeCollection getTypes() {
        return types;
    }

    public static TemplateManager getTemplates() {
        return templates;
    }

    static ItemStack named(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        stack.setItemMeta(meta);
        return stack;
    }

    public static class TypeCollection {
        private final Map<String, Object> byId = new HashMap<String, Object>();

        public Object get(String id) {
            if (!byId.containsKey(id)) {
                byId.put(id, new Object()); // any object stands in for an MI Type
            }
            return byId.get(id);
        }
    }

    public static class TemplateManager {
        private final Map<String, ItemStack> byId = new HashMap<String, ItemStack>();

        void register(String type, String id, ItemStack stack) {
            byId.put(id, stack);
        }

        public Template getTemplate(Object type, String id) {
            ItemStack stack = byId.get(id);
            return stack == null ? null : new Template(stack);
        }
    }

    public static class Template {
        private final ItemStack stack;

        Template(ItemStack stack) {
            this.stack = stack;
        }

        public Builder newBuilder() {
            return new Builder(stack);
        }
    }

    public static class Builder {
        private final ItemStack stack;

        Builder(ItemStack stack) {
            this.stack = stack;
        }

        public ItemStack build() {
            return stack.clone();
        }
    }
}
