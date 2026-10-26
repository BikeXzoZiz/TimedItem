package com.ssomar.executableitems;

import com.ssomar.executableitems.executableitems.ExecutableItemsAPI;
import org.bukkit.plugin.java.JavaPlugin;

/** Fake ExecutableItems main; registers the default items. */
public class ExecutableItemsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ExecutableItemsAPI.registerDefaults();
        getLogger().info("Fake ExecutableItems ready (gkit)");
    }
}
