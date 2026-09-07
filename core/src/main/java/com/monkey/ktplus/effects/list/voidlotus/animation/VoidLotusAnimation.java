package com.monkey.ktplus.effects.list.voidlotus.animation;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class VoidLotusAnimation {
    private VoidLotusAnimation() {}

    public static void launch(
            EffectSession session,
            VisualEffectService visuals,
            Location center,
            Player killer,
            ConfigSnapshot config) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(config, "config");
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        visuals.sound("BLOCK_END_PORTAL_FRAME_FILL", center, 1.2f, 0.8f);

        int[] tick = {0};
        final int maxTicks = 38;
        boolean[] damaged = {false};

        session.runTimer(0L, 1L, () -> {
            int currentTick = tick[0]++;
            if (currentTick > maxTicks) {
                return false;
            }

            spawnLotusPetals(visuals, center, currentTick);
            spawnCore(visuals, center, currentTick);

            if (currentTick == 18) {
                visuals.sound("ENTITY_ALLAY_DEATH", center, 1.3f, 0.65f);
            }

            if (currentTick == 30) {
                visuals.sound("ENTITY_WARDEN_SONIC_BOOM", center, 0.85f, 1.5f);
            }

            if (currentTick == 34) {
                implode(visuals, world, center);
                if (!damaged[0]) {
                    damaged[0] = true;
                    BuiltInDamageService.apply(session, killer, center, config.effectDamage("voidlotus"));
                }
            }

            return true;
        });
    }

    private static void spawnLotusPetals(VisualEffectService visuals, Location center, int tick) {
        int petals = ParticleScale.scale(8);
        double baseRadius = Math.min(3.2, 0.4 + (tick * 0.08));
        double wave = Math.sin(tick * 0.28) * 0.35;

        for (int i = 0; i < petals; i++) {
            double petalAngle = (Math.PI * 2 * i) / petals + (tick * 0.06);
            int steps = ParticleScale.scale(14);
            for (int step = 0; step < steps; step++) {
                double progress = step / (double) Math.max(1, steps - 1);
                double petalWidth = Math.sin(progress * Math.PI) * 0.65;
                double radius = baseRadius + (petalWidth * 0.75);
                double x = Math.cos(petalAngle) * radius;
                double z = Math.sin(petalAngle) * radius;
                double y = 0.12 + (progress * 1.25) + wave;

                Color dustColor = step % 2 == 0
                        ? Color.fromRGB(186, 104, 255)
                        : Color.fromRGB(98, 74, 255);

                visuals.particle("DUST", center.clone().add(x, y, z), 1, 0, 0, 0, 0, dustColor);
            }
        }
    }

    private static void spawnCore(VisualEffectService visuals, Location center, int tick) {
        double spin = tick * 0.21;
        int layers = ParticleScale.scale(3);
        for (int i = 0; i < layers; i++) {
            double angle = spin + (i * 2.1);
            double x = Math.cos(angle) * 0.45;
            double z = Math.sin(angle) * 0.45;
            double y = 0.2 + (i * 0.35);
            visuals.particle(
                    "DRAGON_BREATH",
                    center.clone().add(x, y, z),
                    ParticleScale.scale(2),
                    0.05,
                    0.05,
                    0.05,
                    0.01,
                    Color.PURPLE);
            visuals.particle(
                    "PORTAL",
                    center.clone().add(-x, y + 0.2, -z),
                    ParticleScale.scale(2),
                    0.1,
                    0.1,
                    0.1,
                    0.02,
                    Color.PURPLE);
        }
    }

    private static void implode(VisualEffectService visuals, World world, Location center) {
        visuals.particle(
                "REVERSE_PORTAL",
                center,
                ParticleScale.scale(110),
                1.5,
                1.5,
                1.5,
                0.2,
                Color.PURPLE);
        visuals.particle("SONIC_BOOM", center, 1, 0, 0, 0, 0, Color.PURPLE);
        visuals.particle(
                "SCULK_SOUL",
                center,
                ParticleScale.scale(35),
                1.1,
                0.9,
                1.1,
                0.08,
                Color.PURPLE);
        visuals.sound("ENTITY_ENDER_DRAGON_FLAP", center, 1.0f, 0.6f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_DEPLETE", center, 1.35f, 1.1f);
    }
}
