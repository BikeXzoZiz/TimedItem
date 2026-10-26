package io.lumine.mythiclib;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fake MythicLib main. MythicLib is a separate plugin in production and
 * TimedItem softdepends it by name, so the fake environment ships it as
 * its own plugin holding the io.lumine.mythiclib API classes.
 */
public class MythicLib extends JavaPlugin {

    public static MythicLib plugin;

    @Override
    public void onEnable() {
        plugin = this;
        // touch the API class the TimedItem adapter probes, through this
        // plugin's own loader (mirrors how the real MythicLib loads its API)
        io.lumine.mythiclib.api.item.NBTItem.get(null);
        getLogger().info("Fake MythicLib ready (NBT API)");
    }
}
