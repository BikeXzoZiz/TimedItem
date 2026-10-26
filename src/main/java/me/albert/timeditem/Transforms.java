package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Expiry transformation profiles ("transforms.<key>" in config.yml):
 * what an item turns into when its timer runs out instead of simply
 * vanishing. A profile is a result item (a vanilla material or a custom
 * item built through the HookManager integrations), optionally renamed,
 * plus optional extra console commands run at the moment of the
 * transformation. The whole section is optional - an empty map simply
 * keeps the 2.6 behaviour of removing the item.
 */
public final class Transforms {

    /** One "transforms.<key>" entry. */
    public static final class Profile {
        private final String key;
        private final Material material;   // null when customId is used
        private final String customId;     // "plugin:rest", wins over material
        private final String name;         // display name, already colored; null = keep
        private final List<String> lore;   // already colored; empty = keep
        private final int amount;
        private final List<String> commands;

        Profile(String key, Material material, String customId, String name, List<String> lore,
                int amount, List<String> commands) {
            this.key = key;
            this.material = material;
            this.customId = customId;
            this.name = name;
            this.lore = lore;
            this.amount = amount;
            this.commands = commands;
        }

        public String getKey() {
            return key;
        }

        public Material getMaterial() {
            return material;
        }

        public String getCustomId() {
            return customId;
        }

        public String getName() {
            return name;
        }

        public List<String> getLore() {
            return lore;
        }

        public int getAmount() {
            return amount;
        }

        public List<String> getCommands() {
            return commands;
        }

        /** Human readable description for messages ("x1 GUNPOWDER"). */
        public String describe() {
            String what = customId != null ? customId
                    : (material == null ? "AIR" : material.name());
            return "x" + amount + " " + what;
        }
    }

    private Transforms() {
    }

    private static void warn(TimedItemPlugin plugin, String message) {
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }

    /**
     * Reads the Settings.transforms section; unknown materials, unknown
     * custom ids and profiles without any result item are skipped with a
     * warning so a typo can never break expiry.
     */
    public static Map<String, Profile> parse(ConfigurationSection section, TimedItemPlugin plugin) {
        Map<String, Profile> out = new LinkedHashMap<String, Profile>();
        if (section == null) {
            return out;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection raw = section.getConfigurationSection(key);
            if (raw == null) {
                continue;
            }
            String customId = raw.getString("custom_id", "");
            if (customId == null) {
                customId = "";
            }
            Material material = null;
            String materialName = raw.getString("material", "");
            if (materialName != null && !materialName.trim().isEmpty()) {
                material = Material.matchMaterial(materialName.trim());
                if (material == null) {
                    warn(plugin, "Transform '" + key + "': unknown material '"
                            + materialName + "' - ignored.");
                }
            }
            if (material == null) {
                if (customId.trim().isEmpty()) {
                    warn(plugin, "Transform '" + key
                            + "' has neither material nor custom_id - ignored.");
                    continue;
                }
                // the id is only validated at expiry time (the target
                // plugin may load later than TimedItem)
                customId = customId.trim();
            } else {
                customId = null;
            }
            String name = raw.getString("name");
            List<String> lore = new ArrayList<String>();
            for (String line : raw.getStringList("lore")) {
                lore.add(PluginConfig.color(line));
            }
            int amount = Math.max(1, raw.getInt("amount", 1));
            List<String> commands = new ArrayList<String>(raw.getStringList("commands"));
            out.put(key.toLowerCase(Locale.ROOT), new Profile(key, material, customId,
                    name == null || name.isEmpty() ? null : PluginConfig.color(name),
                    lore, amount, commands));
        }
        return out;
    }

    /** Profile lookup, case-insensitive. */
    public static Profile find(Map<String, Profile> profiles, String key) {
        if (key == null) {
            return null;
        }
        return profiles.get(key.toLowerCase(Locale.ROOT));
    }

    /**
     * Builds the replacement item of a profile; null when the build
     * failed (unknown custom id on this server, for example) - the item
     * then simply expires as usual.
     */
    public static ItemStack build(TimedItemPlugin plugin, Profile profile) {
        ItemStack item;
        if (profile.getCustomId() != null) {
            if (plugin.hooks() == null) {
                return null;
            }
            item = plugin.hooks().build(profile.getCustomId());
            if (item == null) {
                return null;
            }
        } else {
            item = new ItemStack(profile.getMaterial());
        }
        int amount = profile.getAmount();
        item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        if ((profile.getName() != null || !profile.getLore().isEmpty()) && item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (profile.getName() != null) {
                    meta.setDisplayName(profile.getName());
                }
                if (!profile.getLore().isEmpty()) {
                    meta.setLore(new ArrayList<String>(profile.getLore()));
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    /** Unmodifiable view used by PluginConfig. */
    public static Map<String, Profile> unmodifiable(Map<String, Profile> profiles) {
        return Collections.unmodifiableMap(profiles);
    }
}
