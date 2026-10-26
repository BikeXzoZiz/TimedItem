package me.albert.timeditem.hook;

import me.albert.timeditem.DebugLog;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * CrazyCrates (Crazy-Crew) prize lookup for /ti give
 * crazycrates:&lt;crate&gt;:&lt;prize&gt;. The plugin was rewritten between
 * major versions (badbones69 -> ryderbelserion packages, renamed
 * managers), so nothing is matched by exact name: the crate manager,
 * the Crate and the Prize are located by the simple name of their
 * return type instead.
 *
 * Prizes carry no tag on the given stack, so {@link #itemId} cannot
 * identify one after the fact - this adapter only builds.
 */
final class CrazyCratesAdapter implements CustomItemAdapter {

    private final Class<?> pluginClass;
    private volatile boolean probed;
    private volatile Object pluginInstance;
    private volatile Method getManager;
    private volatile Method getCrate;
    private volatile Method getPrize;

    CrazyCratesAdapter() {
        this.pluginClass = HookReflection.findClass(
                "com.badbones69.crazycrates.CrazyCrates",
                "com.ryderbelserion.crazycrates.paper.CrazyCrates",
                "com.ryderbelserion.crazycrates.CrazyCrates");
    }

    @Override
    public String id() {
        return "crazycrates";
    }

    @Override
    public java.util.List<String> aliases() {
        return java.util.Arrays.asList("cc", "crate");
    }

    @Override
    public boolean isAvailable() {
        return probe();
    }

    @Override
    public String itemId(ItemStack item) {
        return null; // prize stacks are plain vanilla, nothing to read
    }

    @Override
    public ItemStack build(String... idParts) {
        if (idParts.length != 2 || idParts[0].isEmpty() || idParts[1].isEmpty()) {
            return null; // needs crate:prize
        }
        if (!probe()) {
            return null;
        }
        try {
            Object manager = getManager == null || pluginInstance == null
                    ? null : HookReflection.invoke(getManager, pluginInstance);
            Object crate = manager == null ? null : HookReflection.invoke(getCrate, manager, idParts[0]);
            if (crate == null) {
                return null;
            }
            Method prizeMethod = getPrize;
            if (prizeMethod == null) {
                // the probe crate name may not exist - resolve from the real one
                prizeMethod = HookReflection.findByReturn(crate.getClass(), "Prize", 1);
                if (prizeMethod != null) {
                    getPrize = prizeMethod;
                }
            }
            Object prize = prizeMethod == null ? null : HookReflection.invoke(prizeMethod, crate, idParts[1]);
            Method getItem = HookReflection.findGetter(prize.getClass(), "item", ItemStack.class);
            Object stack = getItem == null ? null : HookReflection.invoke(getItem, prize);
            return stack instanceof ItemStack ? ((ItemStack) stack).clone() : null;
        } catch (Throwable t) {
            DebugLog.verbose("CrazyCrates build failed: " + t);
            return null;
        }
    }

    /** Resolves the instance and method chain once; false on a version mismatch. */
    private boolean probe() {
        if (probed) {
            return pluginInstance != null;
        }
        synchronized (this) {
            if (probed) {
                return pluginInstance != null;
            }
            try {
                Object plugin = staticInstance();
                if (plugin != null) {
                    Method manager = managerAccessor(plugin);
                    if (manager != null) {
                        Object managerObj = HookReflection.invoke(manager, plugin);
                        Method crate = managerObj == null
                                ? null : HookReflection.findByReturn(managerObj.getClass(), "Crate", 1);
                        Method prize = null;
                        if (crate != null) {
                            Object crateObj = HookReflection.invoke(crate, managerObj, "probe");
                            if (crateObj != null) {
                                prize = HookReflection.findByReturn(crateObj.getClass(), "Prize", 1);
                            }
                        }
                        this.pluginInstance = plugin;
                        this.getManager = manager;
                        this.getCrate = crate;
                        this.getPrize = prize;
                    }
                }
            } catch (Throwable t) {
                DebugLog.verbose("CrazyCrates probe failed: " + t);
            }
            probed = true;
            return pluginInstance != null;
        }
    }

    /** The plugin singleton: getInstance() on Java builds, INSTANCE on Kotlin ones. */
    private Object staticInstance() {
        try {
            Method method = HookReflection.findMethod(pluginClass, "getInstance");
            if (method != null) {
                return method.invoke(null);
            }
            Field field = pluginClass.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /** The getter that hands out the crate manager, by any common name. */
    private Method managerAccessor(Object plugin) {
        for (String name : new String[]{"getCrateManager", "getManager", "getApi", "getApiManager"}) {
            Method method = HookReflection.findMethod(plugin.getClass(), name);
            if (method != null && method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }
}
