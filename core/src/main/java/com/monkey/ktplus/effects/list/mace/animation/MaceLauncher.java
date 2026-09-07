package com.monkey.ktplus.effects.list.mace.animation;

import com.monkey.ktplus.effects.list.mace.animation.util.MaceParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class MaceLauncher {
    private MaceLauncher() {}

    public static void launch(EffectSession session, VisualEffectService visuals, Player killer, Location loc) {
        if (loc.getWorld() == null) {
            return;
        }
        AtomicInteger ticks = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            int current = ticks.incrementAndGet();
            if (current >= 25) {
                MaceParticles.spawnGoldenShockwave(visuals, loc);
                visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", loc, 2.0f, 0.8f);
                MaceOrbitalAnimation.start(session, visuals, loc.clone(), killer);
                return false;
            }
            double radius = (4.0 * current) / 25.0;
            MaceParticles.spawnChargingSphere(visuals, loc, radius, 120);
            visuals.sound("BLOCK_BEACON_AMBIENT", loc, 0.5f, 1.5f);
            return true;
        });
    }
}
