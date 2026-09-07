package com.monkey.ktplus.effects.list.aurafarming.animation.util;

import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import org.bukkit.Color;
import org.bukkit.Location;

public final class AuraParticles {
    private AuraParticles() {}

    public static void spawnThickWhiteSparkle(VisualEffectService visuals, Location loc) {
        visuals.particle("END_ROD", loc, ParticleScale.scale(10), 0.2, 0.2, 0.2, 0.01, null);
        visuals.particle("FIREWORK", loc, ParticleScale.scale(8), 0.2, 0.2, 0.2, 0.01, null);
        visuals.particle("DUST", loc, 1, 0, 0, 0, 0, Color.WHITE);
    }

    public static void spawnExplosionEffect(VisualEffectService visuals, Location loc, int tick) {
        double maxRadius = 10.0;
        double radius = Math.min(maxRadius, tick * 0.6);
        double height = radius / 2;
        visuals.particle("END_ROD", loc, ParticleScale.scale(100), radius, height, radius, 0.04, null);
        visuals.particle(
                "FIREWORK", loc.clone().add(0, height / 2, 0), ParticleScale.scale(80), radius * 1.2, height / 2, radius * 1.2, 0.02, null);
        visuals.particle("DUST", loc, 1, 0, 0, 0, 0, Color.fromRGB(200, 220, 255));
        for (int layer = 0; layer < 3; layer++) {
            double layerHeight = height * (layer - 1);
            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = (tick * 0.2) + (i * 2 * Math.PI / points);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location ringLoc = loc.clone().add(x, layerHeight, z);
                visuals.particle("DUST", ringLoc, 1, 0, 0, 0, 0, Color.fromRGB(240, 250, 255));
            }
        }
    }
}
