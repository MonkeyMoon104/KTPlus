package com.monkey.ktplus.effects.list.tornado.animation;

import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class TornadoFunnel {
    static final Color DUST_DARK = Color.fromRGB(70, 70, 75);
    static final Color DUST_MID = Color.fromRGB(120, 115, 110);
    static final Color DUST_LIGHT = Color.fromRGB(190, 190, 195);
    static final Color DUST_DIRT = Color.fromRGB(110, 75, 45);

    private static final String[] AMBIENT_SOUNDS = {
        "ITEM_ELYTRA_FLYING",
        "ENTITY_BREEZE_IDLE_GROUND",
        "ENTITY_BREEZE_IDLE_AIR",
        "ENTITY_WIND_CHARGE_WIND_BURST",
        "ENTITY_WIND_CHARGE_THROW",
        "ENTITY_BREEZE_SHOOT",
        "ENTITY_BREEZE_JUMP"
    };

    private TornadoFunnel() {}

    static void stopTornadoSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    static void drawFunnel(
            VisualEffectService visuals, Location core, TornadoStats stats, int tick, Vector moveDelta) {
        double moveX = moveDelta == null ? 0.0 : moveDelta.getX();
        double moveZ = moveDelta == null ? 0.0 : moveDelta.getZ();
        double moveLen = Math.sqrt(moveX * moveX + moveZ * moveZ);

        int ghosts = moveLen > 0.04 ? 3 : 1;
        for (int g = 0; g < ghosts; g++) {
            double lag = g * 0.55;
            Location ghost = core.clone().subtract(moveX * lag, 0.0, moveZ * lag);
            float alphaScale = g == 0 ? 1.0f : (float) (0.55 - g * 0.12);
            drawFunnelAt(visuals, ghost, stats, tick + g * 3, alphaScale, g == 0);
        }
    }

    private static void drawFunnelAt(
            VisualEffectService visuals,
            Location core,
            TornadoStats stats,
            int tick,
            float density,
            boolean primary) {
        int layers = 8 + stats.level;
        double spin = tick * stats.spinSpeed * 1.15;
        double tickBias = (tick % 5) * 0.17;
        for (int layer = 0; layer < layers; layer++) {
            if (density < 0.9f && layer % 2 != 0) {
                continue;
            }
            double t = layer / (double) Math.max(1, layers - 1);
            double y = 0.12 + t * stats.height;
            double radius = stats.baseRadius * (0.28 + t * t * 1.7);
            int points = Math.max(6, ParticleScale.scale(7 + stats.level + (int) (radius * 2.2)));
            if (density < 0.9f) {
                points = Math.max(4, points / 2);
            }
            for (int i = 0; i < points; i++) {
                double a = spin * (1.0 + t * 0.55)
                        + (Math.PI * 2.0 * i) / points
                        + layer * 0.55
                        + tickBias;
                double swirl = radius * (0.78 + 0.22 * Math.sin(tick * 0.22 + i * 0.9 + layer));
                Location p = core.clone().add(Math.cos(a) * swirl, y, Math.sin(a) * swirl);
                Color color = t < 0.22 ? DUST_DIRT : (t < 0.55 ? DUST_DARK : (t < 0.82 ? DUST_MID : DUST_LIGHT));
                float size = (float) ((0.75 + t * 1.0 + stats.level * 0.08) * (0.75 + density * 0.25));
                visuals.dust(p, color, size, 1, 0.0, 0.0, 0.0, 0.0);
                if (primary && i % 4 == 0) {
                    visuals.particle("CLOUD", p, 1, 0.03, 0.03, 0.03, 0.0, null);
                }
            }
        }
        if (!primary) {
            return;
        }
        visuals.particle(
                "CLOUD",
                core.clone().add(0, stats.height * 0.35, 0),
                ParticleScale.scale(4 + stats.level),
                stats.baseRadius * 0.3,
                stats.height * 0.22,
                stats.baseRadius * 0.3,
                0.015,
                null);
        if (tick % 2 == 0) {
            for (int i = 0; i < 4 + stats.level; i++) {
                double a = Math.random() * Math.PI * 2.0;
                double r = Math.random() * stats.baseRadius * 1.2;
                Location g = core.clone().add(Math.cos(a) * r, 0.08, Math.sin(a) * r);
                visuals.dust(g, DUST_DIRT, 1.15f, 1, 0.06, 0.02, 0.06, 0.0);
            }
        }
    }
}
