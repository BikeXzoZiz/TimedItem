package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import me.albert.timeditem.PluginConfig;
import me.albert.timeditem.TimeLore;
import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Owns every integration with other item plugins. Created once on
 * enable; {@link #applyConfig()} re-reads the Hooks.* toggles and
 * (re)registers the distribution auto-timers, so /ti reload applies
 * changes without a restart.
 *
 * All adapters are reflection based - the hook package never touches a
 * foreign class at compile time, keeps the single-jar Java 8 promise,
 * and a mismatching plugin version simply reports itself unavailable.
 */
public final class HookManager {

    private final TimedItemPlugin plugin;
    private final List<CustomItemAdapter> itemAdapters = new ArrayList<CustomItemAdapter>();
    private final Map<String, Boolean> tokenEnabled = new HashMap<String, Boolean>();
    private final List<ReflectiveEventHook.Registration> distributionRegistrations =
            new ArrayList<ReflectiveEventHook.Registration>();
    private long shopkeepersSeconds;
    private long crazycratesSeconds;

    private HookManager(TimedItemPlugin plugin) {
        this.plugin = plugin;
        itemAdapters.add(new MmoItemsAdapter());
        itemAdapters.add(new ItemsAdderAdapter());
        itemAdapters.add(new OraxenAdapter());
        itemAdapters.add(new NexoAdapter());
        itemAdapters.add(new ExecutableItemsAdapter());
        itemAdapters.add(new SlimefunAdapter());
        itemAdapters.add(new EcoItemsAdapter());
        itemAdapters.add(new CrazyCratesAdapter());
    }

    /** Probes all adapters and applies the Hooks.* config section. */
    public static HookManager create(TimedItemPlugin plugin) {
        HookManager manager = new HookManager(plugin);
        manager.applyConfig();
        return manager;
    }

    /** Re-reads the config toggles; safe to call on every /ti reload. */
    public void applyConfig() {
        PluginConfig config = plugin.pluginConfig();
        List<String> active = new ArrayList<String>();
        tokenEnabled.clear();
        for (CustomItemAdapter adapter : itemAdapters) {
            boolean on = adapter.isAvailable() && config.isHookEnabled(adapter.id());
            tokenEnabled.put(adapter.id(), on);
            for (String alias : adapter.aliases()) {
                tokenEnabled.put(alias.toLowerCase(Locale.ROOT), on);
            }
            if (on) {
                active.add(adapter.id());
            }
        }
        unregisterDistribution();
        shopkeepersSeconds = parseTimer("Shopkeepers", config.getShopkeepersAutoTimer());
        crazycratesSeconds = parseTimer("CrazyCrates", config.getCrazyCratesAutoTimer());
        hookDistribution("Shopkeepers", "Shopkeepers", shopkeepersSeconds, config);
        hookDistribution("CrazyCrates", "CrazyCrates", crazycratesSeconds, config);
        // block selling timed items on shop/auction plugins (3.1+)
        hookSellBlock("QuickShop", "QuickShop", config, new String[]{
                "com.ghostchu.quickshop.api.event.ShopPreCreateEvent",
                "com.ghostchu.quickshop.api.event.ShopCreateEvent",
                "org.maxgamer.quickshop.api.event.ShopPreCreateEvent",
                "org.maxgamer.quickshop.Event.ShopPreCreateEvent",
                "org.maxgamer.quickshop.api.event.ShopCreateEvent",
                "org.maxgamer.quickshop.Event.ShopCreateEvent"});
        hookSellBlock("zAuctionHouse", "zAuctionHouse", config, new String[]{
                "fr.maxlego08.zauctionhouse.api.event.events.ZSellEvent",
                "fr.maxlego08.zauctionhouse.api.event.ZSellEvent",
                "fr.maxlego08.zauctionhouse.api.event.events.ZAuctionSellEvent"});
        hookSellBlock("CrazyAuctions", "CrazyAuctions", config, new String[]{
                "com.badbones69.crazyauctions.api.events.AuctionListEvent",
                "com.badbones69.crazyauctions.api.events.AuctionSellEvent"});
        if (!active.isEmpty()) {
            plugin.getLogger().info("Hooked into item plugins: " + String.join(", ", active)
                    + " (identify + /ti give <player> <plugin>:<id> <time>).");
        }
    }

    /** Stops the distribution listeners; called on reload and disable. */
    public void close() {
        unregisterDistribution();
    }

    /** "plugin:rest" id of the item, or null when no enabled adapter claims it. */
    public String itemId(ItemStack item) {
        for (CustomItemAdapter adapter : itemAdapters) {
            if (!isEnabled(adapter)) {
                continue;
            }
            String id = adapter.itemId(item);
            if (id != null) {
                return adapter.id() + ":" + id;
            }
        }
        return null;
    }

    /**
     * Human friendly name for the Discord webhook: the item's display
     * name when it has one, then the plugin:id form, then the material.
     * Never null. Color codes are stripped for the Discord chat.
     */
    public String describe(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String stripped = ChatColor.stripColor(meta.getDisplayName());
                if (stripped != null && !stripped.trim().isEmpty()) {
                    return stripped;
                }
            }
        }
        String id = itemId(item);
        if (id != null) {
            return id;
        }
        return item == null ? "unknown" : item.getType().name();
    }

    /** Builds the item of a give token such as "mmoitems:SWORD:CUTLASS", or null. */
    public ItemStack build(String token) {
        String[] parsed = parseToken(token);
        if (parsed == null) {
            return null;
        }
        CustomItemAdapter adapter = byToken(parsed[0]);
        if (adapter == null || !isEnabled(adapter)) {
            return null;
        }
        String[] parts = new String[parsed.length - 1];
        System.arraycopy(parsed, 1, parts, 0, parts.length);
        try {
            return adapter.build(parts);
        } catch (Throwable t) {
            DebugLog.verbose("Hook build " + token + " failed: " + t);
            return null;
        }
    }

    /**
     * Splits a give token "plugin:id[:more...]" into
     * [plugin(lowercase), id...] - null when there is no usable prefix
     * (pure, so the tests can pin the command's grammar).
     */
    static String[] parseToken(String token) {
        if (token == null) {
            return null;
        }
        int split = token.indexOf(':');
        if (split <= 0 || split == token.length() - 1) {
            return null;
        }
        String[] parts = token.substring(split + 1).split(":");
        String[] out = new String[parts.length + 1];
        out[0] = token.substring(0, split).toLowerCase(Locale.ROOT);
        System.arraycopy(parts, 0, out, 1, parts.length);
        return out;
    }

    /** Give tokens ("mmoitems:", "ei:", ...) of the enabled adapters, for tab completion. */
    public List<String> tokens() {
        List<String> out = new ArrayList<String>();
        for (CustomItemAdapter adapter : itemAdapters) {
            if (isEnabled(adapter)) {
                out.add(adapter.id() + ":");
                for (String alias : adapter.aliases()) {
                    out.add(alias + ":");
                }
            }
        }
        return out;
    }

    /** Comma separated ids of the active item adapters (debug report). */
    public String summary() {
        List<String> out = new ArrayList<String>();
        for (CustomItemAdapter adapter : itemAdapters) {
            out.add(adapter.id() + (isEnabled(adapter) ? "" : " (off)"));
        }
        return String.join(", ", out);
    }

    public boolean isHookEnabled(String id) {
        return Boolean.TRUE.equals(tokenEnabled.get(id.toLowerCase(Locale.ROOT)));
    }

    private void hookDistribution(String pluginName, String hookId, long seconds, PluginConfig config) {
        if (seconds <= 0 || !config.isHookEnabled(hookId)) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            return;
        }
        ReflectiveEventHook.Registration registration = pluginName.equals("Shopkeepers")
                ? ShopkeepersTimerHook.register(plugin, seconds)
                : CrazyCratesTimerHook.register(plugin, seconds);
        if (registration != null) {
            distributionRegistrations.add(registration);
            plugin.getLogger().info("Hooked into " + pluginName
                    + " (auto-timer: " + TimeLore.formatDuration(seconds, config) + ").");
        }
    }

    private void unregisterDistribution() {
        for (ReflectiveEventHook.Registration registration : distributionRegistrations) {
            registration.unregister();
        }
        distributionRegistrations.clear();
    }

    /**
     * Blocks selling/listing timed items on one shop plugin (3.1+):
     * gated by Hooks.<id>.enabled, active only when the plugin is
     * installed, silent no-op when its event class is unknown here.
     */
    private void hookSellBlock(String pluginName, String hookId, PluginConfig config,
                               String[] eventCandidates) {
        if (!config.isHookEnabled(hookId)) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            return;
        }
        ReflectiveEventHook.Registration registration =
                ShopSellBlockHook.register(plugin, hookId, eventCandidates);
        if (registration != null) {
            distributionRegistrations.add(registration);
            plugin.getLogger().info("Hooked into " + pluginName
                    + " (block selling timed items).");
        }
    }

    private long parseTimer(String hook, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        long seconds = me.albert.timeditem.DurationParser.parse(raw.trim(), plugin.pluginConfig().unitSeconds());
        if (seconds <= 0) {
            plugin.getLogger().warning("Hooks." + hook.toLowerCase(Locale.ROOT)
                    + ".auto_timer '" + raw + "' is not a valid duration - hook disabled.");
            return 0;
        }
        return seconds;
    }

    private boolean isEnabled(CustomItemAdapter adapter) {
        return Boolean.TRUE.equals(tokenEnabled.get(adapter.id()));
    }

    private CustomItemAdapter byToken(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        for (CustomItemAdapter adapter : itemAdapters) {
            if (adapter.id().equals(lower)) {
                return adapter;
            }
            if (adapter.aliases().contains(lower)) {
                return adapter;
            }
        }
        return null;
    }
}
