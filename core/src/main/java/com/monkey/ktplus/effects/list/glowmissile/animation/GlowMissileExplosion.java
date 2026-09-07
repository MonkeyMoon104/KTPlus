package com.monkey.ktplus.effects.list.glowmissile.animation;

import com.monkey.ktplus.effects.list.glowmissile.animation.util.GlowMissileParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import org.bukkit.Location;

public final class GlowMissileExplosion {
    private enum Phase {
        SHRINK_SPHERE,
        EXPAND_RINGS
    }

    private GlowMissileExplosion() {}

    public static void start(EffectSession session, VisualEffectService visuals, Location center, Runnable onComplete) {
        if (center.getWorld() == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        double[] sphereRadius = {6.0};
        double[] ring1Radius = {0};
        double[] ring2Radius = {0};
        Phase[] phase = {Phase.SHRINK_SPHERE};
        boolean[] sphereStarted = {false};
        session.runTimer(0L, 2L, () -> {
            if (phase[0] == Phase.SHRINK_SPHERE) {
                if (!sphereStarted[0]) {
                    visuals.sound("ENTITY_WARDEN_SONIC_CHARGE", center, 2.0f, 1.0f);
                    sphereStarted[0] = true;
                }
                if (sphereRadius[0] <= 0.1) {
                    phase[0] = Phase.EXPAND_RINGS;
                    visuals.sound("ENTITY_WARDEN_SONIC_BOOM", center, 2.0f, 1.0f);
                    return true;
                }
                GlowMissileParticles.spawnGlowSphere(visuals, center, sphereRadius[0], 300);
                sphereRadius[0] -= 0.7;
                return true;
            }
            if (ring1Radius[0] >= 15) {
                if (onComplete != null) {
                    onComplete.run();
                }
                return false;
            }
            GlowMissileParticles.spawnGlowRing(visuals, center, ring1Radius[0], 100, 0);
            GlowMissileParticles.spawnTiltedGlowRing(visuals, center, ring2Radius[0], 100, 20);
            ring1Radius[0] += 0.5;
            ring2Radius[0] += 0.5;
            return true;
        });
    }
}
