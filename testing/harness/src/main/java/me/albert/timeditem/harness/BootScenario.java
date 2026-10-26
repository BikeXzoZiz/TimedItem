package me.albert.timeditem.harness;

import me.albert.timeditem.TimedItemPlugin;
import org.bukkit.Bukkit;

/**
 * Sanity checks right after boot: TimedItem is enabled and its public
 * services exist.
 */
final class BootScenario implements Scenario {

    @Override
    public String name() {
        return "boot";
    }

    @Override
    public String skipReason(TimedItemPlugin plugin) {
        return null;
    }

    @Override
    public int timeoutSeconds() {
        return 5;
    }

    @Override
    public void start(Context ctx) {
        TimedItemPlugin plugin = ctx.plugin();
        ctx.check(plugin.isEnabled(), "TimedItem enabled");
        ctx.check(plugin.pluginConfig() != null, "config loaded");
        ctx.check(plugin.hooks() != null, "hook manager created");
        ctx.check(plugin.expiryService() != null, "expiry service created");
        // platform diagnostics: the flags the periodic scenarios depend on
        String mmoState;
        try {
            // replicate the adapter's probe through TimedItem's classloader
            ClassLoader tl = plugin.getClass().getClassLoader();
            Class<?> mmo = tl.loadClass("net.Indyuce.mmoitems.MMOItems");
            Class<?> nbt = tl.loadClass("io.lumine.mythiclib.api.item.NBTItem");
            mmo.getField("plugin").get(null);
            nbt.getMethod("get", org.bukkit.inventory.ItemStack.class);
            nbt.getMethod("hasTag", String.class);
            nbt.getMethod("getString", String.class);
            mmoState = "adapter-probe=ok";
        } catch (Throwable t) {
            mmoState = "adapter-probe failed: " + t;
        }
        try {
            // call the ADAPTER's own lookup path (HookReflection.findClass)
            ClassLoader tl = plugin.getClass().getClassLoader();
            Class<?> hr = tl.loadClass("me.albert.timeditem.hook.HookReflection");
            java.lang.reflect.Method findClass = hr.getDeclaredMethod("findClass", String[].class);
            findClass.setAccessible(true);
            Object viaHook = findClass.invoke(null, (Object)
                    new String[]{"io.lumine.mythiclib.api.item.NBTItem"});
            Object direct = null;
            try {
                direct = Class.forName("io.lumine.mythiclib.api.item.NBTItem");
            } catch (Throwable t) {
                direct = "forName failed: " + t;
            }
            Object resource = tl.getResource("io/lumine/mythiclib/api/item/NBTItem.class");
            ctx.note("nbt probe: hookReflection=" + viaHook
                    + " harnessForName=" + direct
                    + " timedItemLoaderResource=" + (resource != null ? "visible" : "MISSING"));
        } catch (Throwable t) {
            ctx.note("nbt probe failed: " + t);
        }
        ctx.note("flags: scanWorldItems=" + plugin.pluginConfig().isScanWorldItems()
                + " containerScan=" + plugin.pluginConfig().isContainerScanEnabled()
                + "/" + plugin.pluginConfig().getContainerScanMode()
                + " hologram=" + plugin.pluginConfig().isHologramEnabled()
                + " transform=" + plugin.pluginConfig().isExpiryTransformEnabled()
                + " hologramCompat=" + me.albert.timeditem.HologramCompat.isAvailable()
                + " folia=" + F.isFolia()
                + " mmoitems[" + mmoState + "]");
        // live view of every adapter: isAvailable NOW + its cached probes
        try {
            java.lang.reflect.Field field = plugin.hooks().getClass().getDeclaredField("itemAdapters");
            field.setAccessible(true);
            for (Object adapter : (java.util.List<?>) field.get(plugin.hooks())) {
                java.lang.reflect.Method id = adapter.getClass().getMethod("id");
                id.setAccessible(true);
                java.lang.reflect.Method isAvail = adapter.getClass().getMethod("isAvailable");
                isAvail.setAccessible(true);
                String name = String.valueOf(id.invoke(adapter));
                boolean available = (Boolean) isAvail.invoke(adapter);
                StringBuilder probes = new StringBuilder();
                for (java.lang.reflect.Field f : adapter.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object value = f.get(adapter);
                    if (f.getName().contains("plugin") || f.getName().contains("nbt")
                            || f.getName().contains("customStack") || f.getName().contains("items")
                            || f.getName().contains("registry") || f.getName().contains("Class")
                            || f.getName().contains("api")) {
                        probes.append(" ").append(f.getName()).append("=")
                                .append(value == null ? "NULL" : "set");
                    }
                }
                ctx.note("adapter " + name + ": isAvailableNow=" + available + probes);
            }
        } catch (Throwable t) {
            ctx.note("adapter probe failed: " + t);
        }
        if (!F.isFolia()) {
            ctx.note("world entity count: " + ctx.base().getWorld().getEntities().size());
        }
        ctx.finish("platform=" + Bukkit.getName() + " " + Bukkit.getBukkitVersion()
                + " hooks=[" + plugin.hooks().summary() + "]");
    }
}
