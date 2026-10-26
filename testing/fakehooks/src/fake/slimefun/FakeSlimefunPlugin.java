package fake.slimefun;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fake Slimefun main. The main class deliberately lives outside the
 * real Slimefun4 package: Paper's remapper recognizes the real main
 * class name and applies Slimefun4's official mappings, which relocates
 * classes inside the jar and breaks plugin loading.
 */
public class FakeSlimefunPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        SlimefunItem.registerDefaults();
        getLogger().info("Fake Slimefun ready (test_item)");
    }
}
