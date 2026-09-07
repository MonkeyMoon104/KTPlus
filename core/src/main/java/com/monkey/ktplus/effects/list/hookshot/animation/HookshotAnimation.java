package com.monkey.ktplus.effects.list.hookshot.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.hookshot.animation.util.HookshotDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public final class HookshotAnimation {
    private static final int MAX_TICKS = 100;
    private static final double DEFAULT_RANGE = 16.0;
    private static final double DEFAULT_PULL = 0.55;
    private static final double DEFAULT_DAMAGE = 3.0;
    private static final int DEFAULT_STUN = 20;
    private static final int LINE_SEGMENTS = 8;

    private static final Color ROPE = Color.fromRGB(210, 190, 140);
    private static final Color HOOK = Color.fromRGB(160, 165, 175);
    private static final Color SNAP = Color.fromRGB(255, 240, 180);

    private static final String[] AMBIENT_SOUNDS = {
        "ENTITY_FISHING_BOBBER_THROW",
        "ENTITY_FISHING_BOBBER_RETRIEVE",
        "ENTITY_FISHING_BOBBER_SPLASH",
        "ENTITY_LEASH_KNOT_PLACE"
    };

    private HookshotAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("hookshot");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double range = perks == null ? DEFAULT_RANGE : Math.max(4.0, perks.getDouble("range", DEFAULT_RANGE));
        double pullSpeed = perks == null ? DEFAULT_PULL : Math.max(0.15, perks.getDouble("pull-speed", DEFAULT_PULL));
        double damageBase = perks == null ? DEFAULT_DAMAGE : Math.max(0.0, perks.getDouble("damage", DEFAULT_DAMAGE));
        int stunTicks = perks == null ? DEFAULT_STUN : Math.max(0, perks.getInt("stun-ticks", DEFAULT_STUN));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("hookshot");
        final double damage = damageCfg.enabled()
                ? Math.max(damageBase, damageCfg.value())
                : damageBase;
        PotionEffectType slowType = PotionTypes.resolve("SLOWNESS", "SLOW");

        Location launchFrom = killer != null && killer.isOnline() && killer.getWorld().equals(world)
                ? killer.getLocation().clone().add(0.0, 1.2, 0.0)
                : origin.clone();

        Player target = findTarget(world, launchFrom, killer, victimId, range, session);
        if (target == null) {
            visuals.sound("ENTITY_FISHING_BOBBER_THROW", launchFrom, 0.7f, 0.8f);
            visuals.dust(launchFrom, ROPE, 1.0f, 6, 0.2, 0.15, 0.2, 0.0);
            return;
        }

        ItemDisplay hook = HookshotDisplays.spawn(launchFrom, HookshotDisplays.hookMaterial(), 0.55f);
        if (hook == null) {
            return;
        }
        session.trackEntity(hook);

        List<ItemDisplay> line = new ArrayList<>(LINE_SEGMENTS);
        for (int i = 0; i < LINE_SEGMENTS; i++) {
            ItemDisplay seg = HookshotDisplays.spawn(launchFrom, HookshotDisplays.lineMaterial(), 0.18f);
            if (seg == null) {
                continue;
            }
            session.trackEntity(seg);
            line.add(seg);
        }

        Location hookLoc = launchFrom.clone();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        boolean[] hooked = {false};
        boolean[] damaged = {false};
        UUID targetId = target.getUniqueId();

        session.onCleanup(() -> {
            stopAmbient(launchFrom);
            HookshotDisplays.remove(hook);
            HookshotDisplays.removeAll(line);
        });

        visuals.sound("ENTITY_FISHING_BOBBER_THROW", launchFrom, 1.0f, 1.15f);
        session.resetDeadline(MAX_TICKS + 20L);

        double finalDamage = damage;
        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopAmbient(launchFrom);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= MAX_TICKS) {
                cleanup(visuals, hookLoc, hook, line);
                finished[0] = true;
                return false;
            }

            Player liveTarget = resolvePlayer(world, targetId);
            Location anchor = killer != null && killer.isOnline() && killer.getWorld().equals(world)
                    ? killer.getLocation().clone().add(0.0, 1.15, 0.0)
                    : launchFrom.clone();

            if (!hooked[0]) {
                if (liveTarget == null || !liveTarget.isOnline() || liveTarget.isDead()) {
                    cleanup(visuals, hookLoc, hook, line);
                    finished[0] = true;
                    return false;
                }
                Location aim = liveTarget.getLocation().clone().add(0, 1.0, 0);
                Vector to = aim.toVector().subtract(hookLoc.toVector());
                double dist = to.length();
                if (dist <= 1.1) {
                    hooked[0] = true;
                    visuals.sound("ENTITY_FISHING_BOBBER_SPLASH", aim, 0.85f, 1.3f);
                    visuals.dust(aim, SNAP, 1.3f, 10, 0.2, 0.2, 0.2, 0.0);
                    if (!damaged[0]
                            && finalDamage > 0.0
                            && killer != null
                            && session.allowsWorldMutation(killer, liveTarget.getLocation())) {
                        damaged[0] = true;
                        liveTarget.damage(finalDamage, killer);
                    }
                } else if (dist > 0.001) {
                    hookLoc.add(to.normalize().multiply(Math.min(0.95, dist)));
                }
            } else {
                if (liveTarget == null || !liveTarget.isOnline() || liveTarget.isDead()) {
                    cleanup(visuals, hookLoc, hook, line);
                    finished[0] = true;
                    return false;
                }
                Location body = liveTarget.getLocation().clone().add(0, 1.0, 0);
                hookLoc.setX(body.getX());
                hookLoc.setY(body.getY());
                hookLoc.setZ(body.getZ());

                Vector pull = anchor.toVector().subtract(liveTarget.getLocation().toVector());
                double dist = pull.length();
                if (dist <= 1.6) {
                    if (stunTicks > 0 && slowType != null && killer != null
                            && session.allowsWorldMutation(killer, liveTarget.getLocation())) {
                        liveTarget.addPotionEffect(new PotionEffect(slowType, stunTicks, 4, true, true, true));
                    }
                    visuals.sound("ENTITY_FISHING_BOBBER_RETRIEVE", anchor, 1.0f, 1.2f);
                    visuals.dust(anchor, ROPE, 1.2f, 8, 0.2, 0.15, 0.2, 0.0);
                    cleanup(visuals, hookLoc, hook, line);
                    finished[0] = true;
                    return false;
                }
                if (killer != null && session.allowsWorldMutation(killer, liveTarget.getLocation())) {
                    Vector step = pull.normalize().multiply(Math.min(pullSpeed, dist * 0.28 + 0.12));
                    step.setY(Math.max(-0.1, Math.min(0.35, step.getY() + 0.08)));
                    liveTarget.setVelocity(step);
                }
            }

            float spin = current * 0.35f;
            HookshotDisplays.place(hook, hookLoc, spin, 0.4f, spin * 0.2f, 0.6f);
            updateLine(line, anchor, hookLoc, current);
            drawRopeDust(visuals, anchor, hookLoc, current);

            if (current % 6 == 0) {
                visuals.sound("ENTITY_LEASH_KNOT_PLACE", hookLoc, 0.2f, 1.5f);
            }
            return true;
        });
    }

    private static void updateLine(List<ItemDisplay> line, Location from, Location to, int tick) {
        int n = line.size();
        if (n == 0) {
            return;
        }
        for (int i = 0; i < n; i++) {
            double t = (i + 1.0) / (n + 1.0);
            Location at = lerp(from, to, t);
            at.add(0, Math.sin(tick * 0.25 + i) * 0.04, 0);
            HookshotDisplays.place(line.get(i), at, tick * 0.1f, 0.2f, 0.0f, 0.16f);
        }
    }

    private static void drawRopeDust(VisualEffectService visuals, Location from, Location to, int tick) {
        int points = ParticleScale.scale(6);
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Location p = lerp(from, to, t);
            visuals.dust(p, i % 2 == 0 ? ROPE : HOOK, 0.7f, 1, 0, 0, 0, 0);
        }
        if (tick % 2 == 0) {
            visuals.particle("CLOUD", to, 1, 0.04, 0.04, 0.04, 0.0, null);
        }
    }

    private static Location lerp(Location a, Location b, double t) {
        return a.clone().add(
                (b.getX() - a.getX()) * t,
                (b.getY() - a.getY()) * t,
                (b.getZ() - a.getZ()) * t);
    }

    private static @Nullable Player findTarget(
            World world, Location from, @Nullable Player killer, @Nullable UUID victimId, double range, EffectSession session) {
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
            double d = player.getLocation().distanceSquared(from);
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

    private static @Nullable Player resolvePlayer(World world, UUID id) {
        for (Player player : world.getPlayers()) {
            if (player.getUniqueId().equals(id)) {
                return player;
            }
        }
        return null;
    }

    private static void cleanup(
            VisualEffectService visuals, Location at, ItemDisplay hook, List<ItemDisplay> line) {
        stopAmbient(at);
        visuals.particle("CLOUD", at, ParticleScale.scale(8), 0.2, 0.15, 0.2, 0.01, null);
        HookshotDisplays.remove(hook);
        HookshotDisplays.removeAll(line);
    }

    private static void stopAmbient(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }
}
