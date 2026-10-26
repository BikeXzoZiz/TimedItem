package com.willfp.ecoitems;

import com.willfp.ecoitems.items.EcoItem;
import com.willfp.ecoitems.items.EcoItems;
import org.bukkit.plugin.java.JavaPlugin;

/** Fake EcoItems main; registers the default items. */
public class EcoItemsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        EcoItems.registerDefaults();
        getLogger().info("Fake EcoItems ready (test_sword)");
    }
}
