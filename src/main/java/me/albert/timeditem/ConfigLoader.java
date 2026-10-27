package me.albert.timeditem;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Builds an immutable {@link PluginConfig} snapshot from config.yml and
 * the language file. All disk I/O happens here against local
 * YamlConfiguration objects - never JavaPlugin's cached config - so the
 * same code path works on the main thread during startup and on an
 * async thread during /ti reload. Missing keys self-heal: defaults are
 * registered and written back to both files.
 */
final class ConfigLoader {

    private ConfigLoader() {
    }

    /** Reads both files and returns a fully built, immutable snapshot. */
    static PluginConfig load(TimedItemPlugin plugin) {
        plugin.saveDefaultConfig();
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration raw = YamlConfiguration.loadConfiguration(file);
        registerDefaults(raw);
        raw.options().copyDefaults(true);
        try {
            raw.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save config.yml: " + e);
        }

        FileConfiguration lang = loadLanguage(plugin, raw);
        Messages messages = new Messages(lang);
        Settings settings = new Settings(raw, plugin, messages.unitSeconds);
        return new PluginConfig(messages, settings);
    }

    /** Translates &amp;-codes to chat colors; shared by messages and lore formats. */
    static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static void registerDefaults(FileConfiguration raw) {
        raw.addDefault("config-version", 13);
        raw.addDefault("Settings.language", "en");
        raw.addDefault("Settings.start_date_calculate_on_click", true);
        raw.addDefault("Settings.reset_player_health_if_armor", true);
        raw.addDefault("Settings.metrics", true);
        raw.addDefault("Settings.debug", false);
        raw.addDefault("Settings.expire_check.enabled", true);
        raw.addDefault("Settings.expire_check.interval_seconds", 10);
        raw.addDefault("Settings.expire_check.scan_world_items", false);
        raw.addDefault("Settings.expire_check.ignore_creative", true);
        raw.addDefault("Settings.discord_webhook.enabled", false);
        raw.addDefault("Settings.discord_webhook.url", "");
        raw.addDefault("Settings.discord_webhook.username", "TimedItem");
        raw.addDefault("Settings.discord_webhook.message",
                "{player}'s timed item expired: {item} ({world})");
        raw.addDefault("Settings.warning.enabled", true);
        raw.addDefault("Settings.warning.seconds", 60);
        raw.addDefault("Settings.warning.stages", Collections.singletonList(60));
        raw.addDefault("Settings.warning.sound.name", "block.note_block.pling");
        raw.addDefault("Settings.warning.sound.volume", 1.0);
        raw.addDefault("Settings.warning.sound.pitch", 1.5);
        raw.addDefault("Settings.particles.enabled", true);
        raw.addDefault("Settings.particles.name", "FLAME");
        raw.addDefault("Settings.particles.count", 30);
        raw.addDefault("Settings.expiry_commands", new java.util.ArrayList<String>());
        raw.addDefault("Settings.actionbar.enabled", true);
        raw.addDefault("Settings.actionbar.format", "&e⌛ {time}");
        raw.addDefault("Settings.sound.enabled", true);
        raw.addDefault("Settings.sound.name", "entity.blaze.death");
        raw.addDefault("Settings.sound.volume", 10.0);
        raw.addDefault("Settings.sound.pitch", 1.2);
        raw.addDefault("Settings.update_check.enabled", true);
        raw.addDefault("Settings.update_check.repository", "BikeXzoZiz/TimedItem");
        raw.addDefault("Settings.recovery.enabled", true);
        raw.addDefault("Settings.recovery.max_items", 500);
        raw.addDefault("Settings.recovery.keep_days", 7);
        raw.addDefault("Settings.storage.type", "flat");
        raw.addDefault("Settings.storage.mysql.host", "localhost");
        raw.addDefault("Settings.storage.mysql.port", 3306);
        raw.addDefault("Settings.storage.mysql.database", "timeditem");
        raw.addDefault("Settings.storage.mysql.user", "timeditem");
        raw.addDefault("Settings.storage.mysql.password", "");
        raw.addDefault("Settings.storage.mysql.use_ssl", false);
        raw.addDefault("Settings.storage.mysql.pool_size", 4);
        raw.addDefault("Settings.storage.sqlite.file", "database.db");
        raw.addDefault("Settings.storage.mongodb.uri", "mongodb://localhost:27017");
        raw.addDefault("Settings.storage.mongodb.database", "timeditem");
        raw.addDefault("Settings.storage.audit_history", 5000);
        raw.addDefault("Settings.expiry_transform.enabled", false);
        raw.addDefault("Settings.expiry_transform.default", "");
        raw.addDefault("Settings.transforms", new java.util.ArrayList<String>());
        raw.addDefault("Settings.grace.enabled", false);
        raw.addDefault("Settings.grace.seconds", 30);
        raw.addDefault("Settings.renewal.enabled", false);
        raw.addDefault("Settings.renewal.add_time", "7d");
        raw.addDefault("Settings.renewal.token.material", "CLOCK");
        raw.addDefault("Settings.renewal.token.custom_id", "");
        raw.addDefault("Settings.renewal.token.name", "");
        raw.addDefault("Settings.discord_webhook.use_embed", false);
        raw.addDefault("Settings.discord_webhook.embed_title", "TimedItem");
        raw.addDefault("Settings.discord_webhook.embed_color", "ff5555");
        raw.addDefault("Settings.visual_stages", new java.util.ArrayList<String>());
        raw.addDefault("Settings.hologram.enabled", false);
        raw.addDefault("Settings.hologram.format", "&e⌛ {time}");
        raw.addDefault("Settings.hologram.height", 0.5);
        raw.addDefault("Settings.live_lore.enabled", false);
        raw.addDefault("Settings.live_lore.format", "&e⌛ {time}");
        raw.addDefault("Settings.container_scan.enabled", false);
        raw.addDefault("Settings.container_scan.mode", "open");
        raw.addDefault("Settings.container_scan.interval_seconds", 60);
        raw.addDefault("Settings.anti_hide.enabled", false);
        raw.addDefault("Settings.anti_hide.block_below_seconds", 60);
        raw.addDefault("Settings.return_to_owner.enabled", false);
        raw.addDefault("Settings.return_to_owner.remove_timer_on_return", true);
        raw.addDefault("Settings.restore.network", false);
        raw.addDefault("Settings.blacklist.materials", new java.util.ArrayList<String>());
        raw.addDefault("Hooks.mmoitems", true);
        raw.addDefault("Hooks.itemsadder", true);
        raw.addDefault("Hooks.oraxen", true);
        raw.addDefault("Hooks.nexo", true);
        raw.addDefault("Hooks.executableitems", true);
        raw.addDefault("Hooks.slimefun", true);
        raw.addDefault("Hooks.ecoitems", true);
        raw.addDefault("Hooks.crazycrates.enabled", true);
        raw.addDefault("Hooks.crazycrates.auto_timer", "");
        raw.addDefault("Hooks.shopkeepers.enabled", true);
        raw.addDefault("Hooks.shopkeepers.auto_timer", "");
    }

    /**
     * Loads lang/messages_&lt;language&gt;.yml (Settings.language). Old
     * installs carry their messages inside config.yml - those values are
     * carried over into the language file once and removed from
     * config.yml, so user edits survive the move. Missing keys self-heal
     * from the built-in English defaults and are written back.
     */
    private static FileConfiguration loadLanguage(TimedItemPlugin plugin, FileConfiguration raw) {
        String language = raw.getString("Settings.language", "en");
        if (language == null || !language.matches("[a-z0-9_-]+")) {
            language = "en";
        }
        String path = "lang/messages_" + language + ".yml";
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            try {
                plugin.saveResource(path, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown language '" + language + "' - using English.");
                path = "lang/messages_en.yml";
                file = new File(plugin.getDataFolder(), path);
                if (!file.exists()) {
                    plugin.saveResource(path, false);
                }
            }
        }
        YamlConfiguration lang = YamlConfiguration.loadConfiguration(file);

        boolean migrated = false;
        if (raw.isSet("Messages")) {
            for (String key : raw.getConfigurationSection("Messages").getKeys(true)) {
                lang.set("Messages." + key, raw.get("Messages." + key));
            }
            raw.set("Messages", null);
            migrated = true;
        }
        if (raw.isSet("DateFormat")) {
            for (String key : raw.getConfigurationSection("DateFormat").getKeys(true)) {
                lang.set("DateFormat." + key, raw.get("DateFormat." + key));
            }
            raw.set("DateFormat", null);
            migrated = true;
        }
        if (migrated) {
            try {
                raw.save(new File(plugin.getDataFolder(), "config.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save config.yml: " + e);
            }
            plugin.getLogger().info("Moved messages from config.yml to " + path
                    + " - edit that file from now on.");
        }

        lang.options().copyDefaults(true);
        addMessageDefaults(lang);
        try {
            lang.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + path + ": " + e);
        }
        return lang;
    }

    /** Built-in English fallbacks for every message key (self-healing). */
    private static void addMessageDefaults(FileConfiguration lang) {
        lang.addDefault("Messages.prefix", "&6[TimedItem] &r");
        lang.addDefault("Messages.expire", "&cYour item has expired and was removed!");
        lang.addDefault("Messages.expire_soon", "&eYour item will expire in &c{time}&e!");
        lang.addDefault("Messages.no_item", "&cHold the item you want to manage first.");
        lang.addDefault("Messages.player_only", "&cOnly players can use this command.");
        lang.addDefault("Messages.no_timer", "&7The item in your hand has no timer.");
        lang.addDefault("Messages.item_cleared", "&aTimer removed from the item in your hand.");
        lang.addDefault("Messages.time_added", "&aTime added. Remaining: &e{time}");
        lang.addDefault("Messages.timer_paused", "&aTimer paused with &e{time}&a remaining.");
        lang.addDefault("Messages.timer_resumed", "&aTimer resumed. Expires in &e{time}&a.");
        lang.addDefault("Messages.timer_not_started", "&7The timer on this item has not started yet.");
        lang.addDefault("Messages.already_paused", "&7The timer on this item is already paused.");
        lang.addDefault("Messages.already_running", "&7The timer on this item is already running.");
        lang.addDefault("Messages.invalid_usage",
                "&cUsage: /ti set <time>, /ti add <time>, /ti give <player> [plugin:id] <time> [amount], /ti pause, /ti resume, /ti remove, /ti info, /ti menu, /ti restore, /ti debug, /ti reload");
        lang.addDefault("Messages.config_reload", "&aTimedItem config reloaded.");
        lang.addDefault("Messages.reload_in_progress", "&7A config reload is already running - wait for it to finish.");
        lang.addDefault("Messages.reload_failed", "&cConfig reload failed: &7{error} &c- the old config stays active.");
        lang.addDefault("Messages.time_set", "&aTimer set: &e{time}");
        lang.addDefault("Messages.info_remaining", "&7Time remaining: &e{time}");
        lang.addDefault("Messages.info_lifetime", "&7Lifetime: &e{time} &7(starts on first use)");
        lang.addDefault("Messages.give_sent", "&aGave a timed item (&e{time}&a) to &e{player}&a.");
        lang.addDefault("Messages.give_received", "&aYou received a timed item expiring in &e{time}&a.");
        lang.addDefault("Messages.target_not_found", "&cPlayer &e{player} &cis not online.");
        lang.addDefault("Messages.material_blacklisted",
                "&cThis item's material cannot carry a TimedItem timer.");
        lang.addDefault("Messages.restore_given", "&aRecovered the item from the expiry log.");
        lang.addDefault("Messages.restore_empty", "&7The expiry recovery log is empty.");
        lang.addDefault("Messages.update_available",
                "&eTimedItem &6{version} &eis available: &7{url}");
        lang.addDefault("Messages.debug_status",
                "&7Debug mode: &e{state}&7. Buffered log entries: &e{entries}&7."
                        + " Recovery entries: &e{recovery}&7. Item storage: &e{storage}&7.");
        lang.addDefault("Messages.debug_on",
                "&aDebug mode enabled - verbose TimedItem logging is now visible in the console."
                        + " Use /ti debug export to write a bug report.");
        lang.addDefault("Messages.debug_off", "&7Debug mode disabled.");
        lang.addDefault("Messages.debug_exported",
                "&aDebug report written to &e{file}");
        lang.addDefault("Messages.debug_export_failed",
                "&cCould not write the debug report: &7{error}");
        lang.addDefault("Messages.debug_usage", "&7Usage: /ti debug [on|off|export]");
        lang.addDefault("Messages.item_not_found",
                "&cUnknown custom item: &e{id}&c. Use <plugin>:<id>, e.g. mmoitems:SWORD:CUTLASS.");
        lang.addDefault("Messages.find_usage",
                "&7Usage: /ti find <material|plugin:id|name>");
        lang.addDefault("Messages.find_no_match",
                "&7No timed items matching &e{query} &7were found.");
        lang.addDefault("Messages.find_result",
                "&aFound &e{count} &atimed item(s) matching &e{query}&a:");
        lang.addDefault("Messages.find_truncated",
                "&7...and &e{rest} &7more (showing the first {shown}).");
        lang.addDefault("Messages.find_hit_player", " &8- &e{player} &7(slot {slot})");
        lang.addDefault("Messages.find_hit_container", " &8- &7{world} &e{x},{y},{z} &7({type})");
        lang.addDefault("Messages.find_hit_ground", " &8- &7{world} &e{x},{y},{z}");
        lang.addDefault("Messages.stats_header", "&6Expired items (last {days} days):");
        lang.addDefault("Messages.stats_line", " &e{material}&7: &a{count}");
        lang.addDefault("Messages.stats_total", "&7Total expired: &e{count}");
        lang.addDefault("Messages.stats_empty",
                "&7No items have expired in the last {days} days.");
        lang.addDefault("Messages.audit_header", "&6Audit log (page {page}):");
        lang.addDefault("Messages.audit_line",
                " &8[{date}] &e{actor} &7{action} &7target=&f{target} &7item=&f{item} &7{details}");
        lang.addDefault("Messages.audit_empty", "&7The audit log is empty.");
        lang.addDefault("Messages.transform_received",
                "&7Your expired item turned into &e{item}&7.");
        lang.addDefault("Messages.grace_blocked",
                "&cThis item expires in &e{time} &cand can no longer be used!");
        lang.addDefault("Messages.renewal_success",
                "&aRenewed with a token. Remaining: &e{time}");
        lang.addDefault("Messages.renewal_token_missing",
                "&7Hold the item to renew in one hand and the renewal token in the other.");
        lang.addDefault("Messages.restore_reminder",
                "&eYou have &6{count} &eexpired item(s) waiting for restore. &7(/ti restore)");
        lang.addDefault("Messages.transform_unknown",
                "&cUnknown transform profile: &e{id}&c. Define it under Settings.transforms.");
        lang.addDefault("Messages.anti_hide_blocked",
                "&cThis item expires in &e{time} &c- it can no longer be stored in containers!");
        lang.addDefault("Messages.check_usage", "&7Usage: /ti check <player>");
        lang.addDefault("Messages.check_offline", "&cPlayer &e{player} &cmust be online for a check.");
        lang.addDefault("Messages.check_header", "&6Timed items of &e{player}&6:");
        lang.addDefault("Messages.check_line", " &8- &7slot &e{slot}&7: &e{item} &7({type}, &e{time}&7)");
        lang.addDefault("Messages.check_empty", "&7{player} carries no timed items.");
        lang.addDefault("Gui.admin_title", "&8TimedItem Admin &7({page}/{pages})");
        lang.addDefault("Gui.admin_owner", "&7Owner: &e{player}");
        lang.addDefault("Gui.admin_type", "&7Timer: &e{type} &7- &e{time}");
        lang.addDefault("Gui.admin_expire_now", "&aLeft-click: &7expire now");
        lang.addDefault("Gui.admin_remove_timer", "&cRight-click: &7remove timer");
        lang.addDefault("Gui.admin_empty", "&7No timed items seen by the last scan");
        lang.addDefault("Gui.admin_prev", "&ePrevious page");
        lang.addDefault("Gui.admin_next", "&eNext page");
        lang.addDefault("Messages.admin_expired", "&aExpired the item of &e{player}&a.");
        lang.addDefault("Messages.admin_removed", "&aRemoved the timer of &e{player}&a's item.");
        lang.addDefault("Messages.info_owner", "&7Owner: &e{player}");
        lang.addDefault("Messages.returned_to_owner",
                "&aYour rented item expired and was returned to you.");
        lang.addDefault("Messages.sell_blocked",
                "&cTimed items cannot be sold or listed! &7(expires in &e{time}&7)");
        lang.addDefault("Messages.restore_loading", "&7Loading expired items...");
        lang.addDefault("Messages.restore_network_title", "&8Expired items (all servers) &7({page}/{pages})");
        lang.addDefault("DateFormat.expires_at", "&cExpires: ");
        lang.addDefault("DateFormat.useful_life", "&7Lifetime: ");
        lang.addDefault("DateFormat.paused", "&7Paused: ");
        lang.addDefault("DateFormat.playtime", "&6Playtime: ");
        lang.addDefault("DateFormat.held", "&6Held time: ");
        lang.addDefault("DateFormat.s", "s");
        lang.addDefault("DateFormat.m", "m");
        lang.addDefault("DateFormat.h", "h");
        lang.addDefault("DateFormat.d", "d");
        lang.addDefault("Gui.menu_title", "&6TimedItem Menu");
        lang.addDefault("Gui.preset_lore", "&7Set this lifetime on the item in your hand");
        lang.addDefault("Gui.toggle_name", "&6Pause / Resume");
        lang.addDefault("Gui.toggle_lore", "&7Freeze the countdown, or resume a paused item");
        lang.addDefault("Gui.remove_name", "&cRemove timer");
        lang.addDefault("Gui.remove_lore", "&7Strip the timer from the item in your hand");
        lang.addDefault("Gui.close_name", "&cClose");
        lang.addDefault("Gui.status_name", "&6Held item status");
        lang.addDefault("Gui.status_no_timer", "&7Held item: &cno timer");
        lang.addDefault("Gui.status_type_expire", "&7Type: &eexpire");
        lang.addDefault("Gui.status_type_paused", "&7Type: &epaused");
        lang.addDefault("Gui.status_type_lifetime", "&7Type: &elifetime (not started)");
        lang.addDefault("Gui.status_type_playtime", "&7Type: &eplaytime (while online)");
        lang.addDefault("Gui.status_type_held", "&7Type: &eheld time (while held/worn)");
        lang.addDefault("Gui.status_remaining", "&7Remaining: &e{time}");
        lang.addDefault("Gui.status_expires_at", "&7Expires at: &e{date}");
        lang.addDefault("Gui.status_duration", "&7Duration: &e{time}");
        lang.addDefault("Gui.restore_title", "&8TimedItem Restore &7({page}/{pages})");
        lang.addDefault("Gui.restore_date", "&8{date}");
        lang.addDefault("Gui.restore_player", "&7Player: &e{player}");
        lang.addDefault("Gui.restore_world", "&7World: &e{world}");
        lang.addDefault("Gui.restore_click", "&aClick to recover");
        lang.addDefault("Gui.restore_prev", "&ePrevious page");
        lang.addDefault("Gui.restore_next", "&eNext page");
    }
}
