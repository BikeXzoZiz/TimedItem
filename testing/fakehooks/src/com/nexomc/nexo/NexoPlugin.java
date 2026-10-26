package com.nexomc.nexo;

import com.nexomc.nexo.api.NexoItems;
import org.bukkit.plugin.java.JavaPlugin;

/** Fake Nexo main; registers the default custom items. */
public class NexoPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        NexoItems.registerDefaults();
        getLogger().info("Fake Nexo ready (ruby_sword)");
    }
}
