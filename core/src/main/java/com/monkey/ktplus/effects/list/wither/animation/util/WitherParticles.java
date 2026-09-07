package com.monkey.ktplus.effects.list.wither.animation.util;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.entity.SupportedEntities;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

public final class WitherParticles {
    private WitherParticles() {}

    public static void spawnDarkSphere(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location center,
            double radius,
            int points) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(center, "center");
        int scaledPoints = ParticleScale.scale(points);
        for (int i = 0; i < scaledPoints; i++) {
            double phi = Math.acos(1 - 2.0 * (i + 0.5) / scaledPoints);
            double theta = Math.PI * (1 + Math.sqrt(5)) * (i + 0.5);

            double x = radius * Math.cos(theta) * Math.sin(phi);
            double y = radius * Math.sin(theta) * Math.sin(phi);
            double z = radius * Math.cos(phi);

            Location particleLoc = center.clone().add(x, y, z);
            visuals.particle("LARGE_SMOKE", particleLoc, 1, 0, 0, 0, 0, Color.BLACK);
        }
    }

    public static void spawnWitherExplosion(EffectSession session, VisualEffectService visuals, Location center) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(center, "center");
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        visuals.particle("FLASH", center, 1, 0, 0, 0, 0, Color.BLACK);
        visuals.particle("EXPLOSION", center, 1, 0, 0, 0, 0, Color.BLACK);

        int ringPoints = ParticleScale.scale(36);
        for (int i = 0; i < ringPoints; i++) {
            double radians = (Math.PI * 2 * i) / ringPoints;
            double x = Math.cos(radians) * 4;
            double z = Math.sin(radians) * 4;

            Location ringLoc = center.clone().add(x, 0.5, z);
            visuals.particle("WITCH", ringLoc, ParticleScale.scale(5), 0.2, 0.2, 0.2, 0.01, Color.BLACK);
            visuals.particle("PORTAL", ringLoc, ParticleScale.scale(10), 0.2, 0.2, 0.2, 0.01, Color.BLACK);
        }
    }

    public static void launchBurstSkulls(
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

        BuiltInDamageService.apply(session, killer, center, config.effectDamage("wither"));

        EntityType skullType = SupportedEntities.resolve(visuals.entity("WITHER_SKULL"));
        if (skullType == null) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            Vector dir = new Vector(Math.cos(angle), -0.5, Math.sin(angle)).normalize();

            Location launchFrom = center.clone().add(0, 1.2, 0);
            if (!session.allowsWorldMutation(killer, launchFrom)) {
                continue;
            }

            WitherSkull skull = (WitherSkull) world.spawnEntity(launchFrom, skullType);
            skull.setDirection(dir);
            skull.setVelocity(dir.multiply(0.6));
            EntityCompat.trySetInvulnerable(skull, true);
            skull.setYield(0);
            skull.setCharged(true);
            EntityCompat.trySetGravity(skull, true);
            EntityCompat.trySetSilent(skull, true);
            session.entityRegistry().registerOwnedDamager(skull.getUniqueId(), killer.getUniqueId());
            session.trackEntity(skull);
            session.runLater(200L, skull::remove);

            session.runTimer(0L, 1L, () -> {
                if (!skull.isValid() || skull.isDead()) {
                    return false;
                }
                visuals.particle(
                        "SOUL_FIRE_FLAME",
                        skull.getLocation(),
                        ParticleScale.scale(2),
                        0,
                        0,
                        0,
                        0.01,
                        Color.BLACK);
                visuals.particle(
                        "LARGE_SMOKE",
                        skull.getLocation(),
                        ParticleScale.scale(1),
                        0.05,
                        0.05,
                        0.05,
                        0.01,
                        Color.BLACK);
                return true;
            });
        }
    }

    public static void spawnFinalImplosion(
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

        int[] step = {0};

        session.runTimer(0L, 1L, () -> {
            if (step[0] > 30) {
                visuals.particle("EXPLOSION", center, ParticleScale.scale(3), 0, 0, 0, 0, Color.BLACK);
                visuals.particle(
                        "FIREWORK",
                        center,
                        ParticleScale.scale(20),
                        0.5,
                        0.5,
                        0.5,
                        0.1,
                        Color.BLACK);
                visuals.particle(
                        "END_ROD",
                        center,
                        ParticleScale.scale(40),
                        0.7,
                        0.7,
                        0.7,
                        0.01,
                        Color.BLACK);
                visuals.sound("ENTITY_GENERIC_EXPLODE", center, 3f, 0.5f);
                launchBurstSkulls(session, visuals, center, killer, config);
                return false;
            }

            double radius = 5 - (step[0] * 0.15);
            int points = ParticleScale.scale(60);
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points + step[0] * 0.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.sin(step[0] * 0.3 + i * 0.1) * 1.5;

                Location pLoc = center.clone().add(x, 1.5 + y, z);
                visuals.particle("PORTAL", pLoc, ParticleScale.scale(2), 0.05, 0.05, 0.05, 0.01, Color.BLACK);
                visuals.particle("SOUL_FIRE_FLAME", pLoc, 1, 0, 0, 0, 0.01, Color.BLACK);
                visuals.particle("DUST", pLoc, 1, 0, 0, 0, 0, Color.fromRGB(90, 0, 140));
            }

            step[0]++;
            return true;
        });
    }
}
