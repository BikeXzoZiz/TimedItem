package com.nisovin.shopkeepers;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import fake.shopkeepers.Bridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fake Shopkeepers main. `fakesh fire` turns Bridge.nextItem into a
 * trade result event; TimedItem's auto-timer handler runs during the
 * call and the post-event stack lands in Bridge.lastResult.
 */
public class ShopkeepersPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Fake Shopkeepers ready - /fakesh fire");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("fire")) {
            ItemStack item = Bridge.nextItem;
            if (item == null) {
                item = new ItemStack(Material.DIAMOND);
            }
            ShopkeeperTradeEvent event = new ShopkeeperTradeEvent(item);
            Bukkit.getPluginManager().callEvent(event);
            Bridge.results.add(event.getItemStack());
            getLogger().info("fakesh fire: next=" + item.getType()
                    + " postMeta=" + event.getItemStack().hasItemMeta()
                    + " postLore=" + (event.getItemStack().hasItemMeta()
                            && event.getItemStack().getItemMeta().hasLore()));
            return true;
        }
        return false;
    }
}
