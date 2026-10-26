package me.albert.timeditem;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Visual decay stages (3.0+, Settings.visual_stages): as an item's
 * remaining time drops below a threshold, its display name gets a color
 * prefix and/or an enchant glint. Vanilla items are recolored; custom
 * items from integrations only glow so the source plugin's name survives.
 *
 * Stages are time thresholds, most severe last: an item is in the first
 * (least severe) stage whose at_seconds >= remaining; below all
 * thresholds the visuals reset. The applied stage index is stored in the
 * item's PDC, so rewriting happens only when the stage actually changes.
 */
public final class VisualStages {

    /** One visual stage; stages are ordered from least to most severe. */
    public static final class Stage {
        public final int atSeconds;
        public final String nameColor; // color code prefix like "&c", empty = keep name
        public final boolean glow;

        Stage(int atSeconds, String nameColor, boolean glow) {
            this.atSeconds = atSeconds;
            this.nameColor = nameColor;
            this.glow = glow;
        }
    }

    private VisualStages() {
    }

    /**
     * Parses Settings.visual_stages (a list of maps). Entries without a
     * positive at_seconds are skipped; the result is sorted from the
     * highest threshold down so index 0 is the least severe stage.
     */
    public static List<Stage> parse(List<Map<?, ?>> raw) {
        List<Stage> stages = new ArrayList<Stage>();
        if (raw == null) {
            return stages;
        }
        for (Map<?, ?> entry : raw) {
            if (entry == null) {
                continue;
            }
            Object atRaw = entry.get("at_seconds");
            int at = atRaw instanceof Number ? ((Number) atRaw).intValue() : -1;
            if (at <= 0) {
                continue;
            }
            Object colorRaw = entry.get("name_color");
            String color = colorRaw == null ? "" : ChatColor.translateAlternateColorCodes('&',
                    String.valueOf(colorRaw));
            Object glowRaw = entry.get("glow");
            boolean glow = glowRaw instanceof Boolean && (Boolean) glowRaw;
            stages.add(new Stage(at, color, glow));
        }
        stages.sort((a, b) -> Integer.compare(b.atSeconds, a.atSeconds));
        return stages;
    }

    /**
     * Applies the stage matching the item's remaining time. Cheap when
     * nothing changed (one PDC read); rewrites name/glow only when the
     * stage index differs from the stored one.
     */
    public static void apply(TimedItemPlugin plugin, ItemStack item, TimeLore.TimerInfo info,
                             PluginConfig config) {
        List<Stage> stages = config.visualStages();
        if (stages.isEmpty() || item == null || !item.hasItemMeta()) {
            return;
        }
        long remaining = remainingOf(info);
        if (remaining < 0) {
            return;
        }
        int computed = -1; // -1 = below every threshold (reset)
        for (int i = 0; i < stages.size(); i++) {
            if (remaining <= stages.get(i).atSeconds) {
                computed = i;
                break;
            }
        }
        String stored = PdcCompat.readString(item, "stage");
        int current = stored == null ? -1 : Integer.parseInt(stored);
        if (computed == current) {
            return;
        }
        if (computed < 0) {
            reset(item, current);
            return;
        }
        Stage stage = stages.get(computed);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        boolean custom = plugin.hooks() != null && plugin.hooks().itemId(item) != null;
        if (!custom && !stage.nameColor.isEmpty()) {
            // remember the original name once so the reset can restore it
            if (current < 0) {
                String original = meta.hasDisplayName()
                        ? meta.getDisplayName() : ChatColor.stripColor(defaultName(item));
                if (original == null) {
                    original = defaultName(item);
                }
                PdcCompat.writeString(item, "visname", original == null ? "" : original);
            }
            String base = meta.hasDisplayName() ? meta.getDisplayName() : defaultName(item);
            meta.setDisplayName(stage.nameColor + (base == null ? "" : base));
        }
        if (stage.glow) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (!stage.nameColor.isEmpty() || current >= 0) {
            // stage without glow after a glowing stage: clean the glint
            meta.removeEnchant(Enchantment.DURABILITY);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        PdcCompat.writeString(item, "stage", String.valueOf(computed));
    }

    /** Restores the pre-stage name (when stored) and removes the glow. */
    private static void reset(ItemStack item, int current) {
        if (current < 0) {
            return; // never staged
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.removeEnchant(Enchantment.DURABILITY);
        meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        String original = PdcCompat.readString(item, "visname");
        if (original != null) {
            if (original.isEmpty()) {
                if (meta.hasDisplayName()) {
                    meta.setDisplayName(null);
                }
            } else {
                meta.setDisplayName(original);
            }
            PdcCompat.removeString(item, "visname");
        }
        item.setItemMeta(meta);
        PdcCompat.removeString(item, "stage");
    }

    private static String defaultName(ItemStack item) {
        String name = item.getType().name();
        return name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    /** Remaining seconds of any timer kind, or -1 when unknown. */
    private static long remainingOf(TimeLore.TimerInfo info) {
        if (info.getType() == TimeLore.Type.EXPIRE) {
            return info.getExpiry() == null ? -1L
                    : Duration.between(LocalDateTime.now(), info.getExpiry()).getSeconds();
        }
        return info.getSeconds();
    }
}
