package com.monkey.ktplus.effects.list.aurafarming.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class AuraHomingTrail {
    private static final int LEVITATION_REFRESH_TICKS = 40;

    private AuraHomingTrail() {}

    public static void start(
            EffectSession session,
            VisualEffectService visuals,
            ConfigurationSection section,
            Player killer,
            Player target,
            Set<UUID> lockedTargets,
            Set<UUID> levitatingTargets) {
        boolean configuredPushEnabled = true;
        double configuredDamagePerSecond = 2.0;
        if (section != null) {
            ConfigurationSection perks = section.getConfigurationSection("perks");
            ConfigurationSection push = perks != null ? perks.getConfigurationSection("push") : null;
            if (push == null) {
                push = section.getConfigurationSection("push-settings");
            }
            if (push != null) {
                configuredPushEnabled = push.getBoolean("enabled", true);
                configuredDamagePerSecond = push.getDouble("damage-per-seconds", 2.0);
            }
        }
        final boolean pushEnabled = configuredPushEnabled;
        final double damagePerSecond = configuredDamagePerSecond;
        Location headPosition = killer.getLocation().clone().add(0, 1.5, 0);
        boolean[] connected = {false};
        AtomicInteger ticks = new AtomicInteger();
        AtomicInteger damageTicks = new AtomicInteger();
        lockedTargets.add(target.getUniqueId());
        session.runTimer(0L, 1L, () -> {
            int current = ticks.incrementAndGet();
            if (current > 180 || !target.isOnline() || target.isDead() || !killer.isOnline() || killer.isDead()) {
                removeLevitation(target);
                levitatingTargets.remove(target.getUniqueId());
                lockedTargets.remove(target.getUniqueId());
                return false;
            }
            Location baseStart = killer.getLocation().clone().add(0, 1.5, 0);
            Location targetLocation = target.getLocation().clone().add(0, 1.2, 0);
            if (baseStart.getWorld() == null
                    || targetLocation.getWorld() == null
                    || !baseStart.getWorld().equals(targetLocation.getWorld())) {
                removeLevitation(target);
                levitatingTargets.remove(target.getUniqueId());
                lockedTargets.remove(target.getUniqueId());
                return false;
            }
            Location end = connected[0] ? targetLocation : headPosition;
            if (!connected[0]) {
                Vector toTarget = targetLocation.toVector().subtract(headPosition.toVector());
                if (toTarget.length() < 1.5) {
                    connected[0] = true;
                    damageTicks.set(0);
                    if (pushEnabled) {
                        applyLevitation(target);
                        levitatingTargets.add(target.getUniqueId());
                    }
                    end = targetLocation;
                } else {
                    Vector direction = toTarget.clone().normalize().multiply(0.50);
                    Vector motion = direction
                            .add(oscillation(current, 0.22, 0.14))
                            .add(new Vector(0, Math.cos(current * 0.1) * 0.16, 0));
                    headPosition.add(motion);
                    end = headPosition;
                }
            } else if (pushEnabled) {
                maintainLevitation(target);
                levitatingTargets.add(target.getUniqueId());
                if (damageTicks.incrementAndGet() % 20 == 0) {
                    target.damage(damagePerSecond, killer);
                }
                Vector pushDirection = target.getLocation()
                        .toVector()
                        .subtract(killer.getLocation().toVector());
                if (pushDirection.lengthSquared() > 1.0e-6) {
                    Vector pushVelocity = pushDirection.normalize().multiply(0.15);
                    pushVelocity.setY(0);
                    target.setVelocity(pushVelocity);
                }
            }
            drawFloatingWaveLink(visuals, baseStart, end, current, connected[0]);
            return true;
        });
    }

    private static void drawFloatingWaveLink(
            VisualEffectService visuals, Location start, Location end, int tick, boolean linked) {
        if (start.getWorld() == null || end.getWorld() == null) {
            return;
        }
        Vector delta = end.toVector().subtract(start.toVector());
        double length = delta.length();
        if (length < 0.05) {
            return;
        }
        Vector axis = delta.clone().multiply(1.0 / length);
        Vector side = axis.clone().crossProduct(new Vector(0, 1, 0));
        if (side.lengthSquared() < 1.0e-6) {
            side = axis.clone().crossProduct(new Vector(1, 0, 0));
        }
        side.normalize();
        Vector lift = axis.clone().crossProduct(side).normalize();

        int segments = Math.max(18, Math.min(42, (int) (length * 2.4)));
        double waves = 2.4 + Math.min(2.0, length * 0.08);
        double amp = linked ? 0.42 : 0.28;
        double time = tick * 0.22;
        Color linkColor = Color.fromRGB(170, 230, 255);

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double envelope = Math.sin(Math.PI * t);
            double phase = t * Math.PI * waves + time;
            double wave = Math.sin(phase) * amp * envelope;
            double bob = Math.cos(phase * 0.55 + time * 0.35) * amp * 0.55 * envelope;
            Vector point = start.toVector()
                    .add(axis.clone().multiply(length * t))
                    .add(side.clone().multiply(wave))
                    .add(lift.clone().multiply(bob + Math.sin(time * 0.4 + t * 4.0) * 0.08));
            Location loc = point.toLocation(start.getWorld());
            visuals.particle("END_ROD", loc, 1, 0, 0, 0, 0, null);
            visuals.particle("DUST", loc, 1, 0, 0, 0, 0, linkColor);
        }
    }

    private static void applyLevitation(Player target) {
        PotionEffectType levitation = PotionTypes.levitation();
        if (levitation == null) {
            return;
        }
        target.addPotionEffect(new PotionEffect(levitation, LEVITATION_REFRESH_TICKS, 0, false, false, true));
    }

    private static void maintainLevitation(Player target) {
        PotionEffectType levitation = PotionTypes.levitation();
        if (levitation == null) {
            return;
        }
        target.addPotionEffect(new PotionEffect(levitation, LEVITATION_REFRESH_TICKS, 0, false, false, true));
    }

    private static void removeLevitation(Player target) {
        PotionEffectType levitation = PotionTypes.levitation();
        if (levitation != null && target.hasPotionEffect(levitation)) {
            target.removePotionEffect(levitation);
        }
    }

    private static Vector oscillation(double time, double strength, double freq) {
        return new Vector(
                Math.sin(time * freq) * strength,
                Math.cos(time * freq * 0.5) * strength * 0.6,
                Math.cos(time * freq) * strength);
    }
}
