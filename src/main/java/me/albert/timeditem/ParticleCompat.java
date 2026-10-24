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

    public static void spawn(Player player, String name, int count) {
        Particle particle = resolve(name);
        if (particle == null) {
            return;
        }
        World world = player.getWorld();
        Location center = player.getLocation().add(0.0, 1.0, 0.0);
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
