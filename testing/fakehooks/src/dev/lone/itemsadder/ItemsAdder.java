package dev.lone.itemsadder;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Fake ItemsAdder main; registers the default custom stacks. */
public class ItemsAdder extends JavaPlugin {

    @Override
    public void onEnable() {
        CustomStack.registerDefaults();
        getLogger().info("Fake ItemsAdder ready (iasurvival:ruby_sword)");
    }
}
