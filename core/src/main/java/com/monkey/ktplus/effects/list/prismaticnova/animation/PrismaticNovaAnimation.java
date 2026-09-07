package com.monkey.ktplus.effects.list.prismaticnova.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;

public final class PrismaticNovaAnimation {
    private PrismaticNovaAnimation() {}

    public static void launch(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location center) {
        if (center.getWorld() == null) {
            return;
        }
        visuals.sound("BLOCK_BEACON_POWER_SELECT", center, 1.6f, 1.2f);
        AtomicInteger tick = new AtomicInteger();
        boolean[] exploded = {false};
        session.runTimer(0L, 1L, () -> {
            int current = tick.getAndIncrement();
            if (current > 34) {
                return false;
            }
            double radius = 0.35 + (current * 0.12);
            double yOffset = (Math.sin(current * 0.35) * 0.4) + 0.15;
            spawnRing(visuals, center.clone().add(0, yOffset, 0), radius, current);
            spawnHelix(visuals, center, radius * 0.6, current);
            if (current % 5 == 0) {
                visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", center, 0.9f, 0.8f + (current * 0.02f));
            }
            if (current >= 28 && !exploded[0]) {
                exploded[0] = true;
                explode(visuals, center);
                BuiltInDamageService.apply(session, context.killer(), center, context.config().effectDamage("prismaticnova"));
            }
            return true;
        });
    }

    private static void spawnRing(VisualEffectService visuals, Location center, double radius, int tick) {
        int points = ParticleScale.scale(42);
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 * i) / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            float hue = (float) ((i / (double) points) + (tick * 0.03)) % 1f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0x00FFFFFF;
            Color color = Color.fromRGB(rgb);
            visuals.particle("DUST", center.clone().add(x, 0, z), 1, 0, 0, 0, 0, color);
        }
    }

    private static void spawnHelix(VisualEffectService visuals, Location center, double radius, int tick) {
        for (int arm = 0; arm < 2; arm++) {
            double phase = arm * Math.PI;
            int steps = ParticleScale.scale(18);
            for (int step = 0; step < steps; step++) {
                double progress = step / (double) Math.max(1, steps - 1);
                double angle = (tick * 0.32) + (progress * 3.3 * Math.PI) + phase;
                double x = Math.cos(angle) * (radius * (0.4 + progress));
                double z = Math.sin(angle) * (radius * (0.4 + progress));
                double y = progress * 2.2;
                visuals.particle("END_ROD", center.clone().add(x, y, z), 1, 0, 0, 0, 0, null);
            }
        }
    }

    private static void explode(VisualEffectService visuals, Location center) {
        visuals.particle("FIREWORK", center, ParticleScale.scale(120), 1.2, 1.2, 1.2, 0.05, null);
        visuals.particle("WAX_ON", center, ParticleScale.scale(80), 1.0, 0.8, 1.0, 0.1, null);
        visuals.particle("END_ROD", center, ParticleScale.scale(40), 0.7, 0.7, 0.7, 0.05, null);
        visuals.sound("ENTITY_FIREWORK_ROCKET_BLAST_FAR", center, 2.0f, 1.0f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 1.2f, 1.35f);
    }
}
