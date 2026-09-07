package com.monkey.ktplus.effects.list.stellarcollapse.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.list.stellarcollapse.animation.util.StellarParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;

public final class StellarCollapseLauncher {
    private StellarCollapseLauncher() {}

    public static void launch(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location center) {
        if (center.getWorld() == null) {
            return;
        }
        AtomicInteger ticks = new AtomicInteger();
        session.runTimer(0L, 2L, () -> {
            int current = ticks.getAndIncrement();
            if (current >= 40) {
                spawnCollapseImplosion(session, visuals, context, center);
                visuals.sound("BLOCK_BEACON_AMBIENT", center, 2.0f, 0.8f);
                return false;
            }
            double radius = 1 + (current * 0.1);
            StellarParticles.spawnStellarSwirl(visuals, center, radius, 80);
            if (current % 5 == 0) {
                visuals.sound("BLOCK_BEACON_POWER_SELECT", center, 2.0f, 1.5f - current * 0.03f);
            }
            return true;
        });
    }

    private static void spawnCollapseImplosion(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location center) {
        BuiltInDamageService.apply(session, context.killer(), center, context.config().effectDamage("stellarcollapse"));
        AtomicInteger step = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            int current = step.getAndIncrement();
            if (current > 24) {
                int columnSteps = ParticleScale.scale(50);
                for (int y = 0; y < columnSteps; y++) {
                    Location loc = center.clone().add(0, y * 0.3, 0);
                    visuals.particle("END_ROD", loc, ParticleScale.scale(4), 0.08, 0.08, 0.08, 0.01, null);
                    visuals.particle("ENCHANT", loc, ParticleScale.scale(5), 0.1, 0.1, 0.1, 0.0, null);
                }
                visuals.particle("FLASH", center, 1, 0, 0, 0, 0, Color.WHITE);
                visuals.sound("BLOCK_BEACON_ACTIVATE", center, 2.0f, 1.2f);
                return false;
            }
            double radius = 2.8 - current * 0.1;
            int ringPoints = ParticleScale.scale(70);
            for (int i = 0; i < ringPoints; i++) {
                double angle = 2 * Math.PI * i / ringPoints + current * 0.3;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = (Math.sin(current * 0.4 + i * 0.15)) * 0.4;
                Location pLoc = center.clone().add(x, y, z);
                visuals.particle("DRAGON_BREATH", pLoc, 1, 0.02, 0.02, 0.02, 0.0, null);
                visuals.particle("DUST", pLoc, 1, 0, 0, 0, 0, Color.fromRGB(255, 255, 100));
            }
            return true;
        });
    }
}
