package com.monkey.ktplus.effects.list.hearts.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class HeartsAnimation {
    private static final int DEFAULT_COUNT = 10;
    private static final int DEFAULT_MAX_TICKS = 110;
    private static final double DEFAULT_SEEK_RADIUS = 12.0;
    private static final double DEFAULT_HEAL_PER_HEART = 2.0;
    private static final double DEFAULT_SPEED = 0.48;
    private static final double HIT_DISTANCE = 1.05;
    private static final int TRAIL_LENGTH = 8;
    private static final int OUTBOUND_TICKS = 12;
    private static final int MIN_FLIGHT_TICKS = 16;
    private static final Color TRAIL_RED = Color.fromRGB(220, 40, 55);
    private static final Color TRAIL_DEEP = Color.fromRGB(150, 18, 38);
    private static final Vector UP = new Vector(0, 1, 0);

    private HeartsAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("hearts");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int count = perks == null ? DEFAULT_COUNT : Math.max(1, perks.getInt("count", DEFAULT_COUNT));
        int maxTicks = perks == null ? DEFAULT_MAX_TICKS : Math.max(30, perks.getInt("max-ticks", DEFAULT_MAX_TICKS));
        double seekRadius = perks == null
                ? DEFAULT_SEEK_RADIUS
                : Math.max(1.0, perks.getDouble("seek-radius", DEFAULT_SEEK_RADIUS));
        double healPerHeart = perks == null
                ? DEFAULT_HEAL_PER_HEART
                : Math.max(0.0, perks.getDouble("heal-per-heart", DEFAULT_HEAL_PER_HEART));
        double speed = perks == null ? DEFAULT_SPEED : Math.max(0.15, perks.getDouble("speed", DEFAULT_SPEED));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damage = context.config().effectDamage("hearts");
        double damagePerHeart = damage.enabled() ? Math.max(0.0, damage.value()) : 0.0;

        List<Player> nearby = findNearbyEnemies(world, origin, killer, victimId, seekRadius, session);
        boolean attackMode = !nearby.isEmpty();

        List<HeartProj> hearts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / count + 0.17;
            double burst = 0.85 + (i % 3) * 0.35;
            Location start = origin.clone().add(
                    Math.cos(angle) * burst,
                    0.35 + (i % 5) * 0.22,
                    Math.sin(angle) * burst);

            HeartMode mode = attackMode ? HeartMode.ATTACK : HeartMode.HEAL;
            UUID targetId;
            if (attackMode) {
                targetId = nearby.get(i % nearby.size()).getUniqueId();
            } else {
                targetId = killer != null ? killer.getUniqueId() : null;
            }

            Vector outbound = new Vector(Math.cos(angle), 0.55 + (i % 4) * 0.12, Math.sin(angle)).normalize();
            hearts.add(new HeartProj(
                    start,
                    mode,
                    targetId,
                    i,
                    i * 2,
                    outbound,
                    0.55 + (i % 5) * 0.18,
                    0.7 + (i % 4) * 0.25,
                    0.88 + (i % 3) * 0.1,
                    (Math.PI * 2.0 * i) / count,
                    0.35 + (i % 3) * 0.25));
        }

        visuals.sound("ENTITY_VILLAGER_CELEBRATE", origin, 1.6f, 1.15f);
        visuals.sound("ENTITY_EXPERIENCE_ORB_PICKUP", origin, 0.7f, 1.4f);

        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= maxTicks || hearts.isEmpty()) {
                return false;
            }

            hearts.removeIf(heart -> updateHeart(
                    session,
                    visuals,
                    world,
                    killer,
                    victimId,
                    heart,
                    seekRadius,
                    speed,
                    healPerHeart,
                    damagePerHeart));
            return !hearts.isEmpty();
        });
    }

    private static boolean updateHeart(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            HeartProj heart,
            double seekRadius,
            double speed,
            double healPerHeart,
            double damagePerHeart) {
        if (heart.done) {
            return true;
        }

        if (heart.age < heart.launchDelay) {
            Location idle = heart.loc.clone().add(0.0, Math.sin(heart.age * 0.4 + heart.index) * 0.04, 0.0);
            visuals.particle("HEART", idle, 1, 0.0, 0.0, 0.0, 0.0, null);
            heart.age++;
            return false;
        }

        int flightAge = heart.age - heart.launchDelay;
        Player target = resolveTarget(world, killer, victimId, heart, seekRadius, session);
        if (target == null) {
            visuals.particle("SMOKE", heart.loc, 3, 0.08, 0.08, 0.08, 0.01, null);
            heart.done = true;
            return true;
        }

        Location aim = aimPoint(target, heart, flightAge);
        double distance = heart.loc.distance(aim);
        boolean canHit = flightAge >= MIN_FLIGHT_TICKS + heart.index;
        if (canHit && distance <= HIT_DISTANCE) {
            drawTrail(visuals, heart);
            impact(session, visuals, killer, target, heart, healPerHeart, damagePerHeart);
            heart.done = true;
            return true;
        }

        double heartSpeed = speed * heart.speedMul;
        Vector step;
        if (flightAge < OUTBOUND_TICKS + heart.index % 4) {
            
            step = heart.outbound.clone().multiply(heartSpeed * 1.15);
            step.setY(step.getY() + 0.08 + heart.arcBias * 0.05);
        } else {
            Vector toAim = aim.toVector().subtract(heart.loc.toVector());
            if (toAim.lengthSquared() < 1.0e-6) {
                heart.age++;
                return false;
            }
            Vector forward = toAim.clone().normalize();
            Vector side = forward.clone().crossProduct(UP);
            if (side.lengthSquared() < 1.0e-6) {
                side = new Vector(1, 0, 0);
            } else {
                side.normalize();
            }

            double fade = Math.min(1.0, distance / 4.5);
            double sway = Math.sin(flightAge * 0.22 + heart.phase) * heart.sideBias * fade;
            double bob = Math.sin(flightAge * 0.17 + heart.phase * 1.3) * heart.arcBias * fade;
            step = forward.multiply(Math.min(heartSpeed, Math.max(0.18, distance * 0.28)));
            step.add(side.multiply(sway * 0.22));
            step.add(UP.clone().multiply(bob * 0.16));
        }

        heart.loc.add(step);
        pushTrail(heart, heart.loc);
        drawTrail(visuals, heart);
        visuals.particle("HEART", heart.loc, 1, 0.0, 0.0, 0.0, 0.0, null);
        heart.age++;
        return false;
    }

    private static Location aimPoint(Player target, HeartProj heart, int flightAge) {
        
        double ring = 0.55 + (heart.index % 5) * 0.18;
        double spin = heart.phase + flightAge * 0.03;
        double fade = 0.35; 
        return target.getLocation()
                .clone()
                .add(
                        Math.cos(spin) * ring * fade,
                        0.85 + heart.aimYOffset * 0.5,
                        Math.sin(spin) * ring * fade);
    }

    private static void pushTrail(HeartProj heart, Location loc) {
        heart.trail.addLast(loc.clone());
        while (heart.trail.size() > TRAIL_LENGTH) {
            heart.trail.removeFirst();
        }
    }

    private static void drawTrail(VisualEffectService visuals, HeartProj heart) {
        int i = 0;
        int size = heart.trail.size();
        for (Location point : heart.trail) {
            float t = size <= 1 ? 1.0f : i / (float) (size - 1);
            float dustSize = 0.55f + t * 0.7f;
            Color color = t > 0.55f ? TRAIL_RED : TRAIL_DEEP;
            
            visuals.dust(point, color, dustSize, 1, 0.0, 0.0, 0.0, 0.0);
            i++;
        }
    }

    private static Player resolveTarget(
            World world,
            Player killer,
            UUID victimId,
            HeartProj heart,
            double seekRadius,
            EffectSession session) {
        if (heart.mode == HeartMode.HEAL) {
            if (killer != null && killer.isOnline() && !killer.isDead() && killer.getWorld().equals(world)) {
                return killer;
            }
            return null;
        }

        Player locked = heart.targetId == null
                ? null
                : world.getPlayers().stream()
                        .filter(p -> p.getUniqueId().equals(heart.targetId))
                        .findFirst()
                        .orElse(null);
        if (locked != null && locked.isOnline() && !locked.isDead() && locked.getWorld().equals(world)) {
            return locked;
        }

        List<Player> live = findNearbyEnemies(world, heart.loc, killer, victimId, seekRadius, session);
        if (!live.isEmpty()) {
            Player next = live.get(heart.index % live.size());
            heart.targetId = next.getUniqueId();
            return next;
        }
        if (killer != null && killer.isOnline() && !killer.isDead() && killer.getWorld().equals(world)) {
            heart.mode = HeartMode.HEAL;
            heart.targetId = killer.getUniqueId();
            return killer;
        }
        return null;
    }

    private static void impact(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Player target,
            HeartProj heart,
            double healPerHeart,
            double damagePerHeart) {
        Location at = target.getLocation().clone().add(0.0, 1.0, 0.0);
        visuals.particle("HEART", at, 5, 0.25, 0.25, 0.25, 0.02, null);
        visuals.dust(at, TRAIL_RED, 1.25f, 4, 0.12, 0.12, 0.12, 0.0);

        if (heart.mode == HeartMode.HEAL) {
            applyHeal(target, healPerHeart);
            visuals.sound("ENTITY_PLAYER_LEVELUP", at, 0.45f, 1.55f + heart.index * 0.02f);
            visuals.sound("ENTITY_EXPERIENCE_ORB_PICKUP", at, 0.55f, 1.2f);
            return;
        }

        if (killer == null || damagePerHeart <= 0.0) {
            return;
        }
        if (!session.allowsWorldMutation(killer, target.getLocation())) {
            return;
        }
        target.damage(damagePerHeart, killer);
        visuals.sound("ENTITY_PLAYER_HURT", at, 0.75f, 1.1f + heart.index * 0.015f);
        visuals.sound("ENTITY_EXPERIENCE_ORB_PICKUP", at, 0.35f, 0.55f);
    }

    private static void applyHeal(Player player, double amount) {
        if (amount <= 0.0 || player == null || !player.isOnline() || player.isDead()) {
            return;
        }
        AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxAttr != null ? maxAttr.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    private static List<Player> findNearbyEnemies(
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            EffectSession session) {
        double radiusSq = radius * radius;
        List<Player> found = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            found.add(player);
        }
        found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)));
        return found;
    }

    private enum HeartMode {
        ATTACK,
        HEAL
    }

    private static final class HeartProj {
        private final Location loc;
        private HeartMode mode;
        private UUID targetId;
        private final int index;
        private final int launchDelay;
        private final Vector outbound;
        private final double arcBias;
        private final double sideBias;
        private final double speedMul;
        private final double phase;
        private final double aimYOffset;
        private final Deque<Location> trail = new ArrayDeque<>(TRAIL_LENGTH + 1);
        private int age;
        private boolean done;

        private HeartProj(
                Location loc,
                HeartMode mode,
                UUID targetId,
                int index,
                int launchDelay,
                Vector outbound,
                double arcBias,
                double sideBias,
                double speedMul,
                double phase,
                double aimYOffset) {
            this.loc = loc.clone();
            this.mode = mode;
            this.targetId = targetId;
            this.index = index;
            this.launchDelay = launchDelay;
            this.outbound = outbound.clone();
            this.arcBias = arcBias;
            this.sideBias = sideBias;
            this.speedMul = speedMul;
            this.phase = phase;
            this.aimYOffset = aimYOffset;
        }
    }
}
