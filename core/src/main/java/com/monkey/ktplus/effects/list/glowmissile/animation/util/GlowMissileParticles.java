package com.monkey.ktplus.effects.list.glowmissile.animation.util;

import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import org.bukkit.Location;

public final class GlowMissileParticles {
    private GlowMissileParticles() {}

    public static void spawnGlowSphere(VisualEffectService visuals, Location center, double radius, int points) {
        int scaled = ParticleScale.scale(points);
        for (int i = 0; i < scaled; i++) {
            double phi = Math.acos(1 - 2.0 * (i + 0.5) / scaled);
            double theta = Math.PI * (1 + Math.sqrt(5)) * (i + 0.5);
            double x = radius * Math.cos(theta) * Math.sin(phi);
            double y = radius * Math.sin(theta) * Math.sin(phi);
            double z = radius * Math.cos(phi);
            visuals.particle("GLOW", center.clone().add(x, y, z), 1, 0, 0, 0, 0, null);
        }
    }

    public static void spawnGlowRing(VisualEffectService visuals, Location center, double radius, int points, double yOffset) {
        int scaled = ParticleScale.scale(points);
        for (int i = 0; i < scaled; i++) {
            double angle = 2 * Math.PI * i / scaled;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            visuals.particle("GLOW", center.clone().add(x, yOffset, z), 1, 0, 0, 0, 0, null);
        }
    }

    public static void spawnTiltedGlowRing(
            VisualEffectService visuals, Location center, double radius, int points, double tiltDegrees) {
        double tiltRad = Math.toRadians(tiltDegrees);
        double sinTilt = Math.sin(tiltRad);
        double cosTilt = Math.cos(tiltRad);
        int scaled = ParticleScale.scale(points);
        for (int i = 0; i < scaled; i++) {
            double angle = 2 * Math.PI * i / scaled;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            double y = z * sinTilt;
            double zTilted = z * cosTilt;
            visuals.particle("GLOW", center.clone().add(x, y, zTilted), 1, 0, 0, 0, 0, null);
        }
    }

    public static void spawnEngineParticles(VisualEffectService visuals, Location anchor) {
        if (anchor.getWorld() == null) {
            return;
        }
        boolean playedSound = false;
        for (GlowMissileShape.BlockEntry thruster : GlowMissileShape.obsidianThrusters()) {
            Location center = blockCenter(anchor, thruster);
            spawnThrusterBurst(visuals, center);
            if (!playedSound) {
                visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", center, 0.5f, 1.4f);
                playedSound = true;
            }
        }
    }

    private static void spawnThrusterBurst(VisualEffectService visuals, Location blockCenter) {
        Location exhaust = blockCenter.clone().add(0, -0.45, 0);
        visuals.particle("FLAME", exhaust, ParticleScale.scale(6), 0.04, 0.02, 0.04, 0.015, null);
        visuals.particle("LAVA", exhaust, ParticleScale.scale(2), 0.05, 0.08, 0.05, 0.01, null);
        for (int step = 1; step <= 5; step++) {
            Location trail = exhaust.clone().add(0, -step * 0.35, 0);
            visuals.particle("FLAME", trail, ParticleScale.scale(4), 0.05, 0.06, 0.05, 0.01, null);
            if (step % 2 == 0) {
                visuals.particle("LAVA", trail, ParticleScale.scale(1), 0.04, 0.05, 0.04, 0.005, null);
            }
        }
    }

    private static Location blockCenter(Location anchor, GlowMissileShape.BlockEntry entry) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX() + entry.dx() + 0.5D,
                anchor.getBlockY() + entry.dy() + 0.5D,
                anchor.getBlockZ() + entry.dz() + 0.5D);
    }
}
