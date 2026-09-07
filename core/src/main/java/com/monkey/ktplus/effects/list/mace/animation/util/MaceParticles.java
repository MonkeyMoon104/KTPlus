package com.monkey.ktplus.effects.list.mace.animation.util;

import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import org.bukkit.Color;
import org.bukkit.Location;

public final class MaceParticles {
    private MaceParticles() {}

    public static void spawnChargingSphere(VisualEffectService visuals, Location center, double radius, int points) {
        int scaled = ParticleScale.scale(points);
        for (int i = 0; i < scaled; i++) {
            double phi = Math.acos(1 - 2.0 * (i + 0.5) / scaled);
            double theta = Math.PI * (1 + Math.sqrt(5)) * (i + 0.5);
            double x = radius * Math.cos(theta) * Math.sin(phi);
            double y = radius * Math.sin(theta) * Math.sin(phi);
            double z = radius * Math.cos(phi);
            visuals.particle("DUST", center.clone().add(x, y, z), 1, 0, 0, 0, 0, Color.fromRGB(255, 215, 0));
        }
    }

    public static void spawnGoldenShockwave(VisualEffectService visuals, Location center) {
        for (int i = 0; i < 360; i += 8) {
            double radians = Math.toRadians(i);
            double x = Math.cos(radians) * 3;
            double z = Math.sin(radians) * 3;
            Location ringLoc = center.clone().add(x, 0.2, z);
            visuals.particle("FIREWORK", ringLoc, 3, 0.1, 0.1, 0.1, 0.02, null);
            visuals.particle("DUST", ringLoc, 1, 0, 0, 0, 0, Color.fromRGB(255, 223, 50));
        }
    }

    public static void spawnFinalBurst(VisualEffectService visuals, Location center) {
        visuals.particle("FLASH", center, 1, 0, 0, 0, 0, Color.fromRGB(255, 215, 0));
        visuals.particle("EXPLOSION", center, 1, 0, 0, 0, 0, null);
        Color gold = Color.fromRGB(255, 215, 0);
        for (int i = 0; i < ParticleScale.scale(80); i++) {
            double angle = Math.random() * Math.PI * 2;
            double y = (Math.random() - 0.5) * 2;
            double radius = Math.sqrt(Math.random()) * 4;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location pLoc = center.clone().add(x, y, z);
            visuals.particle("END_ROD", pLoc, 1, 0, 0, 0, 0.01, null);
            if (i % 3 == 0) {
                visuals.particle("DUST", pLoc, 1, 0, 0, 0, 0, gold);
            }
        }
    }
}
