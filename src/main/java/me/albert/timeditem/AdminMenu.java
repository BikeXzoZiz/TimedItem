package me.albert.timeditem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The /ti admin GUI (3.0+): an overview of every timed item the last
 * periodic scan saw, filterable by player or item. Left-click expires
 * the item now, right-click removes its timer - both run on the owner's
 * region thread, so an entry only reacts while the owner is online and
 * the item still sits in the recorded slot (otherwise nothing happens).
 */
public final class AdminMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final TimedItemPlugin plugin;
    /** Filter typed per admin, kept for page navigation. */
    private final Map<UUID, String> filters = new ConcurrentHashMap<UUID, String>();

    public AdminMenu(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private int page;
        private String filter;
        private List<TrackedItems.Tracked> entries;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public void open(Player admin, int page, String filter) {
        PluginConfig config = plugin.pluginConfig();
        String clean = filter == null ? "" : filter.trim();
        filters.put(admin.getUniqueId(), clean);
        List<TrackedItems.Tracked> entries = plugin.trackedItems().snapshot(clean);
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        Holder holder = new Holder();
        holder.page = page;
        holder.filter = clean;
        holder.entries = new ArrayList<TrackedItems.Tracked>(entries.subList(
                page * PAGE_SIZE, Math.min(entries.size(), (page + 1) * PAGE_SIZE)));
        String title = config.getGuiAdminTitle()
                .replace("{page}", String.valueOf(page + 1))
                .replace("{pages}", String.valueOf(pages));
        if (!clean.isEmpty()) {
            title += " &8(" + clean + ")";
        }
        Inventory inventory = Bukkit.createInventory(holder, 54, PluginConfig.color(title));
        holder.inventory = inventory;
        render(inventory, page, pages, holder.entries, config);
        admin.openInventory(inventory);
    }

    private void render(Inventory inventory, int page, int pages,
                        List<TrackedItems.Tracked> entries, PluginConfig config) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, "&7", null);
        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, filler);
        }
        for (int slot = 0; slot < entries.size(); slot++) {
            TrackedItems.Tracked entry = entries.get(slot);
            Material material = Material.matchMaterial(entry.material);
            ItemStack display = item(material == null ? Material.BARRIER : material,
                    "&e" + entry.itemId,
                    new String[]{
                            config.getGuiAdminOwner().replace("{player}", entry.ownerName),
                            config.getGuiAdminType()
                                    .replace("{type}", entry.typeName)
                                    .replace("{time}", TimeLore.formatDuration(
                                            Math.max(0L, entry.remainingSeconds), config)),
                            config.getGuiAdminExpireNow(),
                            config.getGuiAdminRemoveTimer()
                    });
            inventory.setItem(slot, display);
        }
        if (entries.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, config.getGuiAdminEmpty(), null));
        }
        if (page > 0) {
            inventory.setItem(PREV_SLOT, item(Material.ARROW, config.getGuiAdminPrev(), null));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT_SLOT, item(Material.ARROW, config.getGuiAdminNext(), null));
        }
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, config.getGuiCloseName(), null));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)
                || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }
        Player admin = (Player) event.getWhoClicked();
        Holder holder = (Holder) event.getInventory().getHolder();
        int slot = event.getSlot();
        if (slot == CLOSE_SLOT) {
            admin.closeInventory();
            return;
        }
        if (slot == PREV_SLOT) {
            open(admin, holder.page - 1, holder.filter);
            return;
        }
        if (slot == NEXT_SLOT) {
            open(admin, holder.page + 1, holder.filter);
            return;
        }
        if (slot >= PAGE_SIZE || holder.entries == null || slot >= holder.entries.size()) {
            return;
        }
        TrackedItems.Tracked entry = holder.entries.get(slot);
        Player owner;
        try {
            owner = Bukkit.getPlayer(UUID.fromString(entry.ownerUuid));
        } catch (IllegalArgumentException e) {
            owner = null;
        }
        if (owner == null) {
            admin.sendMessage(plugin.pluginConfig().getPrefix()
                    + plugin.pluginConfig().getTargetNotFound()
                    .replace("{player}", entry.ownerName));
            return;
        }
        final boolean expire = event.isLeftClick();
        final int itemSlot = entry.slot;
        final PluginConfig config = plugin.pluginConfig();
        final Player ownerPlayer = owner;
        SchedulerCompat.runAtPlayer(plugin, ownerPlayer, () -> {
            if (!ownerPlayer.isOnline()) {
                return;
            }
            ItemStack item = ownerPlayer.getInventory().getItem(itemSlot);
            if (item == null || TimeLore.analyze(item, config) == null) {
                return; // the item moved or expired meanwhile
            }
            if (expire) {
                ownerPlayer.getInventory().setItem(itemSlot, null);
                plugin.expiryService().expire(item, ownerPlayer, ownerPlayer.getLocation(),
                        ownerPlayer.getWorld(), () -> {
                        }, false, config);
                admin.sendMessage(config.getPrefix() + config.getGuiAdminExpired()
                        .replace("{player}", ownerPlayer.getName()));
            } else {
                TimeLore.removeTimer(item, config);
                admin.sendMessage(config.getPrefix() + config.getGuiAdminRemoved()
                        .replace("{player}", ownerPlayer.getName()));
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private ItemStack item(Material material, String name, String[] lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PluginConfig.color(name));
            if (lore != null) {
                List<String> lines = new ArrayList<String>();
                for (String line : lore) {
                    lines.add(PluginConfig.color(line));
                }
                meta.setLore(lines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
