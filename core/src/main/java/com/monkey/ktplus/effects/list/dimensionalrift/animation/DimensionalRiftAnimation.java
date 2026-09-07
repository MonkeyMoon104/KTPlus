package com.monkey.ktplus.effects.list.dimensionalrift.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.dimensionalrift.animation.util.RiftDisplays;
import com.monkey.ktplus.effects.list.dimensionalrift.animation.util.RiftParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class DimensionalRiftAnimation {
    private static final int DEFAULT_MAX_RIFTS = 8;
    private static final int DEFAULT_DURATION = 160;
    private static final int DEFAULT_OPEN_TICKS = 10;
    private static final int DEFAULT_FIRE_DELAY = 8;
    private static final int DEFAULT_CLOSE_TICKS = 6;
    private static final double DEFAULT_SPAWN_RADIUS = 6.0;
    private static final double DEFAULT_SEEK_RANGE = 28.0;
    private static final double DEFAULT_BOLT_SPEED = 0.95;
    private static final double DEFAULT_BOLT_DAMAGE = 4.0;
    private static final double BOLT_HIT_RADIUS = 1.35;
    private static final int BOLT_MAX_LIFE = 45;

    private enum Phase {
        OPEN,
        HOLD,
        CLOSE,
        RELOCATE
    }

    private DimensionalRiftAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location ground = context.location().clone().add(0.5, 0.0, 0.5);
        World world = ground.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("dimensionalrift");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int maxRifts = perks == null ? DEFAULT_MAX_RIFTS : Math.max(1, perks.getInt("max-rifts", DEFAULT_MAX_RIFTS));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(40, perks.getInt("duration-ticks", DEFAULT_DURATION));
        int openTicks = perks == null
                ? DEFAULT_OPEN_TICKS
                : Math.max(4, perks.getInt("open-ticks", DEFAULT_OPEN_TICKS));
        int fireDelay = perks == null
                ? DEFAULT_FIRE_DELAY
                : Math.max(2, perks.getInt("fire-delay-ticks", DEFAULT_FIRE_DELAY));
        int closeTicks = perks == null
                ? DEFAULT_CLOSE_TICKS
                : Math.max(3, perks.getInt("close-ticks", DEFAULT_CLOSE_TICKS));
        double spawnRadius = perks == null
                ? DEFAULT_SPAWN_RADIUS
                : Math.max(2.0, perks.getDouble("spawn-radius", DEFAULT_SPAWN_RADIUS));
        double seekRange = perks == null
                ? DEFAULT_SEEK_RANGE
                : Math.max(6.0, perks.getDouble("seek-range", DEFAULT_SEEK_RANGE));
        double boltSpeed = perks == null
                ? DEFAULT_BOLT_SPEED
                : Math.max(0.35, perks.getDouble("bolt-speed", DEFAULT_BOLT_SPEED));
        double boltDamage = perks == null
                ? DEFAULT_BOLT_DAMAGE
                : Math.max(0.0, perks.getDouble("bolt-damage", DEFAULT_BOLT_DAMAGE));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("dimensionalrift");
        if (damageCfg.enabled()) {
            boltDamage = Math.max(boltDamage, damageCfg.value());
        }

        session.resetDeadline(durationTicks + 60L);

        RiftState state = new RiftState();
        List<Bolt> bolts = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        AtomicInteger riftCount = new AtomicInteger();
        boolean[] finished = {false};

        openRift(session, visuals, ground, spawnRadius, state, riftCount);

        double finalBoltDamage = boltDamage;
        session.onCleanup(() -> {
            clearRift(state);
            for (Bolt bolt : bolts) {
                RiftDisplays.removeItem(bolt.display);
            }
            bolts.clear();
        });

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                clearRift(state);
                for (Bolt bolt : bolts) {
                    RiftDisplays.removeItem(bolt.display);
                }
                bolts.clear();
                return false;
            }

            int current = tick.getAndIncrement();
            updateBolts(session, visuals, world, killer, victimId, bolts, finalBoltDamage, damageCfg);

            if (finished[0]) {
                clearRift(state);
                if (bolts.isEmpty()) {
                    return false;
                }
                return true;
            }

            if (current >= durationTicks || riftCount.get() > maxRifts) {
                clearRift(state);
                finished[0] = true;
                return !bolts.isEmpty();
            }

            state.phaseTick++;
            float yaw = current * 0.08f;
            double open = state.phase == Phase.OPEN
                    ? Math.min(1.0, (state.phaseTick + 1) / (double) Math.max(1, openTicks))
                    : state.phase == Phase.CLOSE
                            ? Math.max(0.0, 1.0 - (state.phaseTick + 1) / (double) Math.max(1, closeTicks))
                            : 1.0;

            if (state.frame != null || state.core != null) {
                RiftDisplays.placeBlock(state.frame, state.at, 0.55f * (float) open, 1.45f * (float) open, 0.12f, yaw);
                RiftDisplays.placeBlock(state.core, state.at, 0.9f * (float) open, 1.15f * (float) open, 0.9f * (float) open, -yaw * 0.7f);
                RiftParticles.spawnPortalAura(visuals, state.at, current, open);
            }

            if (state.phase == Phase.OPEN) {
                if (state.phaseTick >= openTicks) {
                    state.phase = Phase.HOLD;
                    state.phaseTick = 0;
                    state.fired = false;
                }
                return true;
            }

            if (state.phase == Phase.HOLD) {
                if (!state.fired && state.phaseTick >= fireDelay) {
                    state.fired = true;
                    fireBolt(session, visuals, world, killer, victimId, ground, state.at, seekRange, boltSpeed, bolts);
                    visuals.sound("ENTITY_SHULKER_SHOOT", state.at, 0.9f, 0.75f);
                    visuals.sound("ENTITY_ENDER_EYE_LAUNCH", state.at, 0.7f, 0.65f);
                    state.phase = Phase.CLOSE;
                    state.phaseTick = 0;
                    RiftParticles.spawnCloseBurst(visuals, state.at);
                    visuals.sound("BLOCK_PORTAL_TRIGGER", state.at, 0.45f, 1.55f);
                    visuals.sound("ENTITY_ILLUSIONER_MIRROR_MOVE", state.at, 0.7f, 1.15f);
                }
                return true;
            }

            if (state.phase == Phase.CLOSE) {
                if (state.phaseTick >= closeTicks) {
                    clearRift(state);
                    state.phase = Phase.RELOCATE;
                    state.phaseTick = 0;
                }
                return true;
            }

            if (state.phase == Phase.RELOCATE && state.phaseTick >= 2) {
                if (riftCount.get() >= maxRifts || current >= durationTicks - (openTicks + fireDelay + closeTicks)) {
                    finished[0] = true;
                    return !bolts.isEmpty();
                }
                openRift(session, visuals, ground, spawnRadius, state, riftCount);
            }
            return true;
        });
    }

    private static void openRift(
            EffectSession session,
            VisualEffectService visuals,
            Location ground,
            double spawnRadius,
            RiftState state,
            AtomicInteger riftCount) {
        clearRift(state);
        Location at = randomRiftLocation(ground, spawnRadius, riftCount.get());
        state.at = at;
        state.frame = RiftDisplays.spawnPortalFrame(at);
        state.core = RiftDisplays.spawnPortalCore(at);
        if (state.frame != null) {
            session.trackEntity(state.frame);
        }
        if (state.core != null) {
            session.trackEntity(state.core);
        }
        state.phase = Phase.OPEN;
        state.phaseTick = 0;
        state.fired = false;
        riftCount.incrementAndGet();
        RiftParticles.spawnOpenBurst(visuals, at);
        visuals.sound("BLOCK_PORTAL_AMBIENT", at, 0.55f, 1.4f);
        visuals.sound("ENTITY_ILLUSIONER_PREPARE_MIRROR", at, 0.85f, 0.9f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", at, 0.55f, 1.45f);
        RiftDisplays.placeBlock(state.frame, at, 0.1f, 0.2f, 0.05f, 0.0f);
        RiftDisplays.placeBlock(state.core, at, 0.15f, 0.2f, 0.15f, 0.0f);
    }

    private static Location randomRiftLocation(Location ground, double radius, int index) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble() * Math.PI * 2.0 + index * 1.1;
        double dist = 1.4 + rng.nextDouble() * Math.max(0.5, radius - 1.4);
        double y = 1.1 + rng.nextDouble() * 2.2;
        return ground.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
    }

    private static void clearRift(RiftState state) {
        RiftDisplays.removeBlock(state.frame);
        RiftDisplays.removeBlock(state.core);
        state.frame = null;
        state.core = null;
    }

    private static void fireBolt(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location ground,
            Location from,
            double seekRange,
            double boltSpeed,
            List<Bolt> bolts) {
        Location seekFrom = killer != null && killer.isOnline() && killer.getWorld().equals(world)
                ? killer.getLocation()
                : ground;
        Player target = findNearestTo(world, seekFrom, killer, victimId, seekRange, session);
        Vector dir;
        if (target != null) {
            dir = target.getEyeLocation().toVector().subtract(from.toVector());
        } else {
            dir = new Vector(ThreadLocalRandom.current().nextGaussian(), 0.1, ThreadLocalRandom.current().nextGaussian());
        }
        if (dir.lengthSquared() < 1.0e-4) {
            dir = new Vector(1, 0, 0);
        }
        dir.normalize();

        ItemDisplay display = RiftDisplays.spawnBolt(from);
        if (display != null) {
            session.trackEntity(display);
        }
        bolts.add(new Bolt(display, from.clone(), dir.multiply(boltSpeed)));
        visuals.particle("PORTAL", from, ParticleScale.scale(12), 0.2, 0.25, 0.2, 0.08, null);
    }

    private static void updateBolts(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            List<Bolt> bolts,
            double boltDamage,
            EffectDamageConfig damageCfg) {
        List<Bolt> alive = new ArrayList<>(bolts.size());
        for (Bolt bolt : bolts) {
            if (bolt.dead) {
                RiftDisplays.removeItem(bolt.display);
                continue;
            }
            bolt.age++;
            bolt.loc.add(bolt.velocity);
            RiftDisplays.placeBolt(bolt.display, bolt.loc, 0.4f + (float) Math.sin(bolt.age * 0.4) * 0.08f, bolt.age * 0.45f);
            RiftParticles.spawnBoltTrail(visuals, bolt.loc, bolt.velocity, bolt.age);

            boolean hit = tryHit(session, visuals, world, killer, victimId, bolt, boltDamage, damageCfg);
            boolean expired = bolt.age >= BOLT_MAX_LIFE;
            boolean blocked = bolt.loc.getBlock().getType().isSolid();
            if (hit || expired || blocked) {
                if (!hit && (expired || blocked)) {
                    RiftParticles.spawnBoltHit(visuals, bolt.loc);
                    visuals.sound("ENTITY_SHULKER_BULLET_HIT", bolt.loc, 0.55f, 0.9f);
                }
                RiftDisplays.removeItem(bolt.display);
                bolt.dead = true;
                continue;
            }
            alive.add(bolt);
        }
        bolts.clear();
        bolts.addAll(alive);
    }

    private static boolean tryHit(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Bolt bolt,
            double boltDamage,
            EffectDamageConfig damageCfg) {
        if (killer == null || boltDamage <= 0.0) {
            return false;
        }
        double hitSq = BOLT_HIT_RADIUS * BOLT_HIT_RADIUS;
        Player struck = null;
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
            if (bolt.hitIds.contains(player.getUniqueId())) {
                continue;
            }
            if (player.getLocation().clone().add(0, 1, 0).distanceSquared(bolt.loc) > hitSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            struck = player;
            break;
        }
        if (struck == null) {
            return false;
        }
        bolt.hitIds.add(struck.getUniqueId());
        struck.damage(boltDamage, killer);
        RiftParticles.spawnBoltHit(visuals, bolt.loc);
        visuals.sound("ENTITY_SHULKER_BULLET_HIT", bolt.loc, 0.85f, 0.8f);
        visuals.sound("ENTITY_ENDERMAN_TELEPORT", bolt.loc, 0.55f, 1.35f);
        if (damageCfg.enabled() && damageCfg.radius() > 0.0) {
            double splashSq = damageCfg.radius() * damageCfg.radius();
            for (Player player : world.getPlayers()) {
                if (player.getUniqueId().equals(killer.getUniqueId()) || player.getUniqueId().equals(struck.getUniqueId())) {
                    continue;
                }
                if (victimId != null && player.getUniqueId().equals(victimId)) {
                    continue;
                }
                if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                    continue;
                }
                if (player.getLocation().distanceSquared(bolt.loc) > splashSq) {
                    continue;
                }
                if (!session.allowsWorldMutation(killer, player.getLocation())) {
                    continue;
                }
                player.damage(Math.max(1.0, boltDamage * 0.45), killer);
            }
        }
        return true;
    }

    private static Player findNearestTo(
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

    private static final class RiftState {
        private Location at;
        private @org.jspecify.annotations.Nullable BlockDisplay frame;
        private @org.jspecify.annotations.Nullable BlockDisplay core;
        private Phase phase = Phase.OPEN;
        private int phaseTick;
        private boolean fired;
    }

    private static final class Bolt {
        private final @org.jspecify.annotations.Nullable ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final Set<UUID> hitIds = new HashSet<>();
        private int age;
        private boolean dead;

        private Bolt(
                @org.jspecify.annotations.Nullable ItemDisplay display, Location loc, Vector velocity) {
            this.display = display;
            this.loc = loc;
            this.velocity = velocity;
        }
    }
}
