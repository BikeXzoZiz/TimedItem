package me.albert.timeditem;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns particles across server versions: Particle changed from an enum
 * to keyed static fields in newer APIs, so the configured constant is
 * resolved via reflection (which works for both shapes) with an automatic
 * fallback to FLAME and a silent no-op when neither exists. Resolutions
 * (including unknown names) are cached - names come from config and are
 * stable, and this runs once per expiry with a viewer.
 */
public final class ParticleCompat {

    private static final Class<?> PARTICLE_CLASS = findParticleClass();
    private static final Map<String, Optional<Particle>> BY_NAME = new ConcurrentHashMap<String, Optional<Particle>>();

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

    /** Package private for tests. */
    static Particle resolve(String name) {
        Particle particle = byName(name);
        if (particle != null) {
            return particle;
        }
        // cached like any other name, so FLAME is only resolved once
        return byName("FLAME");
    }

    private static Particle byName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String key = name.trim().toUpperCase(Locale.ROOT);
        Optional<Particle> cached = BY_NAME.get(key);
        if (cached != null) {
            return cached.orElse(null);
        }
        if (PARTICLE_CLASS == null) {
            return null;
        }
        try {
            Particle particle = (Particle) PARTICLE_CLASS.getField(key).get(null);
            BY_NAME.put(key, Optional.of(particle));
            return particle;
        } catch (ReflectiveOperationException | RuntimeException e) {
            BY_NAME.put(key, Optional.<Particle>empty());
            return null;
        }
    }

    private static Class<?> findParticleClass() {
        try {
            return Class.forName("org.bukkit.Particle");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
