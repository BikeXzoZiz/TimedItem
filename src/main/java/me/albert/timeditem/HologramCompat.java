package me.albert.timeditem;

import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;

/**
 * TextDisplay spawn access through reflection (the compile target is
 * 1.13; display entities exist from 1.19.4). On older servers every
 * method no-ops and {@link #isAvailable()} is false - the hologram
 * feature simply stays off.
 */
public final class HologramCompat {

    private static volatile boolean attempted;
    private static volatile boolean available;
    private static Class<?> textDisplayClass;
    private static volatile Method spawnMethod;
    private static volatile Method setTextMethod;
    private static volatile Method removeMethod;

    private HologramCompat() {
    }

    public static void init() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
            spawnMethod = World.class.getMethod("spawn", Location.class, Class.class);
            setTextMethod = textDisplayClass.getMethod("setText", String.class);
            removeMethod = Class.forName("org.bukkit.entity.Entity").getMethod("remove");
            available = true;
        } catch (Throwable t) {
            available = false; // server without display entities (<1.19.4)
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Spawns a text display at the location; returns the entity or null
     * (unsupported server / spawn refused). The caller must be on the
     * thread that owns the location's region.
     */
    public static Object spawn(Location location, String text) {
        if (!available) {
            return null;
        }
        try {
            Object entity = spawnMethod.invoke(location.getWorld(), location, textDisplayClass);
            setText(entity, text);
            return entity;
        } catch (Throwable t) {
            DebugLog.verbose("Hologram spawn failed: " + t);
            return null;
        }
    }

    public static void setText(Object entity, String text) {
        if (!available || entity == null) {
            return;
        }
        try {
            setTextMethod.invoke(entity, text);
        } catch (Throwable ignored) {
            DebugLog.verbose("Hologram setText failed: " + ignored);
        }
    }

    public static void remove(Object entity) {
        if (!available || entity == null) {
            return;
        }
        try {
            removeMethod.invoke(entity);
        } catch (Throwable ignored) {
            DebugLog.verbose("Hologram remove failed: " + ignored);
        }
    }
}
