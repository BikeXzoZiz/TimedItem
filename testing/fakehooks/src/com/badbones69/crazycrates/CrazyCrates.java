package com.badbones69.crazycrates;

import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.managers.CrateManager;
import fake.crazycrates.Bridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fake CrazyCrates main (the badbones69 layout the adapter probes
 * first): getInstance() + getCrateManager() + `fakecc fire` for the
 * auto-timer hook.
 */
public class CrazyCrates extends JavaPlugin {

    private static CrazyCrates instance;
    private CrateManager crateManager;

    public static CrazyCrates getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        crateManager = new CrateManager();
        crateManager.registerDefaults();
        getLogger().info("Fake CrazyCrates ready (crate basic, prize prize)");
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("fire")) {
            ItemStack item = Bridge.nextItem;
            if (item == null) {
                item = new ItemStack(Material.GOLD_INGOT);
            }
            PlayerPrizeEvent event = new PlayerPrizeEvent(item);
            Bukkit.getPluginManager().callEvent(event);
            Bridge.results.add(event.getItemStack());
            return true;
        }
        return false;
    }
}
