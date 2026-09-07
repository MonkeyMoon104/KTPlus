package com.monkey.ktplus.effects.list.railgun.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class RailgunAnimation {
    private static final int DEFAULT_CHARGE_TICKS = 28;
    private static final double DEFAULT_BEAM_LENGTH = 28.0;
    private static final int DEFAULT_PIERCE = 4;
    private static final double DEFAULT_DAMAGE = 7.0;
    private static final double DEFAULT_KNOCKBACK = 1.4;
    private static final double LEAD_SECONDS = 0.12;
    private static final int FIRE_TICKS = 18;
    private static final int AFTERGLOW = 22;

    private static final Color CHARGE_CYAN = Color.fromRGB(60, 230, 255);
    private static final Color CHARGE_WHITE = Color.fromRGB(230, 250, 255);
    private static final Color BEAM_CORE = Color.fromRGB(180, 240, 255);
    private static final Color BEAM_EDGE = Color.fromRGB(40, 140, 255);
    private static final Color BEAM_HOT = Color.fromRGB(255, 220, 120);

    private RailgunAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.2, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("railgun");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int chargeTicks = perks == null
                ? DEFAULT_CHARGE_TICKS
                : Math.max(10, perks.getInt("charge-ticks", DEFAULT_CHARGE_TICKS));
        double beamLength = perks == null
                ? DEFAULT_BEAM_LENGTH
                : Math.max(8.0, perks.getDouble("beam-length", DEFAULT_BEAM_LENGTH));
        int pierce = perks == null
                ? DEFAULT_PIERCE
                : Math.max(1, perks.getInt("pierce-count", DEFAULT_PIERCE));
        double damageBase = perks == null
                ? DEFAULT_DAMAGE
                : Math.max(0.0, perks.getDouble("damage", DEFAULT_DAMAGE));
        double knockback = perks == null
                ? DEFAULT_KNOCKBACK
                : Math.max(0.2, perks.getDouble("knockback", DEFAULT_KNOCKBACK));

        EffectDamageConfig damageCfg = context.config().effectDamage("railgun");
        final double damage = damageCfg.enabled()
                ? Math.max(damageBase, damageCfg.value())
                : damageBase;
        double hitRadius = damageCfg.enabled() ? Math.max(1.0, damageCfg.radius()) : 3.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        AtomicReference<Player> aimTargetRef = new AtomicReference<>(
                findNearest(world, origin, killer, victimId, beamLength + 8.0, session));
        Vector aimDir = initialAim(origin, aimTargetRef.get(), killer);

        AtomicInteger tick = new AtomicInteger();
        Set<UUID> pierced = new HashSet<>();
        boolean[] fired = {false};
        double[] activeLength = {beamLength};
        int total = chargeTicks + FIRE_TICKS + AFTERGLOW;

        session.onCleanup(() -> PerkActionBar.clear(killer));
        session.resetDeadline(total + 15L);

        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 1.1f, 1.5f);
        visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", origin, 0.35f, 1.8f);

        double finalDamage = damage;
        double finalKnock = knockback;
        int finalPierce = pierce;
        double configBeamLength = beamLength;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                PerkActionBar.clear(killer);
                return false;
            }

            Player locked = refreshAimTarget(
                    world, origin, killer, victimId, configBeamLength + 8.0, session, aimTargetRef.get());
            aimTargetRef.set(locked);
            Location aimPoint = updateAimDirection(origin, locked, killer, aimDir);
            double reach = Math.max(
                    configBeamLength,
                    aimPoint != null ? origin.distance(aimPoint) + 1.0 : configBeamLength);
            activeLength[0] = reach;

            if (current < chargeTicks) {
                double progress = (current + 1) / (double) chargeTicks;
                drawCharge(visuals, origin, aimDir, progress, current);
                if (current % 5 == 0) {
                    visuals.sound(
                            "BLOCK_NOTE_BLOCK_PLING",
                            origin,
                            0.45f,
                            0.8f + (float) progress * 1.0f);
                    visuals.sound("BLOCK_BEACON_POWER_SELECT", origin, 0.3f, 1.2f + (float) progress * 0.4f);
                }
                if (killer != null && killer.isOnline() && current % 2 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&b⚡ RAILGUN &8| &fCHARGE &a%d%% &8| &7AIM &f%s",
                                    (int) (progress * 100),
                                    locked != null ? locked.getName() : "LOCK"));
                }
            } else if (current < chargeTicks + FIRE_TICKS) {
                if (!fired[0]) {
                    fired[0] = true;
                    visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", origin, 1.4f, 1.6f);
                    visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.85f, 1.5f);
                    visuals.sound("ENTITY_FIREWORK_ROCKET_BLAST", origin, 1.0f, 1.3f);
                    visuals.particle("FLASH", origin, 1, 0, 0, 0, 0, Color.WHITE);
                }
                int local = current - chargeTicks;
                double extend = Math.min(1.0, (local + 1) / 6.0);
                fireBeam(
                        session,
                        visuals,
                        world,
                        origin,
                        aimDir,
                        reach * extend,
                        hitRadius,
                        finalDamage,
                        finalKnock,
                        finalPierce,
                        killer,
                        victimId,
                        pierced,
                        local);
                if (killer != null && killer.isOnline()) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&b⚡ RAILGUN &8| &cFIRE &8| &ePIERCE &f%d&8/&f%d",
                                    pierced.size(),
                                    finalPierce));
                }
            } else {
                int local = current - chargeTicks - FIRE_TICKS;
                double fade = 1.0 - local / (double) AFTERGLOW;
                drawAfterglow(visuals, origin, aimDir, activeLength[0], fade, local);
            }
            return true;
        });
    }

    private static Vector initialAim(Location origin, Player aimTarget, Player killer) {
        Vector aim;
        if (aimTarget != null) {
            Location predicted = predictAimPoint(aimTarget);
            aim = predicted.toVector().subtract(origin.toVector());
        } else {
            float yaw = killer != null ? killer.getLocation().getYaw() : origin.getYaw();
            double rad = Math.toRadians(yaw);
            aim = new Vector(-Math.sin(rad), 0.05, Math.cos(rad));
        }
        if (aim.lengthSquared() < 1.0e-6) {
            aim = new Vector(0, 0, 1);
        }
        return aim.normalize();
    }

    private static Location updateAimDirection(
            Location origin, Player target, Player killer, Vector dir) {
        Location aimPoint = null;
        Vector next;
        if (target != null && target.isOnline() && !target.isDead()) {
            aimPoint = predictAimPoint(target);
            next = aimPoint.toVector().subtract(origin.toVector());
        } else if (killer != null && killer.isOnline()) {
            float yaw = killer.getLocation().getYaw();
            double rad = Math.toRadians(yaw);
            next = new Vector(-Math.sin(rad), 0.05, Math.cos(rad));
        } else {
            return null;
        }
        if (next.lengthSquared() < 1.0e-6) {
            return aimPoint;
        }
        next.normalize();
        dir.copy(next);
        return aimPoint;
    }

    private static Location predictAimPoint(Player target) {
        Location body = target.getLocation().clone().add(0, 1.0, 0);
        Vector vel = target.getVelocity();
        if (vel != null && vel.lengthSquared() > 1.0e-6) {
            body.add(vel.clone().multiply(LEAD_SECONDS * 20.0));
        }
        return body;
    }

    private static Player refreshAimTarget(
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double range,
            EffectSession session,
            Player previous) {
        if (previous != null
                && previous.isOnline()
                && !previous.isDead()
                && previous.getWorld().equals(world)
                && previous.getLocation().distanceSquared(origin) <= range * range
                && (killer == null || session.allowsWorldMutation(killer, previous.getLocation()))) {
            return previous;
        }
        return findNearest(world, origin, killer, victimId, range, session);
    }

    private static void drawCharge(
            VisualEffectService visuals, Location origin, Vector dir, double progress, int tick) {
        visuals.dust(origin, CHARGE_WHITE, 1.2f + (float) progress, 4, 0.12, 0.12, 0.12, 0.0);
        visuals.particle("END_ROD", origin, ParticleScale.scale(3), 0.15, 0.15, 0.15, 0.01, null);

        int rings = 3;
        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0e-6) {
            right = new Vector(1, 0, 0);
        } else {
            right.normalize();
        }
        Vector up = right.clone().crossProduct(dir).normalize();
        for (int r = 0; r < rings; r++) {
            double along = 0.6 + r * 0.85 * progress;
            double ringR = (0.55 + r * 0.25) * (0.4 + progress * 0.7);
            int pts = ParticleScale.scale(10);
            double spin = tick * 0.25 + r;
            Location ringCenter = origin.clone().add(dir.clone().multiply(along));
            for (int i = 0; i < pts; i++) {
                double a = spin + (Math.PI * 2.0 * i) / pts;
                Vector offset = right.clone().multiply(Math.cos(a) * ringR)
                        .add(up.clone().multiply(Math.sin(a) * ringR));
                Location p = ringCenter.clone().add(offset);
                visuals.dust(p, r == 0 ? CHARGE_WHITE : CHARGE_CYAN, 1.0f, 1, 0.0, 0.0, 0.0, 0.0);
                if (i % 2 == 0) {
                    visuals.particle("END_ROD", p, 1, 0.0, 0.0, 0.0, 0.0, null);
                }
            }
        }
        for (double t = 1.0; t < 8.0 * progress; t += 0.7) {
            Location p = origin.clone().add(dir.clone().multiply(t));
            visuals.dust(p, CHARGE_CYAN, 0.65f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void fireBeam(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Vector dir,
            double length,
            double hitRadius,
            double damage,
            double knockback,
            int pierce,
            Player killer,
            UUID victimId,
            Set<UUID> pierced,
            int local) {
        int steps = Math.max(8, ParticleScale.scale((int) (length * 2.2)));
        for (int i = 0; i <= steps; i++) {
            double t = (i / (double) steps) * length;
            Location p = origin.clone().add(dir.clone().multiply(t));
            visuals.dust(p, BEAM_CORE, 1.35f, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 2 == 0) {
                visuals.dust(p, BEAM_EDGE, 1.0f, 1, 0.05, 0.05, 0.05, 0.0);
                visuals.particle("END_ROD", p, 1, 0.02, 0.02, 0.02, 0.0, null);
            }
            if (i % 4 == 0) {
                visuals.dust(p, BEAM_HOT, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        if (local != 0 && local != 3 && local != 7) {
            return;
        }

        List<Player> hits = new ArrayList<>();
        double step = Math.max(0.8, hitRadius * 0.6);
        for (double t = 0.5; t <= length; t += step) {
            Location sample = origin.clone().add(dir.clone().multiply(t));
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
                if (pierced.contains(player.getUniqueId())) {
                    continue;
                }
                if (player.getLocation().clone().add(0, 1, 0).distanceSquared(sample) > hitRadius * hitRadius) {
                    continue;
                }
                if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                    continue;
                }
                if (!hits.contains(player)) {
                    hits.add(player);
                }
            }
        }

        for (Player player : hits) {
            if (pierced.size() >= pierce) {
                break;
            }
            if (!pierced.add(player.getUniqueId())) {
                continue;
            }
            Location at = player.getLocation().clone().add(0, 1, 0);
            if (damage > 0.0) {
                if (killer != null) {
                    player.damage(damage, killer);
                } else {
                    player.damage(damage);
                }
            }
            Vector push = dir.clone().multiply(knockback).setY(0.35);
            player.setVelocity(player.getVelocity().multiply(0.3).add(push));
            visuals.sound("ENTITY_PLAYER_HURT", at, 1.0f, 1.2f);
            visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", at, 0.5f, 1.8f);
            visuals.dust(at, BEAM_HOT, 1.5f, 10, 0.3, 0.3, 0.3, 0.0);
            visuals.particle("END_ROD", at, 8, 0.2, 0.3, 0.2, 0.05, null);
        }
    }

    private static void drawAfterglow(
            VisualEffectService visuals, Location origin, Vector dir, double length, double fade, int local) {
        int steps = ParticleScale.scale((int) (length * 1.2 * fade));
        for (int i = 0; i <= steps; i++) {
            double t = (i / (double) Math.max(1, steps)) * length;
            Location p = origin.clone().add(dir.clone().multiply(t));
            visuals.dust(p, BEAM_EDGE, (float) (0.7 * fade), 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 3 == 0 && local % 2 == 0) {
                visuals.particle("ELECTRIC_SPARK", p, 1, 0.05, 0.05, 0.05, 0.0, null);
            }
        }
    }

    private static Player findNearest(
            World world, Location origin, Player killer, UUID victimId, double range, EffectSession session) {
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
}
