package com.monkey.ktplus.effects.list.dimensionalrift.animation.util;

import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class RiftParticles {
    private static final Color VOID_PURPLE = Color.fromRGB(90, 30, 140);
    private static final Color VOID_DEEP = Color.fromRGB(35, 10, 55);
    private static final Color VOID_GLOW = Color.fromRGB(160, 80, 220);

    private RiftParticles() {}

    public static void spawnPortalAura(VisualEffectService visuals, Location center, int tick, double open) {
        double width = 1.1 * open;
        double height = 1.6 * open;
        int rings = ParticleScale.scale(18);
        for (int i = 0; i < rings; i++) {
            double t = i / (double) Math.max(1, rings - 1);
            double angle = t * Math.PI * 2.0 + tick * 0.18;
            Location point = center.clone().add(Math.cos(angle) * width, Math.sin(angle) * height * 0.55, Math.sin(angle) * width * 0.35);
            visuals.particle("PORTAL", point, 2, 0.04, 0.05, 0.04, 0.015, null);
            if (i % 2 == 0) {
                visuals.dust(point, VOID_PURPLE, 1.1f, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (i % 3 == 0) {
                visuals.particle("REVERSE_PORTAL", point, 1, 0.02, 0.02, 0.02, 0.01, null);
            }
        }
        if (tick % 2 == 0) {
            visuals.dust(center, VOID_DEEP, 1.4f, ParticleScale.scale(3), 0.25, 0.35, 0.25, 0.0);
            visuals.dust(center, VOID_GLOW, 0.9f, 2, 0.15, 0.2, 0.15, 0.0);
        }
    }

    public static void spawnOpenBurst(VisualEffectService visuals, Location center) {
        visuals.particle("PORTAL", center, ParticleScale.scale(40), 0.7, 1.0, 0.7, 0.35, null);
        visuals.particle("REVERSE_PORTAL", center, ParticleScale.scale(18), 0.5, 0.7, 0.5, 0.08, null);
        visuals.particle("END_ROD", center, ParticleScale.scale(10), 0.35, 0.55, 0.35, 0.04, null);
        visuals.dust(center, VOID_PURPLE, 1.6f, ParticleScale.scale(12), 0.45, 0.6, 0.45, 0.0);
        visuals.particle("FLASH", center, 1, 0.0, 0.0, 0.0, 0.0, null);
    }

    public static void spawnCloseBurst(VisualEffectService visuals, Location center) {
        visuals.particle("PORTAL", center, ParticleScale.scale(28), 0.55, 0.8, 0.55, 0.25, null);
        visuals.particle("WITCH", center, ParticleScale.scale(10), 0.35, 0.45, 0.35, 0.02, null);
        visuals.dust(center, VOID_DEEP, 1.5f, ParticleScale.scale(10), 0.4, 0.5, 0.4, 0.0);
        visuals.particle("REVERSE_PORTAL", center, ParticleScale.scale(14), 0.4, 0.5, 0.4, 0.05, null);
    }

    public static void spawnBoltTrail(VisualEffectService visuals, Location at, Vector dir, int tick) {
        visuals.particle("PORTAL", at, ParticleScale.scale(4), 0.06, 0.06, 0.06, 0.02, null);
        visuals.dust(at, VOID_GLOW, 1.0f, 2, 0.04, 0.04, 0.04, 0.0);
        if (tick % 2 == 0) {
            visuals.particle("END_ROD", at, 1, 0.0, 0.0, 0.0, 0.0, null);
            visuals.dust(at, VOID_PURPLE, 1.2f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (dir != null && dir.lengthSquared() > 1.0e-4) {
            Vector n = dir.clone().normalize();
            Location wake = at.clone().subtract(n.clone().multiply(0.35));
            visuals.particle("REVERSE_PORTAL", wake, 2, 0.05, 0.05, 0.05, 0.01, null);
        }
    }

    public static void spawnBoltHit(VisualEffectService visuals, Location at) {
        visuals.particle("PORTAL", at, ParticleScale.scale(22), 0.45, 0.55, 0.45, 0.2, null);
        visuals.particle("FLASH", at, 1, 0.0, 0.0, 0.0, 0.0, null);
        visuals.dust(at, VOID_GLOW, 1.7f, ParticleScale.scale(10), 0.3, 0.35, 0.3, 0.0);
        visuals.particle("END_ROD", at, ParticleScale.scale(8), 0.25, 0.3, 0.25, 0.05, null);
    }
}
