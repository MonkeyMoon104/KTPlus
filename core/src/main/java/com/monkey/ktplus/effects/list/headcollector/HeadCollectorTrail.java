package com.monkey.ktplus.effects.list.headcollector;

import com.monkey.ktplus.effects.support.particle.BukkitParticles;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public final class HeadCollectorTrail {
    static final String[] PARTICLE_TYPES = {
        "SOUL",
        "FLAME",
        "WITCH",
        "END_ROD",
        "ELECTRIC_SPARK",
        "DRAGON_BREATH",
        "GLOW",
        "SCULK_SOUL"
    };

    private HeadCollectorTrail() {}

    public static String particleForIndex(int index) {
        return PARTICLE_TYPES[Math.floorMod(index, PARTICLE_TYPES.length)];
    }

    static void emit(HeadCollectorHead head, Location center) {
        emitDense(head, center);
    }

    static void emitOrbit(HeadCollectorHead head, Location center, int intervalTicks) {
        head.trailCooldown++;
        if (head.trailCooldown < intervalTicks) {
            return;
        }
        head.trailCooldown = 0;
        spawnPoint(center, head.trailParticle);
        head.lastTrailLocation = center.clone();
    }

    static void emitDense(HeadCollectorHead head, Location center) {
        Location at = center.clone();
        if (head.lastTrailLocation != null && head.lastTrailLocation.getWorld() == at.getWorld()) {
            Vector delta = at.toVector().subtract(head.lastTrailLocation.toVector());
            double distance = delta.length();
            if (distance > 0.01) {
                Vector step = delta.multiply(1.0 / Math.max(1, Math.ceil(distance / 0.12)));
                Location cursor = head.lastTrailLocation.clone();
                while (cursor.distanceSquared(at) > step.lengthSquared()) {
                    cursor.add(step);
                    spawnPoint(cursor, head.trailParticle);
                }
            }
        }
        spawnPoint(at, head.trailParticle);
        head.lastTrailLocation = at.clone();
    }

    static void reset(HeadCollectorHead head) {
        head.lastTrailLocation = null;
        head.trailCooldown = 0;
    }

    private static void spawnPoint(Location location, String particle) {
        BukkitParticles.spawn(particle, location, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
