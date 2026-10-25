package me.albert.timeditem;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Spawns particles across server versions: Particle changed from an enum
 * to keyed static fields in newer APIs, so the configured constant is
 * resolved via reflection (which works for both shapes) with an automatic
 * fallback to FLAME and a silent no-op when neither exists.
 */
public final class ParticleCompat {

    private static Particle fallback;
    private static boolean fallbackResolved;

    private ParticleCompat() {
    }

    /** Spawns the particle around the player. */
    public static void spawn(Player player, String name, int count) {
        spawnAt(player, player.getLocation().add(0.0, 1.0, 0.0), name, count);
    }

    /** Spawns the particle around an arbitrary location (e.g. a ground item). */
    public static void spawnAt(Player viewer, Location center, String name, int count) {
        Particle particle = resolve(name);
        if (particle == null) {
            return;
        }
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(particle, center, count, 0.4, 0.4, 0.4);
    }

    private static Particle resolve(String name) {
        Particle particle = byName(name);
        if (particle != null) {
            return particle;
        }
        if (!fallbackResolved) {
            fallback = byName("FLAME");
            fallbackResolved = true;
        }
        return fallback;
    }

    private static Particle byName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Object value = particleClass.getField(name.trim().toUpperCase(Locale.ROOT)).get(null);
            return (Particle) value;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
