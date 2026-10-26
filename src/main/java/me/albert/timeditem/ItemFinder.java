package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Backing search for /ti find: locates timed items by material, custom
 * item id (plugin:id) or display name across player inventories, ground
 * items, item frames, armor stands and loaded container block entities.
 *
 * World/entity/container scanning touches multiple regions at once, so
 * it only runs on classic servers; on Folia only the per-player
 * inventory search is available (dispatched onto each player's region).
 */
public final class ItemFinder {

    /** Maximum hits reported for one search. */
    public static final int MAX_HITS = 50;

    private ItemFinder() {
    }

    /**
     * Compiles the query into an item predicate. A material name matches
     * by material, a token with ':' matches the custom item id exactly,
     * anything else matches custom id, material name or display name
     * containing it (case-insensitive). Returns null for empty queries.
     */
    public static Predicate<ItemStack> compile(TimedItemPlugin plugin, String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        final String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.indexOf(':') >= 0) {
            return item -> {
                String id = plugin.hooks().itemId(item);
                return id != null && id.toLowerCase(Locale.ROOT).equals(needle);
            };
        }
        Material material = Material.matchMaterial(needle);
        final Material exact = material;
        return item -> {
            if (exact != null && item.getType() == exact) {
                return true;
            }
            String id = plugin.hooks().itemId(item);
            if (id != null && id.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
            if (item.getType().name().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
            return hasDisplayNameContaining(item, needle);
        };
    }

    private static boolean hasDisplayNameContaining(ItemStack item, String needle) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String name = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        return name != null && name.toLowerCase(Locale.ROOT).contains(needle);
    }

    /** True when the stack carries any TimedItem timer (find reports timed items only). */
    public static boolean isTimed(ItemStack item, PluginConfig config) {
        return item != null && item.getType() != Material.AIR && TimeLore.analyze(item, config) != null;
    }

    /**
     * Search hit test: the stack itself matches, or (3.1+) it is a
     * shulker box whose contents match - the shulker is reported, since
     * that is what the admin has to open.
     */
    private static boolean hitIn(ItemStack item, PluginConfig config, Predicate<ItemStack> matcher) {
        if (isTimed(item, config) && matcher.test(item)) {
            return true;
        }
        return Shulkers.isShulker(item) && anyInnerMatches(config, item, matcher);
    }

    private static boolean anyInnerMatches(PluginConfig config, ItemStack shulker,
                                           Predicate<ItemStack> matcher) {
        boolean[] hit = {false};
        Shulkers.forEachInnerItem(shulker, (slot, inner) -> {
            if (!hit[0] && isTimed(inner, config) && matcher.test(inner)) {
                hit[0] = true;
            }
        });
        return hit[0];
    }

    /**
     * Searches one player's whole inventory (storage + armor + offhand).
     * Must run on the player's region thread (Folia) or the main thread.
     */
    public static List<String> findInPlayer(TimedItemPlugin plugin, Player player,
                                            Predicate<ItemStack> matcher) {
        PluginConfig config = plugin.pluginConfig();
        List<String> hits = new ArrayList<String>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!hitIn(item, config, matcher)) {
                continue;
            }
            hits.add(PluginConfig.color(config.getFindHitPlayer()
                    .replace("{player}", player.getName())
                    .replace("{slot}", String.valueOf(slot))));
        }
        return hits;
    }

    /**
     * Searches ground items, frames, armor stands and every loaded
     * container of every world. Classic servers only - on Folia the
     * cross-region reads are not safe and the caller skips this.
     */
    public static List<String> findInWorlds(TimedItemPlugin plugin, Predicate<ItemStack> matcher) {
        PluginConfig config = plugin.pluginConfig();
        List<String> hits = new ArrayList<String>();
        for (World world : Bukkit.getWorlds()) {
            for (Item entity : world.getEntitiesByClass(Item.class)) {
                addGroundHit(hits, config, matcher, entity.getItemStack(), entity.getLocation());
            }
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                addGroundHit(hits, config, matcher, frame.getItem(), frame.getLocation());
            }
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (stand.getEquipment() == null) {
                    continue;
                }
                for (ItemStack piece : stand.getEquipment().getArmorContents()) {
                    addGroundHit(hits, config, matcher, piece, stand.getLocation());
                }
                addGroundHit(hits, config, matcher, stand.getEquipment().getItemInMainHand(),
                        stand.getLocation());
                addGroundHit(hits, config, matcher, stand.getEquipment().getItemInOffHand(),
                        stand.getLocation());
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof InventoryHolder)) {
                        continue;
                    }
                    ItemStack[] contents = ((InventoryHolder) state).getInventory().getContents();
                    for (int slot = 0; slot < contents.length; slot++) {
                        ItemStack item = contents[slot];
                        if (!hitIn(item, config, matcher)) {
                            continue;
                        }
                        hits.add(PluginConfig.color(config.getFindHitContainer()
                                .replace("{world}", world.getName())
                                .replace("{x}", String.valueOf(state.getX()))
                                .replace("{y}", String.valueOf(state.getY()))
                                .replace("{z}", String.valueOf(state.getZ()))
                                .replace("{type}", state.getType().name())));
                    }
                }
            }
            if (hits.size() >= MAX_HITS) {
                break;
            }
        }
        return hits;
    }

    private static void addGroundHit(List<String> hits, PluginConfig config,
                                     Predicate<ItemStack> matcher, ItemStack item, Location at) {
        if (!hitIn(item, config, matcher)) {
            return;
        }
        hits.add(PluginConfig.color(config.getFindHitGround()
                .replace("{world}", at.getWorld() == null ? "?" : at.getWorld().getName())
                .replace("{x}", String.valueOf(at.getBlockX()))
                .replace("{y}", String.valueOf(at.getBlockY()))
                .replace("{z}", String.valueOf(at.getBlockZ()))));
    }
}
