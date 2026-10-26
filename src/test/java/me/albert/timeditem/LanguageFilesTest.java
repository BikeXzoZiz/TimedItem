package me.albert.timeditem;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards the shipped language files: every message key must exist in
 * every translation, and the unit labels must stay identical across
 * files because they are part of the stored lore format.
 */
class LanguageFilesTest {

    private static final String[] MESSAGE_KEYS = {
            "prefix", "expire", "expire_soon", "no_item", "player_only", "no_timer",
            "item_cleared", "time_added", "timer_paused", "timer_resumed",
            "timer_not_started", "already_paused", "already_running", "invalid_usage",
            "config_reload", "time_set", "info_remaining", "info_lifetime",
            "give_sent", "give_received", "target_not_found", "material_blacklisted",
            "restore_given", "restore_empty", "update_available",
            "debug_status", "debug_on", "debug_off", "debug_exported",
            "debug_export_failed", "debug_usage", "item_not_found"};

    private static final String[] DATE_KEYS = {
            "expires_at", "useful_life", "paused", "s", "m", "h", "d"};

    private static final String[] GUI_KEYS = {
            "menu_title", "preset_lore", "toggle_name", "toggle_lore", "remove_name",
            "remove_lore", "close_name", "status_name", "status_no_timer",
            "status_type_expire", "status_type_paused", "status_type_lifetime",
            "status_remaining", "status_expires_at", "status_duration", "restore_title",
            "restore_date", "restore_player", "restore_world", "restore_click",
            "restore_prev", "restore_next"};

    @Test
    void englishFileHasEveryKey() {
        YamlConfiguration lang = load("/lang/messages_en.yml");
        assertAllKeysPresent(lang);
    }

    @Test
    void thaiFileHasEveryKey() {
        YamlConfiguration lang = load("/lang/messages_th.yml");
        assertAllKeysPresent(lang);
    }

    @Test
    void unitLabelsMatchAcrossLanguages() {
        YamlConfiguration en = load("/lang/messages_en.yml");
        YamlConfiguration th = load("/lang/messages_th.yml");
        for (String unit : new String[] {"s", "m", "h", "d"}) {
            assertEquals(en.getString("DateFormat." + unit), th.getString("DateFormat." + unit),
                    "unit label '" + unit + "' must be identical in every language file");
        }
    }

    private void assertAllKeysPresent(YamlConfiguration lang) {
        for (String key : MESSAGE_KEYS) {
            String value = lang.getString("Messages." + key);
            assertNotNull(value, "missing key Messages." + key);
            assertFalse(value.trim().isEmpty(), "empty key Messages." + key);
        }
        for (String key : DATE_KEYS) {
            String value = lang.getString("DateFormat." + key);
            assertNotNull(value, "missing key DateFormat." + key);
            assertFalse(value.isEmpty(), "empty key DateFormat." + key);
        }
        for (String key : GUI_KEYS) {
            String value = lang.getString("Gui." + key);
            assertNotNull(value, "missing key Gui." + key);
            assertFalse(value.trim().isEmpty(), "empty key Gui." + key);
        }
    }

    private YamlConfiguration load(String resource) {
        InputStream stream = getClass().getResourceAsStream(resource);
        assertNotNull(stream, "resource not found: " + resource
                + " (src/main/resources must be on the test classpath)");
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
