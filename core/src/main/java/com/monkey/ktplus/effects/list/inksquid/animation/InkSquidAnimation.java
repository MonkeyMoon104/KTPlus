package com.monkey.ktplus.effects.list.inksquid.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.inksquid.animation.util.InkSquidDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class InkSquidAnimation {
    private static final int DEFAULT_TENTACLE_COUNT = 8;
    private static final double DEFAULT_SLAP_DAMAGE = 2.0;
    private static final double DEFAULT_SEEK_RANGE = 12.0;
    private static final int DEFAULT_BLIND_TICKS = 40;
    private static final int DEFAULT_LASH_INTERVAL = 8;
    private static final int DEFAULT_DURATION = 140;
    private static final int SEGMENTS = 4;
    private static final double DEFAULT_ATTACK_RADIUS = 3.55;
    private static final double DEFAULT_MIN_CENTER_DISTANCE = 2.85;
    private static final double BODY_CHASE_SPEED = 0.22;

    private static final Color INK_BLACK = Color.fromRGB(18, 16, 22);
    private static final Color INK_PURPLE = Color.fromRGB(55, 25, 70);
    private static final Color INK_DEEP = Color.fromRGB(35, 30, 45);
    private static final Color INK_GLOW = Color.fromRGB(90, 40, 120);

    private InkSquidAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.15, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("inksquid");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int tentacleCount = perks == null
                ? DEFAULT_TENTACLE_COUNT
                : Math.max(6, Math.min(10, perks.getInt("tentacle-count", DEFAULT_TENTACLE_COUNT)));
        double slapDamage = perks == null
                ? DEFAULT_SLAP_DAMAGE
                : Math.max(0.0, perks.getDouble("slap-damage", DEFAULT_SLAP_DAMAGE));
        double seekRange = perks == null
                ? DEFAULT_SEEK_RANGE
                : Math.max(4.0, perks.getDouble("seek-range", DEFAULT_SEEK_RANGE));
        int blindTicks = perks == null
                ? DEFAULT_BLIND_TICKS
                : Math.max(10, perks.getInt("blind-ticks", DEFAULT_BLIND_TICKS));
        int lashInterval = perks == null
                ? DEFAULT_LASH_INTERVAL
                : Math.max(4, perks.getInt("lash-interval", DEFAULT_LASH_INTERVAL));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(60, perks.getInt("duration-ticks", DEFAULT_DURATION));
        double attackRadius = perks == null
                ? DEFAULT_ATTACK_RADIUS
                : Math.max(1.8, perks.getDouble("attack-radius", DEFAULT_ATTACK_RADIUS));
        double configuredMinCenter = perks == null
                ? DEFAULT_MIN_CENTER_DISTANCE
                : Math.max(1.2, perks.getDouble("min-center-distance", DEFAULT_MIN_CENTER_DISTANCE));
        double minCenterDistance = configuredMinCenter >= attackRadius
                ? Math.max(1.2, attackRadius - 0.55)
                : configuredMinCenter;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("inksquid");
        if (damageCfg.enabled()) {
            slapDamage = Math.max(slapDamage, damageCfg.value());
        }
        PotionEffectType blindness = PotionTypes.resolve("BLINDNESS", "BLINDNESS");

        ItemDisplay body = InkSquidDisplays.spawnBody(origin);
        if (body != null) {
            session.trackEntity(body);
        }

        List<Tentacle> tentacles = new ArrayList<>(tentacleCount);
        for (int i = 0; i < tentacleCount; i++) {
            double baseAngle = (Math.PI * 2.0 * i) / tentacleCount;
            List<BlockDisplay> segments = new ArrayList<>(SEGMENTS);
            for (int s = 0; s < SEGMENTS; s++) {
                Location spawn = origin.clone().add(Math.cos(baseAngle) * (0.4 + s * 0.35), -0.2 - s * 0.1, Math.sin(baseAngle) * (0.4 + s * 0.35));
                BlockDisplay seg = InkSquidDisplays.spawnTentacleSegment(spawn);
                if (seg != null) {
                    session.trackEntity(seg);
                    segments.add(seg);
                }
            }
            ItemDisplay tip = InkSquidDisplays.spawnTentacleTip(origin);
            if (tip != null) {
                session.trackEntity(tip);
            }
            tentacles.add(new Tentacle(segments, tip, i, baseAngle, i * 2, 0.4 + (i % 4) * 0.08));
        }

        Set<UUID> slapCooldown = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        Location core = origin.clone();

        session.onCleanup(() -> {
            InkSquidDisplays.removeItem(body);
            for (Tentacle tentacle : tentacles) {
                cleanupTentacle(tentacle);
            }
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_SQUID_SQUIRT", origin, 1.2f, 0.75f);
        visuals.sound("ENTITY_SQUID_AMBIENT", origin, 0.8f, 0.85f);
        session.resetDeadline(durationTicks + 40L);

        double finalSlapDamage = slapDamage;
        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks) {
                dissipate(visuals, core, body, tentacles);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            double bob = Math.sin(current * 0.14) * 0.18;
            chaseBody(world, core, killer, victimId, seekRange, attackRadius, minCenterDistance, session, current);
            Location bodyAt = core.clone().add(0.0, bob, 0.0);
            InkSquidDisplays.placeItem(body, bodyAt, 0.85f + (float) Math.sin(current * 0.1) * 0.08f, current * 0.05f, 0.2f, current * 0.03f);
            drawInkCloud(visuals, bodyAt, current);

            int lashes = 0;
            for (Tentacle tentacle : tentacles) {
                if (updateTentacle(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        bodyAt,
                        tentacle,
                        seekRange,
                        lashInterval,
                        finalSlapDamage,
                        blindTicks,
                        blindness,
                        slapCooldown,
                        current)) {
                    lashes++;
                }
            }

            if (current % 9 == 0) {
                visuals.sound("ENTITY_SQUID_AMBIENT", bodyAt, 0.35f, 0.7f + (float) Math.random() * 0.3f);
                visuals.sound("BLOCK_HONEY_BLOCK_SLIDE", bodyAt, 0.25f, 0.55f);
            }
            if (current % 16 == 0) {
                visuals.sound("ENTITY_SQUID_SQUIRT", bodyAt, 0.45f, 1.1f);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&8🦑 INKSQUID &8| &5LASH &f%d &8| &7%d",
                                lashes,
                                Math.max(0, durationTicks - current)));
            }
            return true;
        });
    }

    private static void chaseBody(
            World world,
            Location core,
            Player killer,
            UUID victimId,
            double seekRange,
            double attackRadius,
            double minCenterDistance,
            EffectSession session,
            int tick) {
        Player nearest = findNearest(world, core, killer, victimId, seekRange, session);
        if (nearest == null) {
            core.add(
                    Math.cos(tick * 0.07) * 0.03,
                    Math.sin(tick * 0.11) * 0.01,
                    Math.sin(tick * 0.07) * 0.03);
            return;
        }
        Location aim = nearest.getLocation().clone().add(0.0, 1.0, 0.0);
        Vector toTarget = aim.toVector().subtract(core.toVector());
        double dist = toTarget.length();
        if (dist < 1.0e-4) {
            toTarget = new Vector(1.0, 0.0, 0.0);
            dist = 1.0;
        }

        Vector move;
        if (dist < minCenterDistance) {
            Vector away = toTarget.clone().multiply(-1.0).normalize();
            Vector tangent = away.clone().crossProduct(new Vector(0, 1, 0));
            if (tangent.lengthSquared() < 1.0e-6) {
                tangent = new Vector(1, 0, 0);
            } else {
                tangent.normalize();
            }
            double push = Math.min(BODY_CHASE_SPEED * 2.1, (minCenterDistance - dist) * 0.55 + 0.14);
            move = away.multiply(push).add(tangent.multiply(Math.sin(tick * 0.21) * 0.14));
            move.setY(move.getY() + Math.sin(tick * 0.17) * 0.03);
        } else if (dist > attackRadius) {
            Vector toward = toTarget.clone().normalize();
            double gap = dist - attackRadius;
            double step = Math.min(BODY_CHASE_SPEED, gap * 0.16 + 0.05);
            move = toward.multiply(step);
            Vector side = toward.clone().crossProduct(new Vector(0, 1, 0));
            if (side.lengthSquared() > 1.0e-6) {
                side.normalize().multiply(Math.sin(tick * 0.13) * 0.05);
                move.add(side);
            }
            move.setY(move.getY() + Math.sin(tick * 0.09) * 0.02);
        } else {
            
            double ideal = (minCenterDistance + attackRadius) * 0.5;
            Vector tangent = toTarget.clone().crossProduct(new Vector(0, 1, 0));
            if (tangent.lengthSquared() < 1.0e-6) {
                tangent = new Vector(1, 0, 0);
            } else {
                tangent.normalize();
            }
            double radialError = dist - ideal;
            Vector radial = toTarget.clone().normalize().multiply(-radialError * 0.16);
            move = tangent.multiply(Math.sin(tick * 0.16) * 0.09).add(radial);
            move.setY(Math.sin(tick * 0.11) * 0.025);
        }
        core.add(move);
    }

    private static boolean updateTentacle(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location body,
            Tentacle tentacle,
            double seekRange,
            int lashInterval,
            double slapDamage,
            int blindTicks,
            PotionEffectType blindness,
            Set<UUID> slapCooldown,
            int tick) {
        boolean lashing = tentacle.lashProgress > 0 || (tick + tentacle.index) % lashInterval == 0;
        if ((tick + tentacle.index) % lashInterval == 0 && tentacle.lashProgress <= 0) {
            tentacle.lashProgress = 10 + (tentacle.index % 4);
            Player nearest = findNearest(world, body, killer, victimId, seekRange, session);
            if (nearest != null) {
                tentacle.targetDir = nearest.getLocation().toVector().add(new Vector(0, 0.9, 0)).subtract(body.toVector());
                if (tentacle.targetDir.lengthSquared() > 1.0e-4) {
                    tentacle.targetDir.normalize();
                } else {
                    tentacle.targetDir = dirFromAngle(tentacle.baseAngle);
                }
            } else {
                tentacle.targetDir = dirFromAngle(tentacle.baseAngle + Math.sin(tick * 0.05 + tentacle.phase) * 0.6);
            }
            visuals.sound("ENTITY_SLIME_JUMP", body, 0.35f, 0.55f + tentacle.index * 0.03f);
        }

        double reach = 1.6 + tentacle.reachBias;
        if (tentacle.lashProgress > 0) {
            double t = 1.0 - tentacle.lashProgress / 12.0;
            reach = 2.2 + tentacle.reachBias + Math.sin(t * Math.PI) * 2.8;
            tentacle.lashProgress--;
        }

        Vector baseDir = tentacle.targetDir == null ? dirFromAngle(tentacle.baseAngle) : tentacle.targetDir.clone();
        double swirl = Math.sin(tick * 0.16 + tentacle.phase) * 0.35;
        Vector side = baseDir.clone().crossProduct(new Vector(0, 1, 0));
        if (side.lengthSquared() > 1.0e-6) {
            side.normalize().multiply(swirl);
            baseDir.add(side);
        }
        if (baseDir.lengthSquared() > 1.0e-6) {
            baseDir.normalize();
        }

        Location tipLoc = body.clone();
        for (int s = 0; s < tentacle.segments.size(); s++) {
            double frac = (s + 1) / (double) Math.max(1, tentacle.segments.size());
            double curl = Math.sin(tick * 0.2 + tentacle.phase + s * 0.55) * 0.35 * (1.0 - frac * 0.3);
            Vector offset = baseDir.clone().multiply(reach * frac);
            offset.setY(offset.getY() - frac * 0.35 + curl * 0.4);
            Location segAt = body.clone().add(offset);
            float scale = 0.34f - s * 0.04f;
            float yaw = (float) Math.atan2(baseDir.getX(), baseDir.getZ());
            float pitch = (float) (Math.sin(tick * 0.18 + s) * 0.4);
            InkSquidDisplays.placeBlock(tentacle.segments.get(s), segAt, scale, yaw, pitch);
            if (s == tentacle.segments.size() - 1) {
                tipLoc = segAt.clone().add(baseDir.clone().multiply(0.25));
            }
            if (tick % 2 == s % 2) {
                visuals.dust(segAt, s % 2 == 0 ? INK_BLACK : INK_PURPLE, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        InkSquidDisplays.placeItem(
                tentacle.tip,
                tipLoc,
                0.4f,
                (float) (tick * 0.1 + tentacle.phase),
                (float) Math.sin(tick * 0.25 + tentacle.phase) * 0.5f,
                (float) (tick * 0.08));

        if (tentacle.lashProgress > 0 && tentacle.lashProgress <= 4) {
            trySlap(session, visuals, world, killer, victimId, tipLoc, slapDamage, blindTicks, blindness, slapCooldown);
        }
        return lashing;
    }

    private static void trySlap(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location tip,
            double slapDamage,
            int blindTicks,
            PotionEffectType blindness,
            Set<UUID> slapCooldown) {
        if (killer == null || slapDamage <= 0.0) {
            return;
        }
        double hitRadius = 1.35;
        double hitSq = hitRadius * hitRadius;
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
            if (player.getLocation().clone().add(0, 1, 0).distanceSquared(tip) > hitSq) {
                continue;
            }
            if (slapCooldown.contains(player.getUniqueId())) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            slapCooldown.add(player.getUniqueId());
            final UUID id = player.getUniqueId();
            session.runLater(14L, () -> slapCooldown.remove(id));
            player.damage(slapDamage, killer);
            Vector knock = player.getLocation().toVector().subtract(tip.toVector());
            if (knock.lengthSquared() > 1.0e-4) {
                player.setVelocity(player.getVelocity().add(knock.normalize().multiply(0.42).setY(0.22)));
            }
            if (blindness != null) {
                player.addPotionEffect(new PotionEffect(blindness, blindTicks, 0, true, true, true));
            }
            visuals.sound("ENTITY_PLAYER_ATTACK_KNOCKBACK", tip, 0.85f, 0.7f);
            visuals.sound("ENTITY_SQUID_HURT", tip, 0.55f, 1.2f);
            visuals.dust(tip, INK_BLACK, 1.5f, ParticleScale.scale(10), 0.3, 0.3, 0.3, 0.0);
            visuals.particle("SQUID_INK", tip, ParticleScale.scale(8), 0.25, 0.25, 0.25, 0.02, null);
        }
    }

    private static void drawInkCloud(VisualEffectService visuals, Location center, int tick) {
        visuals.dust(center, INK_BLACK, 1.35f, ParticleScale.scale(6), 0.55, 0.4, 0.55, 0.0);
        visuals.dust(center, INK_DEEP, 1.1f, ParticleScale.scale(4), 0.7, 0.5, 0.7, 0.0);
        if (tick % 2 == 0) {
            visuals.dust(center.clone().add(0, 0.4, 0), INK_PURPLE, 0.95f, ParticleScale.scale(3), 0.4, 0.3, 0.4, 0.0);
        }
        if (tick % 3 == 0) {
            visuals.dust(center, INK_GLOW, 0.8f, 2, 0.35, 0.25, 0.35, 0.0);
            visuals.particle("SQUID_INK", center, ParticleScale.scale(5), 0.45, 0.35, 0.45, 0.01, null);
        }
        if (tick % 4 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = tick * 0.08 + i;
                Location puff = center.clone().add(Math.cos(a) * 1.1, Math.sin(a * 2) * 0.4, Math.sin(a) * 1.1);
                visuals.dust(puff, INK_BLACK, 1.0f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static Player findNearest(
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            EffectSession session) {
        List<Player> found = new ArrayList<>();
        double radiusSq = radius * radius;
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
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            found.add(player);
        }
        found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)));
        return found.isEmpty() ? null : found.get(0);
    }

    private static Vector dirFromAngle(double angle) {
        return new Vector(Math.cos(angle), -0.15, Math.sin(angle)).normalize();
    }

    private static void dissipate(
            VisualEffectService visuals, Location core, ItemDisplay body, List<Tentacle> tentacles) {
        visuals.sound("ENTITY_SQUID_DEATH", core, 1.0f, 0.85f);
        visuals.particle("SQUID_INK", core, ParticleScale.scale(30), 0.9, 0.7, 0.9, 0.05, null);
        visuals.dust(core, INK_BLACK, 1.7f, ParticleScale.scale(20), 0.8, 0.6, 0.8, 0.0);
        visuals.dust(core, INK_PURPLE, 1.3f, ParticleScale.scale(12), 1.0, 0.7, 1.0, 0.0);
        InkSquidDisplays.removeItem(body);
        for (Tentacle tentacle : tentacles) {
            cleanupTentacle(tentacle);
        }
        tentacles.clear();
    }

    private static void cleanupTentacle(Tentacle tentacle) {
        for (BlockDisplay seg : tentacle.segments) {
            InkSquidDisplays.removeBlock(seg);
        }
        InkSquidDisplays.removeItem(tentacle.tip);
    }

    private static final class Tentacle {
        private final List<BlockDisplay> segments;
        private final ItemDisplay tip;
        private final int index;
        private final double baseAngle;
        private final double phase;
        private final double reachBias;
        private Vector targetDir;
        private int lashProgress;

        private Tentacle(
                List<BlockDisplay> segments,
                ItemDisplay tip,
                int index,
                double baseAngle,
                double phase,
                double reachBias) {
            this.segments = segments;
            this.tip = tip;
            this.index = index;
            this.baseAngle = baseAngle;
            this.phase = phase;
            this.reachBias = reachBias;
            this.targetDir = dirFromAngle(baseAngle);
        }
    }
}
