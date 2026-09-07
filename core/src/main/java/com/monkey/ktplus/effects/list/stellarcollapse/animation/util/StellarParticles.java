package com.monkey.ktplus.effects.list.stellarcollapse.animation.util;

import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import org.bukkit.Color;
import org.bukkit.Location;

public final class StellarParticles {
    private StellarParticles() {}

    public static void spawnStellarSwirl(VisualEffectService visuals, Location center, double radius, int points) {
        int scaledPoints = ParticleScale.scale(points);
        for (int i = 0; i < scaledPoints; i++) {
            double angle = 2 * Math.PI * i / scaledPoints;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.sin(angle * 3) * 0.5;
            Location particleLoc = center.clone().add(x, y, z);
            visuals.particle("DUST", particleLoc, 1, 0, 0, 0, 0, Color.WHITE);
            visuals.particle("DRAGON_BREATH", particleLoc, 1, 0.05, 0.05, 0.05, 0.0, null);
        }
    }
}
