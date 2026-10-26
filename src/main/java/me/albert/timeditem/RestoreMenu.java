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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The /ti restore GUI: pages of recently expired items, newest first.
 * Clicking an entry hands the item back to the admin and removes it
 * from the recovery log. Same stable-material approach as TimedItemMenu.
 */
public final class RestoreMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final TimedItemPlugin plugin;

    public RestoreMenu(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Marker holder so our event handlers can recognise the menu inventory. */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private int page;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public void open(Player player, int page) {
        RecoveryStore store = plugin.recoveryStore();
        Holder holder = new Holder();
        holder.page = Math.max(0, Math.min(page, store.pageCount(PAGE_SIZE) - 1));
        Inventory inventory = Bukkit.createInventory(holder, 54,
                plugin.pluginConfig().getGuiRestoreTitle()
                        .replace("{page}", String.valueOf(holder.page + 1))
                        .replace("{pages}", String.valueOf(store.pageCount(PAGE_SIZE))));
        holder.inventory = inventory;
        render(inventory, holder.page);
        player.openInventory(inventory);
    }

    private void render(Inventory inventory, int page) {
        PluginConfig config = plugin.pluginConfig();
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, "&7", null);
        for (int slot = 0; slot < 54; slot++) {
            inventory.setItem(slot, filler);
        }
        List<RecoveryStore.Entry> entries = plugin.recoveryStore().page(PAGE_SIZE, page);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int slot = 0; slot < entries.size(); slot++) {
            RecoveryStore.Entry entry = entries.get(slot);
            if (entry.getItem() == null) {
                continue;
            }
            ItemStack display = entry.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<String>();
                if (meta.hasLore() && meta.getLore() != null) {
                    lore.addAll(meta.getLore());
                }
                lore.add(config.getGuiRestoreDate()
                        .replace("{date}", format.format(new Date(entry.getTimeMillis()))));
                lore.add(config.getGuiRestorePlayer()
                        .replace("{player}", entry.getPlayer() == null ? "-" : entry.getPlayer()));
                lore.add(config.getGuiRestoreWorld().replace("{world}", entry.getWorld()));
                lore.add(config.getGuiRestoreClick());
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
        }
        if (page > 0) {
            inventory.setItem(PREV_SLOT, item(Material.ARROW, config.getGuiRestorePrev(), null));
        }
        if (page < plugin.recoveryStore().pageCount(PAGE_SIZE) - 1) {
            inventory.setItem(NEXT_SLOT, item(Material.ARROW, config.getGuiRestoreNext(), null));
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
        Player player = (Player) event.getWhoClicked();
        Holder holder = (Holder) event.getInventory().getHolder();
        int slot = event.getSlot();
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == PREV_SLOT) {
            open(player, holder.page - 1);
            return;
        }
        if (slot == NEXT_SLOT) {
            open(player, holder.page + 1);
            return;
        }
        if (slot >= PAGE_SIZE) {
            return;
        }
        RecoveryStore store = plugin.recoveryStore();
        List<RecoveryStore.Entry> entries = store.page(PAGE_SIZE, holder.page);
        if (slot >= entries.size()) {
            return;
        }
        RecoveryStore.Entry entry = entries.get(slot);
        ItemStack item = entry.getItem();
        if (item == null) {
            return;
        }
        store.remove(entry);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
        player.sendMessage(plugin.pluginConfig().getPrefix() + plugin.pluginConfig().getRestoreGiven());
        plugin.storage().appendAudit(new me.albert.timeditem.storage.AuditRecord(
                System.currentTimeMillis(), player.getUniqueId().toString(), player.getName(),
                me.albert.timeditem.storage.AuditRecord.RESTORE,
                entry.getPlayer() == null ? "-" : entry.getPlayer(),
                PdcCompat.readUid(item),
                plugin.hooks().itemId(item) == null ? item.getType().name() : plugin.hooks().itemId(item),
                entry.getWorld()));
        player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return PluginConfig.color(text);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) {
                List<String> colored = new ArrayList<String>();
                for (String line : lore) {
                    colored.add(color(line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
