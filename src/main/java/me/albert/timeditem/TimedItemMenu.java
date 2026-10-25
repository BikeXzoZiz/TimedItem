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
import java.util.Arrays;
import java.util.List;

/**
 * The /ti menu GUI: pick a preset duration for the held item, or
 * pause/resume/remove it, with a live status display. Uses only
 * materials whose enum names are stable across 1.13 - 26.x
 * (PAPER, ENCHANTED_BOOK, BLAZE_POWDER, TNT, BARRIER,
 * GRAY_STAINED_GLASS_PANE).
 */
public final class TimedItemMenu implements Listener {

    private static final long[] PRESETS = {60L, 300L, 1800L, 3600L, 21600L, 86400L, 604800L};
    private static final int[] PRESET_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int STATUS_SLOT = 4;
    private static final int TOGGLE_SLOT = 20;
    private static final int REMOVE_SLOT = 22;
    private static final int CLOSE_SLOT = 24;

    private final TimedItemPlugin plugin;

    public TimedItemMenu(TimedItemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Marker holder so our event handlers can recognise the menu inventory. */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, 27, ChatColorWrap.color("&6TimedItem Menu"));
        holder.inventory = inventory;
        render(inventory, player);
        player.openInventory(inventory);
    }

    private void render(Inventory inventory, Player player) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, "&7", null);
        for (int slot = 0; slot < 27; slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(STATUS_SLOT, statusItem(player));
        PluginConfig config = plugin.pluginConfig();
        for (int i = 0; i < PRESETS.length; i++) {
            String label = TimeLore.formatDuration(PRESETS[i], config);
            inventory.setItem(PRESET_SLOTS[i], item(Material.PAPER,
                    "&e" + label,
                    Arrays.asList("&7Set this lifetime on", "&7the item in your hand")));
        }
        inventory.setItem(TOGGLE_SLOT, item(Material.BLAZE_POWDER, "&6Pause / Resume",
                Arrays.asList("&7Freeze the countdown,", "&7or resume a paused item")));
        inventory.setItem(REMOVE_SLOT, item(Material.TNT, "&cRemove timer",
                Arrays.asList("&7Strip the timer from", "&7the item in your hand")));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "&cClose", null));
    }

    private ItemStack statusItem(Player player) {
        PluginConfig config = plugin.pluginConfig();
        TimeLore.TimerInfo info = TimeLore.analyze(player.getInventory().getItemInMainHand(), config);
        List<String> lore = new ArrayList<String>();
        if (info == null) {
            lore.add("&7Held item: &cno timer");
        } else if (info.getType() == TimeLore.Type.EXPIRE && info.getExpiry() != null) {
            long remaining = java.time.Duration.between(java.time.LocalDateTime.now(), info.getExpiry()).getSeconds();
            String pretty = remaining > 0 ? TimeLore.formatDuration(Math.max(0L, remaining), config) : "0" + config.getUnitS();
            lore.add("&7Type: &eexpire");
            lore.add("&7Remaining: &e" + pretty);
            lore.add("&7Expires at: &e" + TimeLore.LORE_FORMAT.format(info.getExpiry()));
        } else if (info.getType() == TimeLore.Type.PAUSED) {
            lore.add("&7Type: &epaused");
            lore.add("&7Remaining: &e" + TimeLore.formatDuration(Math.max(0L, info.getSeconds()), config));
        } else {
            lore.add("&7Type: &elifetime (not started)");
            lore.add("&7Duration: &e" + TimeLore.formatDuration(Math.max(0L, info.getSeconds()), config));
        }
        return item(Material.ENCHANTED_BOOK, "&6Held item status", lore);
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
        int slot = event.getSlot();
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == TOGGLE_SLOT) {
            toggle(player);
            refresh(event.getInventory(), player);
            return;
        }
        if (slot == REMOVE_SLOT) {
            removeTimer(player);
            refresh(event.getInventory(), player);
            return;
        }
        for (int i = 0; i < PRESET_SLOTS.length; i++) {
            if (PRESET_SLOTS[i] == slot) {
                applyPreset(player, PRESETS[i]);
                refresh(event.getInventory(), player);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private void applyPreset(Player player, long seconds) {
        PluginConfig config = plugin.pluginConfig();
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        if (TimeLore.isBlacklisted(item, config)) {
            player.sendMessage(config.getPrefix() + config.getMaterialBlacklisted());
            return;
        }
        TimeLore.applyLifetime(item, config, seconds);
        if (!config.isStartDateCalculateOnClick()) {
            TimeLore.startLifetime(item, config, player);
        }
        player.sendMessage(config.getPrefix() + config.getTimeSet()
                .replace("{time}", TimeLore.formatDuration(seconds, config)));
    }

    private void toggle(Player player) {
        PluginConfig config = plugin.pluginConfig();
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        TimeLore.TimerInfo info = TimeLore.analyze(item, config);
        if (info == null) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        if (info.getType() == TimeLore.Type.PAUSED) {
            long seconds = info.getSeconds();
            if (seconds <= 0) {
                player.sendMessage(config.getPrefix() + config.getNoTimer());
                return;
            }
            TimeLore.applyExpiry(item, config, seconds);
            player.sendMessage(config.getPrefix() + config.getTimerResumed()
                    .replace("{time}", TimeLore.formatDuration(seconds, config)));
            return;
        }
        if (info.getType() != TimeLore.Type.EXPIRE || info.getExpiry() == null) {
            player.sendMessage(config.getPrefix() + config.getTimerNotStarted());
            return;
        }
        long remaining = java.time.Duration.between(java.time.LocalDateTime.now(), info.getExpiry()).getSeconds();
        if (remaining <= 0) {
            player.sendMessage(config.getPrefix() + config.getNoTimer());
            return;
        }
        TimeLore.applyPaused(item, config, remaining);
        player.sendMessage(config.getPrefix() + config.getTimerPaused()
                .replace("{time}", TimeLore.formatDuration(remaining, config)));
    }

    private void removeTimer(Player player) {
        PluginConfig config = plugin.pluginConfig();
        ItemStack item = mainHand(player, config);
        if (item == null) {
            return;
        }
        TimeLore.removeTimer(item, config);
        player.sendMessage(config.getPrefix() + config.getItemCleared());
    }

    private ItemStack mainHand(Player player, PluginConfig config) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(config.getPrefix() + config.getNoItem());
            return null;
        }
        return item;
    }

    private void refresh(Inventory inventory, Player player) {
        ItemStack held = inventory.getItem(STATUS_SLOT);
        if (held != null && held.getType() == Material.ENCHANTED_BOOK) {
            inventory.setItem(STATUS_SLOT, statusItem(player));
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColorWrap.color(name));
            if (lore != null) {
                List<String> colored = new ArrayList<String>();
                for (String line : lore) {
                    colored.add(ChatColorWrap.color(line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** ChatColor is referenced through a wrapper to keep imports tidy. */
    private static final class ChatColorWrap {
        static String color(String text) {
            return PluginConfig.color(text);
        }
    }
}
