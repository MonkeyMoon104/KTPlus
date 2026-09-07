package com.monkey.ktplus.effects.list.firephoenix.animation.util;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.BukkitParticles;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.util.compat.WorldCompat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class FirePhoenixParticles {
    private FirePhoenixParticles() {}

    public static void spawnPhoenixExplosion(EffectSession session, EffectContext context, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Player killer = context.killer();
        ConfigurationSection effectConfig = context.config().effectSection("firephoenix");
        boolean projectilesEnabled = false;
        boolean projectilesDamage = false;
        int projectilesRange = 200;
        int projectilesDelay = 3;
        double projectilesValue = 4.0;
        if (effectConfig != null) {
            ConfigurationSection perks = effectConfig.getConfigurationSection("perks");
            ConfigurationSection projectiles =
                    perks != null ? perks.getConfigurationSection("projectiles") : null;
            if (projectiles == null) {
                projectiles = effectConfig.getConfigurationSection("projectiles");
            }
            if (projectiles != null) {
                projectilesEnabled = projectiles.getBoolean("enabled", false);
                ConfigurationSection settings = projectiles.getConfigurationSection("settings");
                ConfigurationSection values = settings != null ? settings : projectiles;
                projectilesDamage = values.getBoolean("damage", true);
                projectilesRange = values.contains("range")
                        ? values.getInt("range", 200)
                        : values.getInt("rage", 200);
                projectilesDelay = values.getInt("delay", 3);
                projectilesValue = values.getDouble("value", 4.0);
            }
        }
        BuiltInDamageService.apply(session, killer, center, context.config().effectDamage("firephoenix"));
        AtomicInteger step = new AtomicInteger();
        AtomicInteger projectileTimer = new AtomicInteger();
        Set<UUID> lockedTargets = ConcurrentHashMap.newKeySet();
        AtomicInteger activeVolleys = new AtomicInteger();
        session.onCleanup(() -> PerkActionBar.clear(killer));
        session.runTimer(0L, 5L, () -> {
            if (!session.active() || !killer.isOnline()) {
                PerkActionBar.clear(killer);
                return false;
            }
            PerkActionBar.show(
                    killer,
                    String.format(
                            "&c☀ PHOENIX &8| &eLOCKED &f%d &8| &6VOLLEYS &f%d",
                            lockedTargets.size(),
                            activeVolleys.get()));
            return true;
        });
        Color[] phoenixColors = {
            Color.fromRGB(255, 40, 0),
            Color.fromRGB(255, 120, 0),
            Color.fromRGB(255, 220, 50),
            Color.fromRGB(255, 180, 30)
        };
        boolean finalProjectilesEnabled = projectilesEnabled;
        boolean finalProjectilesDamage = projectilesDamage;
        int finalProjectilesRange = projectilesRange;
        int finalProjectilesDelay = projectilesDelay;
        double finalProjectilesValue = projectilesValue;
        session.runTimer(0L, 1L, () -> {
            int current = step.getAndIncrement();
            Vector directionToKiller =
                    killer.getLocation().toVector().subtract(center.toVector()).normalize();
            double yawToKiller = Math.atan2(-directionToKiller.getX(), directionToKiller.getZ()) + Math.PI;
            double pitchToKiller = Math.asin(-directionToKiller.getY());
            if (current > 240) {
                BukkitParticles.particleSimple(world, "FLASH", center, 3);
                BukkitParticles.particle(
                        world, "EXPLOSION_LARGE", center, 5, 2.0, 2.0, 2.0, 0.0, "EXPLOSION_EMITTER", "EXPLOSION");
                for (int i = 0; i < 150; i++) {
                    double ox = (Math.random() - 0.5) * 6.0;
                    double oy = Math.random() * 3.0;
                    double oz = (Math.random() - 0.5) * 6.0;
                    BukkitParticles.particle(
                            world,
                            "FALLING_LAVA",
                            center.clone().add(ox, oy + 2.0, oz),
                            0,
                            0,
                            0,
                            0,
                            0,
                            "DRIPPING_LAVA",
                            "LAVA");
                    BukkitParticles.particle(
                            world,
                            "FLAME",
                            center.clone().add(ox * 0.5, oy + 1.0, oz * 0.5),
                            0,
                            0,
                            0,
                            0,
                            0.1);
                }
                WorldCompat.playSound(world, center, "entity.generic.explode", 3.0f, 0.8f);
                return false;
            }
            double forwardTilt = Math.toRadians(25.0) + pitchToKiller;
            double phoenixMove = Math.sin(current * 0.1) * 0.3;
            for (double y = 0.0; y <= 4.0; y += 0.12) {
                double bodyRadius = 0.6 + Math.sin(y / 4.0 * Math.PI) * 0.4;
                double tiltedY = y * Math.cos(forwardTilt);
                double tiltedZ = -y * Math.sin(forwardTilt);
                for (double angle = 0.0; angle < Math.PI * 2; angle += 0.2617993877991494) {
                    double x = Math.cos(angle) * bodyRadius;
                    double z = Math.sin(angle) * bodyRadius + tiltedZ;
                    double rotatedX = x * Math.cos(yawToKiller) - z * Math.sin(yawToKiller);
                    double rotatedZ = x * Math.sin(yawToKiller) + z * Math.cos(yawToKiller);
                    spawnDust(
                            world,
                            center.clone().add(rotatedX, tiltedY + phoenixMove, rotatedZ),
                            phoenixColors[current % phoenixColors.length],
                            1.6f);
                    if (Math.random() < 0.1) {
                        BukkitParticles.particle(
                                world,
                                "FLAME",
                                center.clone().add(rotatedX, tiltedY + phoenixMove, rotatedZ),
                                0,
                                0.05,
                                0.0,
                                0.0,
                                0.02);
                    }
                }
            }
            double headY = 4.5 * Math.cos(forwardTilt) + phoenixMove;
            double headZ = -4.5 * Math.sin(forwardTilt);
            double rotatedHeadX = -headZ * Math.sin(yawToKiller);
            double rotatedHeadZ = headZ * Math.cos(yawToKiller);
            Location headPos = center.clone().add(rotatedHeadX, headY, rotatedHeadZ);
            spawnSphere(world, headPos, 0.4, "FLAME");
            spawnSphere(world, headPos, 0.25, phoenixColors[0], 1.8f);
            if (finalProjectilesEnabled) {
                if (projectileTimer.get() <= 0) {
                    for (Player player : world.getPlayers()) {
                        if (player.equals(killer)
                                || player.getLocation().distance(center) > finalProjectilesRange) {
                            continue;
                        }
                        FirePhoenixProjectiles.launchHomingFireCharge(
                                session,
                                headPos,
                                killer,
                                player,
                                finalProjectilesDamage,
                                finalProjectilesValue,
                                lockedTargets,
                                activeVolleys);
                    }
                    projectileTimer.set(finalProjectilesDelay * 20);
                } else {
                    projectileTimer.decrementAndGet();
                }
            }
            double beakX = 0.4 * Math.sin(yawToKiller);
            double beakZ = -0.4 * Math.cos(yawToKiller);
            BukkitParticles.redstoneDust(
                    world, headPos.clone().add(beakX, 0.0, beakZ), Color.fromRGB(200, 100, 0), 1.0f);
            for (int i = 0; i < 5; i++) {
                double crownX = (Math.random() - 0.5) * 0.6;
                double crownZ = (Math.random() - 0.5) * 0.4;
                double rotatedCrownX = crownX * Math.cos(yawToKiller) - crownZ * Math.sin(yawToKiller);
                double rotatedCrownZ = crownX * Math.sin(yawToKiller) + crownZ * Math.cos(yawToKiller);
                BukkitParticles.particle(
                        world,
                        "FLAME",
                        headPos.clone().add(rotatedCrownX, 0.3 + Math.random() * 0.8, rotatedCrownZ),
                        0,
                        0,
                        0,
                        0,
                        0.01);
            }
            double wingBeatSpeed = current * 0.25;
            for (int side = -1; side <= 1; side += 2) {
                for (double wingSection = 0.0; wingSection <= 1.0; wingSection += 0.08) {
                    for (double wingLength = 0.5; wingLength < 7.0; wingLength += 0.3) {
                        double wavePhase = wingBeatSpeed - wingLength * 0.4;
                        double wingBeat = Math.sin(wavePhase);
                        double amplitudeReduction = 1.0 - wingLength / 7.0 * 0.3;
                        wingBeat *= amplitudeReduction;
                        double wingY = wingBeat + 2.0;
                        double wingTiltY = wingY * Math.cos(forwardTilt);
                        double wingTiltZ = -wingY * Math.sin(forwardTilt) * 0.3;
                        double wingCurve = Math.sin(wingSection * Math.PI) * 2.0;
                        double wingX = side * (wingLength + wingCurve);
                        double wingZ = wingTiltZ + Math.cos(wingLength / 7.0 * Math.PI) * 1.2;
                        double rotatedWingX = wingX * Math.cos(yawToKiller) - wingZ * Math.sin(yawToKiller);
                        double rotatedWingZ = wingX * Math.sin(yawToKiller) + wingZ * Math.cos(yawToKiller);
                        double featherMovement = Math.sin(wavePhase + wingSection * Math.PI) * 0.15;
                        double featherY = wingTiltY + featherMovement;
                        Location wingPos = center.clone().add(rotatedWingX, featherY, rotatedWingZ);
                        spawnDust(
                                world,
                                wingPos,
                                phoenixColors[(current + (int) (wingLength * 3.0)) % phoenixColors.length],
                                1.4f);
                        if (Math.abs(Math.cos(wavePhase)) > 0.7 && Math.random() < 0.2) {
                            BukkitParticles.particle(world, "FLAME", wingPos, 0, 0, 0, 0, 0.02);
                        }
                        if (wingLength > 5.0 && wingBeat < -0.3 && Math.random() < 0.15) {
                            BukkitParticles.particle(
                                    world, "CRIT_MAGIC", wingPos, 0, 0, 0, 0, 0, "ENCHANTED_HIT", "CRIT");
                            BukkitParticles.redstoneDust(
                                    world, wingPos, Color.fromRGB(255, 215, 0), 0.8f);
                        }
                    }
                }
            }
            for (double t = 0.0; t < Math.PI; t += 0.15707963267948966) {
                for (double r = 0.3; r < 4.0; r += 0.2) {
                    double tailX = Math.cos(t) * r * 0.9;
                    double tailZ = Math.sin(t) * r - r * 0.8;
                    double tailY = -r * 0.4 + Math.sin(current * 0.15 + r) * 0.2;
                    double tiltedTailY = tailY * Math.cos(forwardTilt) - tailZ * Math.sin(forwardTilt);
                    double tiltedTailZ = tailY * Math.sin(forwardTilt) + tailZ * Math.cos(forwardTilt);
                    double rotatedTailX = tailX * Math.cos(yawToKiller) - tiltedTailZ * Math.sin(yawToKiller);
                    double rotatedTailZ = tailX * Math.sin(yawToKiller) + tiltedTailZ * Math.cos(yawToKiller);
                    Location tailPos = center.clone().add(rotatedTailX, tiltedTailY, rotatedTailZ);
                    spawnDust(
                            world,
                            tailPos,
                            phoenixColors[(current + (int) (r * 15.0)) % phoenixColors.length],
                            1.2f);
                    if (Math.random() < 0.12) {
                        BukkitParticles.particle(world, "FLAME", tailPos, 0, 0, 0, 0, 0.01);
                    }
                }
            }
            if (current > 10) {
                for (int i = 0; i < 8; i++) {
                    double trailX = (Math.random() - 0.5) * 2.0;
                    double trailY = (Math.random() - 0.5) * 6.0;
                    double trailZ = (Math.random() - 0.5) * 2.0;
                    double rotatedTrailX = trailX * Math.cos(yawToKiller) - trailZ * Math.sin(yawToKiller);
                    double rotatedTrailZ = trailX * Math.sin(yawToKiller) + trailZ * Math.cos(yawToKiller);
                    BukkitParticles.particle(
                            world,
                            "FLAME",
                            center.clone().add(rotatedTrailX, trailY, rotatedTrailZ),
                            0,
                            0,
                            0,
                            0,
                            0.05);
                }
            }
            return true;
        });
    }

    private static void spawnDust(World world, Location location, Color color, float size) {
        BukkitParticles.redstoneDust(world, location, color, size);
    }

    private static void spawnSphere(World world, Location center, double radius, Color color, float size) {
        for (double phi = 0.0; phi < Math.PI; phi += 0.39269908169872414) {
            for (double theta = 0.0; theta < Math.PI * 2; theta += 0.39269908169872414) {
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                BukkitParticles.redstoneDust(world, center.clone().add(x, y, z), color, size);
            }
        }
    }

    private static void spawnSphere(World world, Location center, double radius, String particle) {
        for (double phi = 0.0; phi < Math.PI; phi += 0.39269908169872414) {
            for (double theta = 0.0; theta < Math.PI * 2; theta += 0.39269908169872414) {
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                BukkitParticles.particle(world, particle, center.clone().add(x, y, z), 0, 0, 0, 0, 0.01);
            }
        }
    }
}
