package io.th0rgal.oraxen;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.plugin.java.JavaPlugin;

/** Fake Oraxen main; registers the default custom items. */
public class OraxenPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        OraxenItems.registerDefaults();
        getLogger().info("Fake Oraxen ready (ruby_sword)");
    }
}
