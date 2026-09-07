package com.monkey.ktplus.effects.list.sniper.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.sniper.SniperKillEffect;
import com.monkey.ktplus.effects.list.sniper.animation.util.SniperDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.resourcepack.ResourcePackSettings;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public final class SniperAnimation {
    private static final int DEFAULT_RISE_TICKS = 28;
    private static final int DEFAULT_TILT_TICKS = 32;
    private static final int DEFAULT_FIRE_TICKS = 90;
    private static final int DEFAULT_CLOSE_TICKS = 30;
    private static final double DEFAULT_HEAD_HEIGHT = 1.62;
    private static final double DEFAULT_SEEK_RANGE = 24.0;
    private static final int DEFAULT_FIRE_INTERVAL = 22;
    private static final double DEFAULT_ARROW_SPEED = 2.8;
    private static final double DEFAULT_TRACK_LEAD = 0.2;
    private static final double TRACK_LERP = 0.55;

    private static final Color TRACE = Color.fromRGB(180, 220, 255);
    private static final Color BEAM = Color.fromRGB(120, 200, 255);
    private static final Color MUZZLE = Color.fromRGB(255, 230, 140);

    private SniperAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location base = context.location().clone().add(0.5, 0.05, 0.5);
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("sniper");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int riseTicks = perkInt(perks, "rise-ticks", DEFAULT_RISE_TICKS, 1);
        int tiltTicks = perkInt(perks, "tilt-ticks", DEFAULT_TILT_TICKS, 1);
        int fireTicks = perkInt(perks, "fire-ticks", DEFAULT_FIRE_TICKS, 1);
        int closeTicks = perkInt(perks, "close-ticks", DEFAULT_CLOSE_TICKS, 1);
        double headHeight = perkDouble(perks, "head-height", DEFAULT_HEAD_HEIGHT, 0.8);
        double seekRange = perkDouble(perks, "seek-range", DEFAULT_SEEK_RANGE, 1.0);
        int fireInterval = perkInt(perks, "fire-interval-ticks", DEFAULT_FIRE_INTERVAL, 1);
        double arrowSpeed = perkDouble(perks, "arrow-speed", DEFAULT_ARROW_SPEED, 0.2);
        double trackLead = perkDouble(perks, "track-lead", DEFAULT_TRACK_LEAD, 0.0);

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("sniper");
        double arrowDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 4.0;

        ItemDisplay turret = SniperDisplays.spawnTurret(base.clone());
        if (turret == null) {
            return;
        }
        session.trackEntity(turret);

        int total = riseTicks + tiltTicks + fireTicks + closeTicks;
        AtomicInteger tick = new AtomicInteger();
        AtomicInteger sinceShot = new AtomicInteger(fireInterval);
        boolean[] finished = {false};
        double[] yaw = {0.0};
        double[] pitch = {-Math.PI / 2.0};
        double[] height = {0.05};
        Vector aimDir = new Vector(0.0, 1.0, 0.0);

        String bowHit = ResourcePackSettings.soundName(context.config().resourcePack(), "bow-hit", "kt.hs1");

        session.onCleanup(() -> {
            SniperDisplays.remove(turret);
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(total + 50L);

        visuals.sound("ITEM_CROSSBOW_LOADING_START", base, 0.9f, 1.15f);
        visuals.dust(base, TRACE, 1.1f, ParticleScale.scale(8), 0.2, 0.05, 0.2, 0.0);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            if (!turret.isValid() || turret.isDead()) {
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                SniperDisplays.remove(turret);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            Location turretLoc = fixedPivot(base, height[0]);

            if (current < riseTicks) {
                double t = (current + 1) / (double) riseTicks;
                double ease = smoothstep(t);
                height[0] = 0.05 + ease * headHeight;
                pitch[0] = -Math.PI / 2.0;
                yaw[0] = 0.0;
                aimDir.copy(SniperDisplays.directionFromYawPitch(yaw[0], pitch[0]));
                turretLoc = fixedPivot(base, height[0]);
                SniperDisplays.place(turret, turretLoc, aimDir);
                if (current % 4 == 0) {
                    visuals.dust(turretLoc, TRACE, 0.85f, 2, 0.04, 0.06, 0.04, 0.0);
                }
                showBar(killer, "RISE", (int) (ease * 100), null, 0);
            } else if (current < riseTicks + tiltTicks) {
                int local = current - riseTicks;
                double t = (local + 1) / (double) tiltTicks;
                double ease = smoothstep(t);
                height[0] = headHeight;
                pitch[0] = -Math.PI / 2.0 + ease * (Math.PI / 2.0);
                yaw[0] = 0.0;
                aimDir.copy(SniperDisplays.directionFromYawPitch(yaw[0], pitch[0]));
                turretLoc = fixedPivot(base, height[0]);
                SniperDisplays.place(turret, turretLoc, aimDir);
                if (local == 0) {
                    visuals.sound("ITEM_CROSSBOW_LOADING_MIDDLE", turretLoc, 0.85f, 1.25f);
                }
                if (local % 2 == 0) {
                    drawAimBeam(visuals, turretLoc, aimDir, null, 4.0, current);
                }
                showBar(killer, "DEPLOY", (int) (ease * 100), null, 0);
            } else if (current < riseTicks + tiltTicks + fireTicks) {
                height[0] = headHeight;
                turretLoc = fixedPivot(base, height[0]);
                Player target = findNearest(world, turretLoc, killer, victimId, seekRange, session);

                Vector desired = aimDir.clone();
                if (target != null) {
                    Location aimPoint = predictAimPoint(target, turretLoc, arrowSpeed, trackLead);
                    Vector to = aimPoint.toVector().subtract(turretLoc.toVector());
                    if (to.lengthSquared() > 1.0e-6) {
                        desired = to.normalize();
                    }
                } else {
                    desired = SniperDisplays.directionFromYawPitch(yaw[0], 0.0);
                }

                aimDir.copy(lerpDir(aimDir, desired, TRACK_LERP));
                yaw[0] = Math.atan2(-aimDir.getX(), aimDir.getZ());
                pitch[0] = -Math.asin(clamp(aimDir.getY(), -1.0, 1.0));
                SniperDisplays.place(turret, turretLoc, aimDir);
                drawAimBeam(visuals, turretLoc, aimDir, target, seekRange, current);

                int fireLocal = current - riseTicks - tiltTicks;
                if (target != null && sinceShot.incrementAndGet() >= fireInterval) {
                    sinceShot.set(0);
                    Location muzzle = turretLoc.clone().add(aimDir.clone().normalize().multiply(0.55));
                    fireArrow(
                            session,
                            visuals,
                            world,
                            muzzle,
                            target,
                            killer,
                            arrowSpeed,
                            trackLead,
                            arrowDamage,
                            bowHit);
                }
                int remaining = riseTicks + tiltTicks + fireTicks - current;
                showBar(killer, "TRACK", Math.min(100, fireLocal * 100 / fireTicks), target, remaining);
            } else {
                int local = current - riseTicks - tiltTicks - fireTicks;
                double t = (local + 1) / (double) closeTicks;
                double ease = smoothstep(t);
                if (local == 0) {
                    visuals.sound("ITEM_CROSSBOW_LOADING_END", turretLoc, 0.9f, 0.85f);
                }
                pitch[0] = lerp(pitch[0], -Math.PI / 2.0, ease);
                yaw[0] = lerpAngle(yaw[0], 0.0, ease);
                if (ease < 0.45) {
                    height[0] = headHeight;
                } else {
                    double down = (ease - 0.45) / 0.55;
                    height[0] = headHeight * (1.0 - down * down) - 0.15 * down;
                }
                aimDir.copy(SniperDisplays.directionFromYawPitch(yaw[0], pitch[0]));
                turretLoc = fixedPivot(base, Math.max(-0.35, height[0]));
                SniperDisplays.place(turret, turretLoc, aimDir);
                showBar(killer, "STOW", (int) (ease * 100), null, 0);
                if (local >= closeTicks - 1) {
                    visuals.sound("BLOCK_IRON_TRAPDOOR_CLOSE", base, 0.7f, 1.3f);
                    visuals.particle("CLOUD", base, ParticleScale.scale(6), 0.2, 0.1, 0.2, 0.02, null);
                    SniperDisplays.remove(turret);
                    finished[0] = true;
                    PerkActionBar.clear(killer);
                    return false;
                }
            }
            return true;
        });
    }

    private static void drawAimBeam(
            VisualEffectService visuals,
            Location origin,
            Vector aimDir,
            @Nullable Player target,
            double maxRange,
            int tick) {
        Vector dir = aimDir.clone();
        if (dir.lengthSquared() < 1.0e-8) {
            return;
        }
        dir.normalize();
        double length = maxRange;
        if (target != null && target.isOnline() && !target.isDead() && target.getWorld() != null
                && origin.getWorld() != null
                && target.getWorld().equals(origin.getWorld())) {
            length = Math.min(maxRange, Math.max(1.5, origin.distance(headPoint(target))));
        }
        int steps = Math.max(6, Math.min(28, (int) (length * 2.2)));
        Location muzzle = origin.clone().add(dir.clone().multiply(0.35));
        visuals.dust(muzzle, MUZZLE, 0.9f, 2, 0.03, 0.03, 0.03, 0.0);
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Location point = muzzle.clone().add(dir.clone().multiply(length * t));
            if (i % 2 == tick % 2) {
                visuals.dust(point, BEAM, 0.7f + (float) (0.25 * (1.0 - t)), 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (i % 3 == 0) {
                visuals.particle("END_ROD", point, 1, 0.0, 0.0, 0.0, 0.0, null);
            }
        }
        Location tip = muzzle.clone().add(dir.clone().multiply(length));
        visuals.dust(tip, TRACE, 1.05f, ParticleScale.scale(3), 0.04, 0.04, 0.04, 0.0);
    }

    private static Location fixedPivot(Location base, double height) {
        Location at = base.clone();
        at.setY(base.getY() + height);
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return at;
    }

    private static void fireArrow(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location muzzle,
            Player target,
            @Nullable Player killer,
            double arrowSpeed,
            double trackLead,
            double arrowDamage,
            String bowHitSound) {
        Location aimPoint = predictAimPoint(target, muzzle, arrowSpeed, trackLead);
        Vector dir = aimPoint.toVector().subtract(muzzle.toVector());
        if (dir.lengthSquared() < 1.0e-6) {
            dir = new Vector(0.0, 0.0, 1.0);
        } else {
            dir.normalize();
        }
        Location spawnAt = muzzle.clone().add(dir.clone().multiply(0.1));
        final Vector flight = dir.clone().multiply(arrowSpeed);
        Arrow arrow = EntityCompat.spawnArrow(world, spawnAt, dir, (float) arrowSpeed, spawned -> {
            spawned.addScoreboardTag(SniperKillEffect.TAG);
            spawned.setShooter(killer);
            spawned.setCritical(true);
            spawned.setDamage(arrowDamage);
            EntityCompat.trySetGravity(spawned, false);
            EntityCompat.trySetPersistent(spawned, false);
            EntityCompat.trySetArrowPickup(spawned, true);
        });
        arrow.setVelocity(flight.clone());
        session.trackEntity(arrow);
        AtomicInteger flightTicks = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (!arrow.isValid() || arrow.isDead() || flightTicks.incrementAndGet() > 50) {
                if (!arrow.isDead()) {
                    arrow.remove();
                }
                return false;
            }
            arrow.setVelocity(flight.clone());
            EntityCompat.trySetGravity(arrow, false);
            return true;
        });

        visuals.sound("ENTITY_ARROW_SHOOT", spawnAt, 1.35f, 0.95f + (float) (Math.random() * 0.35));
        visuals.sound(bowHitSound, spawnAt, 0.55f, 1.25f);
        visuals.dust(spawnAt, MUZZLE, 1.2f, ParticleScale.scale(5), 0.08, 0.08, 0.08, 0.0);
        visuals.particle("CRIT", spawnAt, ParticleScale.scale(4), 0.08, 0.08, 0.08, 0.05, null);
    }

    private static Location headPoint(Player target) {
        return target.getEyeLocation().clone();
    }

    private static Location predictAimPoint(
            Player target, Location from, double arrowSpeed, double trackLead) {
        Location head = headPoint(target);
        Vector vel = target.getVelocity();
        double dist = Math.max(0.75, from.distance(head));
        double flightTicks = dist / Math.max(0.2, arrowSpeed);
        double leadTicks = flightTicks * 0.95 + trackLead * 20.0;
        if (vel != null && vel.lengthSquared() > 1.0e-6) {
            head.add(vel.clone().multiply(leadTicks));
        }
        return head;
    }

    private static @Nullable Player findNearest(
            World world,
            Location origin,
            @Nullable Player killer,
            @Nullable UUID victimId,
            double range,
            EffectSession session) {
        double best = range * range;
        Player nearest = null;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            double d = player.getLocation().distanceSquared(origin);
            if (d > best) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            best = d;
            nearest = player;
        }
        return nearest;
    }

    private static void showBar(
            @Nullable Player killer, String phase, int progress, @Nullable Player target, int remaining) {
        if (killer == null || !killer.isOnline()) {
            return;
        }
        String targetName = target != null ? target.getName() : "-";
        if (remaining > 0) {
            PerkActionBar.show(
                    killer,
                    String.format("&b⌖ SNIPER &8| &f%s &8| &e%s &8| &7%d", phase, targetName, remaining));
        } else {
            PerkActionBar.show(
                    killer,
                    String.format("&b⌖ SNIPER &8| &f%s &8| &a%d%%", phase, Math.max(0, Math.min(100, progress))));
        }
    }

    private static Vector lerpDir(Vector current, Vector desired, double t) {
        Vector a = current.clone();
        if (a.lengthSquared() < 1.0e-8) {
            a = new Vector(0.0, 1.0, 0.0);
        } else {
            a.normalize();
        }
        Vector b = desired.clone();
        if (b.lengthSquared() < 1.0e-8) {
            return a;
        }
        b.normalize();
        Vector mixed = a.multiply(1.0 - t).add(b.multiply(t));
        if (mixed.lengthSquared() < 1.0e-8) {
            return b;
        }
        return mixed.normalize();
    }

    private static double smoothstep(double t) {
        t = clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static int perkInt(ConfigurationSection perks, String key, int def, int min) {
        return Math.max(min, perks == null ? def : perks.getInt(key, def));
    }

    private static double perkDouble(ConfigurationSection perks, String key, double def, double min) {
        return Math.max(min, perks == null ? def : perks.getDouble(key, def));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0.0, 1.0);
    }

    private static double lerpAngle(double a, double b, double t) {
        double diff = b - a;
        while (diff > Math.PI) {
            diff -= Math.PI * 2.0;
        }
        while (diff < -Math.PI) {
            diff += Math.PI * 2.0;
        }
        return a + diff * clamp(t, 0.0, 1.0);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
