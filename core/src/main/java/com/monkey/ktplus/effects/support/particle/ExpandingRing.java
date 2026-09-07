package com.monkey.ktplus.effects.support.particle;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import org.bukkit.Color;
import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

public final class ExpandingRing {
    private ExpandingRing() {}

    public static void play(
            EffectSession session,
            VisualEffectService visuals,
            String particle,
            Location base,
            @Nullable Color color,
            int maxTicks,
            double radiusStep,
            long periodTicks) {
        play(session, visuals, particle, null, base, tick -> color, maxTicks, radiusStep, 0.02, periodTicks);
    }

    public static void play(
            EffectSession session,
            VisualEffectService visuals,
            String primary,
            @Nullable String secondary,
            Location base,
            @Nullable Color color,
            int maxTicks,
            double radiusStep,
            double heightStep,
            long periodTicks) {
        play(session, visuals, primary, secondary, base, tick -> color, maxTicks, radiusStep, heightStep, periodTicks);
    }

    public static void playCollapse(
            EffectSession session,
            VisualEffectService visuals,
            String primary,
            @Nullable String secondary,
            Location base,
            @Nullable Color color,
            int maxTicks,
            double maxRadius,
            long periodTicks) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(base, "base");
        AtomicInteger tick = new AtomicInteger();
        int limit = Math.max(1, maxTicks);
        session.runTimer(0L, Math.max(1L, periodTicks), () -> {
            int current = tick.getAndIncrement();
            double progress = current / (double) limit;
            double radius = 0.35 + Math.max(0.2, maxRadius) * Math.sin(Math.PI * progress);
            spawnRing(visuals, primary, secondary, base, color, current, radius, 0.015);
            return current < limit;
        });
    }

    public static void play(
            EffectSession session,
            VisualEffectService visuals,
            String primary,
            @Nullable String secondary,
            Location base,
            IntFunction<@Nullable Color> colorAtTick,
            int maxTicks,
            double radiusStep,
            double heightStep,
            long periodTicks) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(colorAtTick, "colorAtTick");
        AtomicInteger tick = new AtomicInteger();
        int limit = Math.max(1, maxTicks);
        session.runTimer(0L, Math.max(1L, periodTicks), () -> {
            int current = tick.getAndIncrement();
            double radius = 0.4 + current * radiusStep;
            spawnRing(
                    visuals,
                    primary,
                    secondary,
                    base,
                    colorAtTick.apply(current),
                    current,
                    radius,
                    current * heightStep);
            return current < limit;
        });
    }

    private static void spawnRing(
            VisualEffectService visuals,
            String primary,
            @Nullable String secondary,
            Location base,
            @Nullable Color color,
            int current,
            double radius,
            double heightOffset) {
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0 * i) / 24.0;
            Location point = base.clone().add(Math.cos(angle) * radius, 0.12 + heightOffset, Math.sin(angle) * radius);
            visuals.particle(primary, point, 1, 0, 0, 0, 0, color);
            if (secondary != null) {
                Location secondaryPoint = point.clone().add(0, 0.08 + (current % 3) * 0.02, 0);
                visuals.particle(secondary, secondaryPoint, 1, 0, 0, 0, 0, color);
            }
        }
    }
}
