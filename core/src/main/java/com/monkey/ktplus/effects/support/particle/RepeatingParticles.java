package com.monkey.ktplus.effects.support.particle;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

public final class RepeatingParticles {
    private RepeatingParticles() {}

    public static void play(
            EffectSession session,
            VisualEffectService visuals,
            String particle,
            Location center,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            long periodTicks,
            int repeats,
            @Nullable Color color) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(particle, "particle");
        Objects.requireNonNull(center, "center");
        AtomicInteger remaining = new AtomicInteger(Math.max(1, repeats));
        session.runTimer(0L, Math.max(1L, periodTicks), () -> {
            visuals.particle(
                    particle, center, ParticleScale.scale(count), offsetX, offsetY, offsetZ, extra, color);
            return remaining.decrementAndGet() > 0;
        });
    }
}
