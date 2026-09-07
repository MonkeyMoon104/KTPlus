package com.monkey.ktplus.effects.list.explosion.animation.util;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;

public final class ExplosionUtils {
    private static final Random RANDOM = new Random();

    private ExplosionUtils() {}

    public static void launchCosmeticTnt(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        
        TNTPrimed tnt = world.spawn(center.clone().add(0, 1, 0), TNTPrimed.class);
        tnt.setFuseTicks(40 + RANDOM.nextInt(20));
        EntityCompat.trySetInvulnerable(tnt, true);
        EntityCompat.trySetSilent(tnt, true);
        tnt.setYield(0f);
        session.trackEntity(tnt);
        ConfigurationSection section = context.config().effectSection("explosion");
        ConfigurationSection projectiles = section == null ? null : section.getConfigurationSection("projectiles");
        ConfigurationSection settings = projectiles == null ? null : projectiles.getConfigurationSection("settings");
        boolean damageEnabled = projectiles != null && projectiles.getBoolean("damage", true);
        double rage = settings == null ? 5.0 : settings.getDouble("rage", 5.0);
        double value = settings == null ? 2.0 : settings.getDouble("value", 2.0);
        Vector velocity = new Vector(
                (RANDOM.nextDouble() - 0.5) * 1.5,
                0.8 + RANDOM.nextDouble() * 0.5,
                (RANDOM.nextDouble() - 0.5) * 1.5);
        tnt.setVelocity(velocity);
        Player killer = context.killer();
        AtomicInteger tickCount = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            tickCount.incrementAndGet();
            if (!tnt.isValid() || tnt.isDead() || tickCount.get() > 200) {
                handleExplosion(session, visuals, killer, tnt, damageEnabled, rage, value);
                return false;
            }
            Location loc = tnt.getLocation();
            visuals.particle("FLAME", loc, ParticleScale.scale(3), 0.1, 0.1, 0.1, 0.05, null);
            visuals.particle("LARGE_SMOKE", loc, ParticleScale.scale(2), 0.1, 0.1, 0.1, 0.02, null);
            visuals.particle("ENCHANTED_HIT", loc, ParticleScale.scale(2), 0.1, 0.1, 0.1, 0.01, null);
            return true;
        });
    }

    private static void handleExplosion(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            TNTPrimed tnt,
            boolean damageEnabled,
            double rage,
            double value) {
        Location explosionLoc = tnt.getLocation();
        World world = explosionLoc.getWorld();
        if (world == null) {
            return;
        }
        spawnExplosionParticles(visuals, explosionLoc);
        visuals.sound("ENTITY_DRAGON_FIREBALL_EXPLODE", explosionLoc, 2.0f, 1.0f);
        if (damageEnabled) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity.equals(killer)) {
                    continue;
                }
                if (entity.getLocation().distance(explosionLoc) > rage) {
                    continue;
                }
                if (!session.allowsWorldMutation(killer, entity.getLocation())) {
                    continue;
                }
                entity.damage(value, killer);
            }
        }
        if (tnt.isValid() && !tnt.isDead()) {
            tnt.remove();
        }
    }

    private static void spawnExplosionParticles(VisualEffectService visuals, Location loc) {
        for (int layer = 0; layer < 4; layer++) {
            double radius = 0.5 + layer * 0.6;
            for (int i = 0; i < 16; i++) {
                double angle = 2 * Math.PI / 16 * i;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                double y = 0.2 * layer;
                Location particleLoc = loc.clone().add(x, y + 0.5, z);
                visuals.particle("FIREWORK", particleLoc, 1, 0, 0, 0, 0.01, null);
                visuals.particle("DRIPPING_LAVA", particleLoc, 1, 0.05, 0.05, 0.05, 0.01, null);
                Location colorLoc = particleLoc.clone().add(0, Math.sin(angle * 4) * 0.3, 0);
                visuals.particle(
                        "DUST",
                        colorLoc,
                        1,
                        0,
                        0,
                        0,
                        0,
                        Color.fromRGB(255, 180 - layer * 30, 50 + layer * 40));
                if (i % 4 == 0) {
                    visuals.particle("LARGE_SMOKE", particleLoc.clone().add(0, 0.2, 0), 1, 0.05, 0.1, 0.05, 0.02, null);
                }
            }
        }
    }
}
