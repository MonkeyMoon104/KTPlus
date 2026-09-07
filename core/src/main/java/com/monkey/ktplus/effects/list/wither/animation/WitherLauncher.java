package com.monkey.ktplus.effects.list.wither.animation;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.list.wither.animation.util.WitherParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class WitherLauncher {
    private static final double SOUND_STOP_RADIUS = 48.0;
    private static final String[] EFFECT_SOUNDS = {
        "ENTITY_WITHER_SPAWN",
        "ENTITY_WITHER_DEATH",
        "ENTITY_WITHER_AMBIENT",
        "ENTITY_WITHER_SHOOT",
        "AMBIENT_CAVE",
        "ENTITY_GENERIC_EXPLODE"
    };

    private WitherLauncher() {}

    public static void launch(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Location loc,
            ConfigSnapshot config) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(loc, "loc");
        Objects.requireNonNull(config, "config");
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        session.onCleanup(() -> stopEffectSounds(world, loc));

        int[] ticks = {0};
        final int maxTicks = 30;
        final double maxRadius = 5;

        visuals.sound("ENTITY_WITHER_SPAWN", loc, 0.85f, 1.1f);

        session.runTimer(0L, 2L, () -> {
            if (ticks[0] >= maxTicks) {
                WitherParticles.spawnWitherExplosion(session, visuals, loc);
                visuals.sound("ENTITY_WITHER_DEATH", loc, 2.0f, 0.5f);
                new WitherOrbitalAnimation(session, visuals, loc.clone(), killer, config).start();
                return false;
            }

            double radius = (maxRadius * ticks[0]) / maxTicks;
            WitherParticles.spawnDarkSphere(session, visuals, world, loc, radius, 150);

            ticks[0]++;
            return true;
        });
    }

    static void stopEffectSounds(World world, Location center) {
        if (world == null || center == null) {
            return;
        }
        double radiusSq = SOUND_STOP_RADIUS * SOUND_STOP_RADIUS;
        for (Player player : world.getPlayers()) {
            if (player.getWorld() != world) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            for (String sound : EFFECT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }
}
