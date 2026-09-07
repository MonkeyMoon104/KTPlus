package com.monkey.ktplus.effects.list.boomerang.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.boomerang.animation.util.BoomerangDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class BoomerangAnimation {
    private static final int DEFAULT_COUNT = 3;
    private static final double DEFAULT_RANGE = 12.0;
    private static final double DEFAULT_SPEED = 0.55;
    private static final double DEFAULT_HIT_DAMAGE = 2.5;
    private static final double DEFAULT_RETURN_HEAL = 1.0;
    private static final double HIT_DISTANCE = 1.15;
    private static final int TRAIL_LENGTH = 7;
    private static final int MAX_TICKS = 140;

    private static final Color WOOD = Color.fromRGB(170, 120, 65);
    private static final Color SAND = Color.fromRGB(230, 200, 140);
    private static final Color BREEZE = Color.fromRGB(170, 210, 230);

    private BoomerangAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("boomerang");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int count = perks == null ? DEFAULT_COUNT : Math.max(1, Math.min(5, perks.getInt("count", DEFAULT_COUNT)));
        double range = perks == null ? DEFAULT_RANGE : Math.max(3.0, perks.getDouble("range", DEFAULT_RANGE));
        double speed = perks == null ? DEFAULT_SPEED : Math.max(0.2, perks.getDouble("speed", DEFAULT_SPEED));
        double hitDamage = perks == null
                ? DEFAULT_HIT_DAMAGE
                : Math.max(0.0, perks.getDouble("hit-damage", DEFAULT_HIT_DAMAGE));
        double returnHeal = perks == null
                ? DEFAULT_RETURN_HEAL
                : Math.max(0.0, perks.getDouble("return-heal", DEFAULT_RETURN_HEAL));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("boomerang");
        double splash = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 0.0;

        List<Boomer> boomers = new ArrayList<>(count);
        List<ItemDisplay> displays = new ArrayList<>(count);
        Location launchFrom = killer != null && killer.isOnline() && killer.getWorld().equals(world)
                ? killer.getLocation().clone().add(0.0, 1.1, 0.0)
                : origin.clone();

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / count + 0.2;
            Vector out = new Vector(Math.cos(angle), 0.12 + (i % 3) * 0.05, Math.sin(angle)).normalize();
            ItemDisplay display = BoomerangDisplays.spawn(launchFrom, BoomerangDisplays.resolveBoomerangMaterial(i));
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            boomers.add(new Boomer(display, launchFrom.clone(), out, i, i * 3));
        }

        if (boomers.isEmpty()) {
            return;
        }

        session.onCleanup(() -> BoomerangDisplays.removeAll(displays));
        visuals.sound("ENTITY_BREEZE_SHOOT", launchFrom, 0.9f, 1.2f);
        visuals.sound("ITEM_TRIDENT_THROW", launchFrom, 0.7f, 1.45f);
        session.resetDeadline(MAX_TICKS + 20L);

        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= MAX_TICKS || boomers.stream().allMatch(b -> b.done)) {
                BoomerangDisplays.removeAll(displays);
                return false;
            }

            for (Boomer boom : boomers) {
                updateBoomer(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        origin,
                        boom,
                        range,
                        speed,
                        hitDamage,
                        splash,
                        returnHeal,
                        current);
            }
            return true;
        });
    }

    private static void updateBoomer(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location deathOrigin,
            Boomer boom,
            double range,
            double speed,
            double hitDamage,
            double splash,
            double returnHeal,
            int globalTick) {
        if (boom.done) {
            return;
        }
        if (globalTick < boom.launchDelay) {
            BoomerangDisplays.place(boom.display, boom.loc, globalTick * 0.4f, 0.3f, globalTick * 0.5f, 0.45f);
            return;
        }

        int age = boom.age++;
        Location catchPoint = killer != null && killer.isOnline() && killer.getWorld().equals(world)
                ? killer.getLocation().clone().add(0.0, 1.05, 0.0)
                : deathOrigin.clone();

        if (!boom.returning) {
            double outward = boom.loc.distance(boom.start);
            Vector forward = boom.outDir.clone();
            Vector side = forward.clone().crossProduct(new Vector(0, 1, 0));
            if (side.lengthSquared() < 1.0e-6) {
                side = new Vector(1, 0, 0);
            } else {
                side.normalize();
            }
            double curve = Math.sin(age * 0.18 + boom.index) * 0.28;
            Vector step = forward.multiply(speed).add(side.multiply(curve * 0.35));
            step.setY(step.getY() + Math.cos(age * 0.15 + boom.index) * 0.04);
            boom.loc.add(step);

            if (outward >= range || age > 55) {
                boom.returning = true;
                boom.hitIds.clear();
            }
        } else {
            Vector toCatch = catchPoint.toVector().subtract(boom.loc.toVector());
            double dist = toCatch.length();
            if (dist <= HIT_DISTANCE) {
                catchBoomer(session, visuals, killer, boom, returnHeal);
                boom.done = true;
                BoomerangDisplays.remove(boom.display);
                return;
            }
            if (dist > 0.001) {
                Vector step = toCatch.normalize().multiply(Math.min(speed * 1.15, dist * 0.35 + 0.12));
                Vector side = step.clone().crossProduct(new Vector(0, 1, 0));
                if (side.lengthSquared() > 1.0e-6) {
                    side.normalize().multiply(Math.sin(age * 0.25) * 0.12);
                    step.add(side);
                }
                boom.loc.add(step);
            }
        }

        float spin = age * 0.55f + boom.index;
        BoomerangDisplays.place(boom.display, boom.loc, spin, 0.85f, spin * 0.4f, 0.55f);
        pushTrail(boom, boom.loc);
        drawTrail(visuals, boom);

        if (age % 2 == 0) {
            visuals.dust(boom.loc, boom.index % 2 == 0 ? WOOD : BREEZE, 0.85f, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (age % 6 == 0) {
            visuals.sound("ENTITY_BREEZE_IDLE_AIR", boom.loc, 0.18f, 1.4f + boom.index * 0.05f);
        }

        damageNearby(session, visuals, world, killer, victimId, boom, hitDamage, splash);
    }

    private static void damageNearby(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Boomer boom,
            double hitDamage,
            double splash) {
        if (killer == null || (hitDamage <= 0.0 && splash <= 0.0)) {
            return;
        }
        double dmg = Math.max(hitDamage, splash);
        for (Player player : world.getPlayers()) {
            if (player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (boom.hitIds.contains(player.getUniqueId())) {
                continue;
            }
            if (player.getLocation().clone().add(0, 1, 0).distanceSquared(boom.loc) > HIT_DISTANCE * HIT_DISTANCE) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            boom.hitIds.add(player.getUniqueId());
            player.damage(dmg, killer);
            visuals.particle("CRIT", player.getLocation().add(0, 1, 0), 8, 0.2, 0.25, 0.2, 0.05, null);
            visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", boom.loc, 0.55f, 1.3f);
        }
    }

    private static void catchBoomer(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Boomer boom,
            double returnHeal) {
        Location at = boom.loc.clone();
        visuals.dust(at, SAND, 1.3f, 8, 0.2, 0.15, 0.2, 0.0);
        visuals.particle("CLOUD", at, 6, 0.15, 0.1, 0.15, 0.01, null);
        visuals.sound("ENTITY_ITEM_PICKUP", at, 0.9f, 1.25f);
        visuals.sound("ENTITY_EXPERIENCE_ORB_PICKUP", at, 0.45f, 1.5f);
        if (killer != null && killer.isOnline() && !killer.isDead() && returnHeal > 0.0) {
            AttributeInstance maxAttr = killer.getAttribute(Attribute.MAX_HEALTH);
            double max = maxAttr != null ? maxAttr.getValue() : 20.0;
            killer.setHealth(Math.min(max, killer.getHealth() + returnHeal));
        }
    }

    private static void pushTrail(Boomer boom, Location loc) {
        boom.trail.addLast(loc.clone());
        while (boom.trail.size() > TRAIL_LENGTH) {
            boom.trail.removeFirst();
        }
    }

    private static void drawTrail(VisualEffectService visuals, Boomer boom) {
        int i = 0;
        int size = boom.trail.size();
        for (Location point : boom.trail) {
            float t = size <= 1 ? 1.0f : i / (float) (size - 1);
            Color color = t > 0.5f ? (boom.index % 2 == 0 ? WOOD : BREEZE) : SAND;
            visuals.dust(point, color, 0.55f + t * 0.55f, 1, 0, 0, 0, 0);
            i++;
        }
    }

    private static final class Boomer {
        private final ItemDisplay display;
        private final Location start;
        private final Location loc;
        private final Vector outDir;
        private final int index;
        private final int launchDelay;
        private final Set<UUID> hitIds = new HashSet<>();
        private final Deque<Location> trail = new ArrayDeque<>(TRAIL_LENGTH + 1);
        private int age;
        private boolean returning;
        private boolean done;

        private Boomer(ItemDisplay display, Location start, Vector outDir, int index, int launchDelay) {
            this.display = display;
            this.start = start.clone();
            this.loc = start.clone();
            this.outDir = outDir.clone();
            this.index = index;
            this.launchDelay = launchDelay;
        }
    }
}
