package me.albert.timeditem;

import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Particle resolution runs against the spigot-api enum, which is
 * server-independent: valid names resolve case-insensitively, anything
 * else (including blank) falls back to FLAME. Resolutions are cached per
 * name, so a configured particle is only looked up reflectively once.
 */
class ParticleCompatTest {

    @Test
    void resolvesKnownNamesCaseInsensitively() {
        assertSame(Particle.FLAME, ParticleCompat.resolve("FLAME"));
        assertSame(Particle.FLAME, ParticleCompat.resolve("flame"));
        assertSame(Particle.FLAME, ParticleCompat.resolve(" Flame "));
        assertSame(Particle.HEART, ParticleCompat.resolve("HEART"));
    }

    @Test
    void unknownAndBlankNamesFallBackToFlame() {
        assertSame(Particle.FLAME, ParticleCompat.resolve("not_a_particle"));
        assertSame(Particle.FLAME, ParticleCompat.resolve(null));
        assertSame(Particle.FLAME, ParticleCompat.resolve(""));
        assertSame(Particle.FLAME, ParticleCompat.resolve("   "));
    }

    @Test
    void repeatedResolutionsAreStable() {
        Particle first = ParticleCompat.resolve("SMOKE_NORMAL");
        for (int i = 0; i < 3; i++) {
            assertSame(first, ParticleCompat.resolve("SMOKE_NORMAL"));
        }
        // an unknown name keeps falling back after its negative cache entry
        assertSame(Particle.FLAME, ParticleCompat.resolve("still_unknown"));
        assertSame(Particle.FLAME, ParticleCompat.resolve("still_unknown"));
    }
}
