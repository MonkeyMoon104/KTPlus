package com.monkey.ktplus.effects.list.mace.animation;

import com.monkey.ktplus.effects.list.mace.animation.util.MaceParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class MaceOrbitalAnimation {
    private MaceOrbitalAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, Location center, Player killer) {
        if (center.getWorld() == null) {
            return;
        }
        AtomicInteger ticks = new AtomicInteger();
        double[] angle = {0};
        session.runTimer(0L, 1L, () -> {
            int current = ticks.incrementAndGet();
            if (current > 60) {
                MaceParticles.spawnFinalBurst(visuals, center);
                visuals.sound("ENTITY_GENERIC_EXPLODE", center, 2.0f, 1.2f);
                return false;
            }
            angle[0] += Math.PI / 20;
            double radius = 2.5 + Math.sin(current / 5.0) * 0.5;
            double x = Math.cos(angle[0]) * radius;
            double z = Math.sin(angle[0]) * radius;
            Location orbLoc = center.clone().add(x, Math.sin(angle[0] * 2) * 1.2 + 1, z);
            visuals.particle("END_ROD", orbLoc, 1, 0, 0, 0, 0.01, null);
            visuals.particle("ENCHANTED_HIT", orbLoc, 1, 0.05, 0.05, 0.05, 0.01, null);
            if (current % 5 == 0) {
                visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", center, 0.6f, 1.8f);
            }
            return true;
        });
    }
}
