package com.monkey.ktplus.effects.list.headcollector.animation;

import com.monkey.ktplus.effects.list.headcollector.HeadCollectorService;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorSettings;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorTrail;
import com.monkey.ktplus.effects.list.headcollector.HeadDisplayUtil;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.BukkitParticles;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

public final class HeadIntroAnimation {
    private static final double RISE_HEIGHT = 0.5;
    private static final long TICK_PERIOD = 1L;

    private HeadIntroAnimation() {}

    public static void play(
            EffectSession session,
            VisualEffectService visuals,
            HeadCollectorService collector,
            Player killer,
            Player victim,
            Location origin,
            HeadCollectorSettings settings) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(collector, "collector");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(settings, "settings");

        if (origin.getWorld() == null) {
            session.complete();
            return;
        }

        Location base = origin.clone().add(0.5, 0.05, 0.5);
        ItemDisplay display = HeadDisplayUtil.spawn(base, victim);
        if (display == null) {
            session.complete();
            return;
        }

        session.trackEntity(display);
        visuals.sound("ENTITY_ITEM_PICKUP", base, 0.8f, 0.7f);
        String introParticle = HeadCollectorTrail.particleForIndex(0);
        double[] spin = {0.0};
        long[] tick = {0L};
        session.runTimer(TICK_PERIOD, TICK_PERIOD, () -> {
            if (!display.isValid() || display.isDead()) {
                session.complete();
                return false;
            }
            tick[0]++;
            double progress = tick[0] / (double) settings.introDurationTicks();
            Location current = base.clone().add(0, RISE_HEIGHT * progress, 0);
            display.teleport(current);
            spin[0] += settings.headSpinSpeed();
            HeadDisplayUtil.setSlowSpin(display, Math.toDegrees(spin[0]));
            BukkitParticles.spawn(introParticle, current, 1, 0.0, 0.0, 0.0, 0.0);

            if (tick[0] >= settings.introDurationTicks()) {
                session.releaseEntity(display);
                collector.adoptHead(killer, display, settings);
                session.complete();
                return false;
            }
            return true;
        });
    }
}
