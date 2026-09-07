package com.monkey.ktplus.effects.list.firephoenix.animation.util;

import com.monkey.ktplus.effects.runtime.CustomProjectileHitPolicy;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.BukkitParticles;
import com.monkey.ktplus.util.compat.EntityCompat;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.util.Vector;

public final class FirePhoenixProjectiles {
    private FirePhoenixProjectiles() {}

    public static void launchHomingFireCharge(
            EffectSession session,
            Location beakLocation,
            Player killer,
            Player target,
            boolean shouldDamage,
            double damageValue,
            java.util.Set<java.util.UUID> lockedTargets,
            java.util.concurrent.atomic.AtomicInteger activeVolleys) {
        World world = beakLocation.getWorld();
        if (world == null) {
            return;
        }
        Vector direction =
                target.getEyeLocation().toVector().subtract(beakLocation.toVector()).normalize();
        SmallFireball fireball = world.spawn(beakLocation, SmallFireball.class);
        fireball.setShooter(killer);
        fireball.setDirection(direction);
        fireball.setVelocity(direction.multiply(0.5));
        fireball.setIsIncendiary(false);
        fireball.setYield(0);
        EntityCompat.trySetSilent(fireball, true);
        session.trackEntity(fireball);
        lockedTargets.add(target.getUniqueId());
        activeVolleys.incrementAndGet();
        if (shouldDamage) {
            session.entityRegistry()
                    .registerProjectile(
                            fireball.getUniqueId(),
                            new CustomProjectileHitPolicy(session.playerId(), damageValue, true));
        }
        double[] angle = {0};
        session.runTimer(0L, 2L, () -> {
            if (!fireball.isValid()
                    || fireball.isDead()
                    || target.isDead()
                    || !target.getWorld().equals(world)) {
                activeVolleys.updateAndGet(value -> Math.max(0, value - 1));
                if (!target.isOnline() || target.isDead()) {
                    lockedTargets.remove(target.getUniqueId());
                }
                return false;
            }
            Vector newDirection =
                    target.getEyeLocation().toVector().subtract(fireball.getLocation().toVector()).normalize();
            fireball.setVelocity(newDirection.multiply(0.5));
            Location location = fireball.getLocation();
            BukkitParticles.particle(world, "FLAME", location, 4, 0.2, 0.2, 0.2, 0.05);
            BukkitParticles.redstoneDust(world, location, Color.fromRGB(255, 180, 50), 1.7f, 2);
            BukkitParticles.particle(world, "SMOKE_NORMAL", location, 2, 0.1, 0.1, 0.1, 0.02, "SMOKE", "CLOUD");
            angle[0] += 0.5235987755982988;
            for (int i = 0; i < 3; i++) {
                double x = 0.4 * Math.cos(angle[0] + i * Math.PI / 2.0);
                double y = 0.4 * Math.sin(angle[0] + i * Math.PI / 2.0);
                Location vortex = location.clone().add(x, y, -i * 0.1);
                BukkitParticles.particle(world, "CRIT", vortex, 1, 0, 0, 0, 0.01);
                BukkitParticles.particle(world, "LAVA", vortex, 1, 0.05, 0.05, 0.05, 0.01);
                BukkitParticles.particle(
                        world, "SPELL_WITCH", vortex, 1, 0, 0, 0, 0.01, "WITCH", "ENCHANT");
            }
            return true;
        });
    }
}
