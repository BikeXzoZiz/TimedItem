package me.albert.timeditem;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parses transform profiles from YAML text (no server: Material,
 * YamlConfiguration and ChatColor are server-independent).
 */
class TransformsTest {

    private static Map<String, Transforms.Profile> parse(TimedItemPlugin unused, String yaml) {
        YamlConfiguration raw = new YamlConfiguration();
        try {
            raw.loadFromString(yaml);
        } catch (org.bukkit.configuration.InvalidConfigurationException e) {
            throw new IllegalStateException(e);
        }
        return Transforms.parse(raw.getConfigurationSection("transforms"), null);
    }

    private static final String YAML = ""
            + "transforms:\n"
            + "  ash:\n"
            + "    material: GUNPOWDER\n"
            + "    name: \"&8Ash\"\n"
            + "    lore:\n"
            + "      - \"&7line one\"\n"
            + "    amount: 2\n"
            + "    commands:\n"
            + "      - \"say done\"\n"
            + "  broken-key:\n"
            + "    material: NOT_A_MATERIAL\n"
            + "  empty-key:\n"
            + "    name: \"&cno result\"\n";

    @Test
    void parsesMaterialProfile() {
        Map<String, Transforms.Profile> profiles = parse(null, YAML);
        assertEquals(1, profiles.size());
        Transforms.Profile ash = Transforms.find(profiles, "ASH");
        assertTrue(ash != null); // lookup is case-insensitive
        assertEquals(Material.GUNPOWDER, ash.getMaterial());
        assertNull(ash.getCustomId());
        assertEquals(2, ash.getAmount());
        assertEquals(1, ash.getCommands().size());
        assertEquals("x2 GUNPOWDER", ash.describe());
    }

    @Test
    void skipsProfilesWithoutValidResult() {
        Map<String, Transforms.Profile> profiles = parse(null, YAML);
        assertNull(Transforms.find(profiles, "broken-key"));
        assertNull(Transforms.find(profiles, "empty-key"));
        assertNull(Transforms.find(profiles, null));
        assertNull(Transforms.find(profiles, "missing"));
    }
}
