package com.monkey.ktplus.effects.list.beeswarm.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.beeswarm.animation.util.BeeSwarmDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class BeeSwarmAnimation {
    private static final int DEFAULT_BEE_COUNT = 7;
    private static final int DEFAULT_STING_INTERVAL = 7;
    private static final int DEFAULT_POISON_SECONDS = 2;
    private static final double DEFAULT_AGGRO_RADIUS = 16.0;
    private static final int DEFAULT_MAX_STINGS = 3;
    private static final double DEFAULT_SPEED = 0.42;
    private static final int DEFAULT_DURATION = 180;
    private static final int INTRO_ORBIT_TICKS = 18;
    private static final int BEE_MAX_LIFE_TICKS = 140;

    private static final Color HONEY = Color.fromRGB(240, 180, 40);
    private static final Color AMBER = Color.fromRGB(210, 130, 20);
    private static final Color GOLD = Color.fromRGB(255, 220, 90);
    private static final Color WARM = Color.fromRGB(180, 100, 30);

    private enum BeePhase {
        INTRO,
        SEEK,
        DONE
    }

    private BeeSwarmAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.6, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("beeswarm");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int beeCount = perks == null
                ? DEFAULT_BEE_COUNT
                : Math.max(3, Math.min(16, perks.getInt("bee-count", DEFAULT_BEE_COUNT)));
        int stingInterval = perks == null
                ? DEFAULT_STING_INTERVAL
                : Math.max(3, perks.getInt("sting-interval", DEFAULT_STING_INTERVAL));
        int poisonSeconds = perks == null
                ? DEFAULT_POISON_SECONDS
                : Math.max(0, perks.getInt("poison-seconds", DEFAULT_POISON_SECONDS));
        double aggroRadius = perks == null
                ? DEFAULT_AGGRO_RADIUS
                : Math.max(4.0, perks.getDouble("aggro-radius", DEFAULT_AGGRO_RADIUS));
        int configuredMaxStings = perks == null
                ? DEFAULT_MAX_STINGS
                : Math.max(1, perks.getInt("max-stings", DEFAULT_MAX_STINGS));
        double speed = perks == null
                ? DEFAULT_SPEED
                : Math.max(0.15, perks.getDouble("speed", DEFAULT_SPEED));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(80, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("beeswarm");
        double stingDamage = damageCfg.enabled()
                ? Math.max(0.5, damageCfg.value())
                : (perks == null ? 1.0 : Math.max(0.5, perks.getDouble("sting-damage", 1.0)));
        PotionEffectType poison = PotionTypes.resolve("POISON", "POISON");

        List<SwarmBee> bees = new ArrayList<>(beeCount);
        for (int i = 0; i < beeCount; i++) {
            
            double angle = (Math.PI * 2.0 * i) / beeCount
                    + (Math.random() - 0.5) * 0.55
                    + i * 0.17;
            double radius = 1.8 + (i % 5) * 0.55 + Math.random() * 0.7;
            double spawnY = 0.35 + (i % 4) * 0.45 + Math.random() * 0.35;
            Location spawn = origin.clone().add(
                    Math.cos(angle) * radius,
                    spawnY,
                    Math.sin(angle) * radius);
            Bee entity = BeeSwarmDisplays.spawnBee(spawn);
            if (entity == null) {
                continue;
            }
            session.trackEntity(entity);
            double phaseOffset = Math.random() * Math.PI * 2.0 + i * 0.73;
            double orbitDir = i % 2 == 0 ? 1.0 : -1.0;
            int introTicks = INTRO_ORBIT_TICKS + i * 4 + (int) (Math.random() * 6);
            int aggroDelay = 4 + i * 5 + (int) (Math.random() * 8);
            bees.add(new SwarmBee(
                    entity,
                    spawn,
                    i,
                    angle,
                    radius,
                    0.75 + (i % 5) * 0.14 + Math.random() * 0.12,
                    phaseOffset,
                    introTicks,
                    orbitDir,
                    aggroDelay));
        }

        int maxStings = Math.max(configuredMaxStings, bees.size());

        Map<UUID, Integer> stingCounts = new HashMap<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            for (SwarmBee bee : bees) {
                BeeSwarmDisplays.remove(bee.entity);
            }
            bees.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_BEE_LOOP", origin, 0.7f, 1.15f);
        visuals.sound("ENTITY_BEE_POLLINATE", origin, 0.8f, 1.2f);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || bees.isEmpty()) {
                for (SwarmBee bee : bees) {
                    puffOut(visuals, bee);
                }
                bees.clear();
                visuals.sound("ENTITY_BEE_DEATH", origin, 0.6f, 1.1f);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            int activeStings = 0;
            Iterator<SwarmBee> it = bees.iterator();
            while (it.hasNext()) {
                SwarmBee bee = it.next();
                if (!bee.entity.isValid() || bee.entity.isDead() || bee.phase == BeePhase.DONE) {
                    puffOut(visuals, bee);
                    it.remove();
                    continue;
                }
                bee.age++;
                if (bee.age >= BEE_MAX_LIFE_TICKS && bee.phase == BeePhase.SEEK) {
                    bee.phase = BeePhase.DONE;
                }

                if (updateBee(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        origin,
                        bee,
                        aggroRadius,
                        speed,
                        stingInterval,
                        stingDamage,
                        poisonSeconds,
                        poison,
                        maxStings,
                        stingCounts,
                        current)) {
                    activeStings++;
                }

                if (bee.phase == BeePhase.DONE) {
                    puffOut(visuals, bee);
                    it.remove();
                }
            }

            if (current % 7 == 0) {
                visuals.sound("ENTITY_BEE_LOOP", origin, 0.28f, 1.0f + (float) Math.random() * 0.35f);
            }
            if (current % 11 == 0) {
                visuals.sound("ENTITY_BEE_POLLINATE", origin, 0.22f, 1.3f);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&6🐝 BEESWARM &8| &e%d &8| &6STING &f%d",
                                bees.size(),
                                activeStings));
            }
            return true;
        });
    }

    private static boolean updateBee(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location hive,
            SwarmBee bee,
            double aggroRadius,
            double speed,
            int stingInterval,
            double stingDamage,
            int poisonSeconds,
            PotionEffectType poison,
            int maxStingsPerPlayer,
            Map<UUID, Integer> stingCounts,
            int tick) {
        boolean stung = false;

        switch (bee.phase) {
            case INTRO -> {
                double spin = bee.orbitAngle
                        + tick * (0.12 + bee.index * 0.008) * bee.orbitDir
                        + bee.phaseOffset * 0.05;
                double y = Math.sin(tick * 0.18 + bee.phaseOffset) * 0.45 + 0.4 + bee.index * 0.05;
                double r = bee.orbitRadius * (0.85 + 0.15 * Math.sin(tick * 0.1 + bee.phaseOffset));
                bee.loc = hive.clone().add(
                        Math.cos(spin) * r,
                        y,
                        Math.sin(spin) * r);
                bee.yaw = (float) Math.toDegrees(spin + Math.PI * 0.5 * bee.orbitDir);
                bee.phaseTicks--;
                if (bee.phaseTicks <= 0) {
                    bee.phase = BeePhase.SEEK;
                    
                    bee.stingCooldown = bee.aggroDelay + bee.index % Math.max(1, stingInterval);
                }
            }
            case SEEK -> {
                if (tick < bee.aggroDelay) {
                    
                    bee.loc.add(
                            Math.cos(tick * 0.13 + bee.phaseOffset) * (0.08 + bee.index * 0.01),
                            Math.sin(tick * 0.19 + bee.phaseOffset) * 0.06,
                            Math.sin(tick * 0.11 + bee.phaseOffset) * (0.08 + bee.index * 0.01));
                    break;
                }
                Player target = resolveTarget(world, bee, killer, victimId, hive, aggroRadius, session, stingCounts, maxStingsPerPlayer);
                if (target != null) {
                    
                    double seekSpin = bee.phaseOffset + tick * (0.06 + bee.index * 0.004) * bee.orbitDir;
                    Location aim = target.getLocation().clone().add(
                            Math.cos(seekSpin) * (0.45 + (bee.index % 3) * 0.2),
                            0.85 + Math.sin(tick * 0.14 + bee.phaseOffset) * 0.35 + (bee.index % 4) * 0.08,
                            Math.sin(seekSpin) * (0.45 + (bee.index % 3) * 0.2));
                    Vector to = aim.toVector().subtract(bee.loc.toVector());
                    double dist = to.length();
                    if (dist > 0.001) {
                        double beeSpeed = speed * bee.speedMul;
                        Vector step = to.normalize().multiply(Math.min(beeSpeed, Math.max(0.12, dist * 0.35)));
                        Vector side = step.clone().crossProduct(new Vector(0, 1, 0));
                        if (side.lengthSquared() > 1.0e-6) {
                            side.normalize().multiply(Math.sin(tick * 0.28 + bee.phaseOffset) * (0.09 + bee.index * 0.01));
                            step.add(side);
                        }
                        step.setY(step.getY() + Math.sin(tick * 0.35 + bee.phaseOffset) * 0.05);
                        bee.loc.add(step);
                        bee.yaw = (float) Math.toDegrees(Math.atan2(-step.getX(), step.getZ()));
                    }
                    int used = stingCounts.getOrDefault(target.getUniqueId(), 0);
                    if (dist < 1.1
                            && bee.stingCooldown <= 0
                            && !bee.hasStung
                            && used < maxStingsPerPlayer
                            && killer != null
                            && stingDamage > 0.0
                            && session.allowsWorldMutation(killer, target.getLocation())) {
                        bee.hasStung = true;
                        stingCounts.put(target.getUniqueId(), used + 1);
                        target.damage(stingDamage, killer);
                        if (poison != null && poisonSeconds > 0) {
                            target.addPotionEffect(
                                    new PotionEffect(poison, poisonSeconds * 20, 0, true, true, true));
                        }
                        visuals.sound("ENTITY_BEE_STING", bee.loc, 0.9f, 1.15f + bee.index * 0.01f);
                        visuals.dust(bee.loc, HONEY, 1.3f, 5, 0.15, 0.15, 0.15, 0.0);
                        visuals.particle("CRIT", bee.loc, 4, 0.15, 0.15, 0.15, 0.02, null);
                        stung = true;
                        bee.phase = BeePhase.DONE;
                    } else if (used >= maxStingsPerPlayer) {
                        bee.targetId = null;
                    }
                } else {
                    double roam = 0.12 + bee.index * 0.008;
                    bee.loc.add(
                            Math.cos(tick * (0.09 + bee.index * 0.01) + bee.phaseOffset) * roam,
                            Math.sin(tick * 0.16 + bee.phaseOffset) * 0.06,
                            Math.sin(tick * (0.11 + bee.index * 0.008) + bee.phaseOffset) * roam);
                }
            }
            case DONE -> {
                
            }
        }

        if (bee.stingCooldown > 0) {
            bee.stingCooldown--;
        }

        BeeSwarmDisplays.place(bee.entity, bee.loc, bee.yaw);
        drawBuzzTrail(visuals, bee, tick);
        return stung;
    }

    private static void drawBuzzTrail(VisualEffectService visuals, SwarmBee bee, int tick) {
        if (tick % 2 == bee.index % 2) {
            visuals.dust(bee.loc, GOLD, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % 3 == bee.index % 3) {
            visuals.dust(bee.loc.clone().add(0, 0.15, 0), HONEY, 0.7f, 1, 0.02, 0.02, 0.02, 0.0);
            visuals.dust(bee.loc.clone().add(0.05, 0.05, -0.05), WARM, 0.55f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void puffOut(VisualEffectService visuals, SwarmBee bee) {
        Location at = bee.loc != null ? bee.loc.clone() : bee.entity.getLocation();
        visuals.dust(at, HONEY, 1.4f, ParticleScale.scale(8), 0.25, 0.25, 0.25, 0.0);
        visuals.dust(at, AMBER, 1.1f, ParticleScale.scale(5), 0.3, 0.2, 0.3, 0.0);
        visuals.particle("CLOUD", at, 3, 0.12, 0.12, 0.12, 0.01, null);
        visuals.sound("ENTITY_BEE_DEATH", at, 0.35f, 1.25f);
        BeeSwarmDisplays.remove(bee.entity);
    }

    private static Player resolveTarget(
            World world,
            SwarmBee bee,
            Player killer,
            UUID victimId,
            Location hive,
            double radius,
            EffectSession session,
            Map<UUID, Integer> stingCounts,
            int maxStingsPerPlayer) {
        if (bee.targetId != null) {
            Player locked = world.getPlayers().stream()
                    .filter(p -> p.getUniqueId().equals(bee.targetId))
                    .findFirst()
                    .orElse(null);
            if (locked != null
                    && locked.isOnline()
                    && !locked.isDead()
                    && locked.getWorld().equals(world)
                    && locked.getLocation().distanceSquared(hive) <= radius * radius
                    && stingCounts.getOrDefault(locked.getUniqueId(), 0) < maxStingsPerPlayer) {
                return locked;
            }
            bee.targetId = null;
        }
        List<Player> nearby = findNearby(world, hive, killer, victimId, radius, session);
        nearby.removeIf(p -> stingCounts.getOrDefault(p.getUniqueId(), 0) >= maxStingsPerPlayer);
        if (nearby.isEmpty()) {
            return null;
        }
        
        Player next = nearby.get(bee.index % nearby.size());
        bee.targetId = next.getUniqueId();
        return next;
    }

    private static List<Player> findNearby(
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
        return found;
    }

    private static final class SwarmBee {
        private final Bee entity;
        private Location loc;
        private final int index;
        private double orbitAngle;
        private double orbitRadius;
        private final double speedMul;
        private final double phaseOffset;
        private final double orbitDir;
        private final int aggroDelay;
        private UUID targetId;
        private float yaw;
        private int stingCooldown;
        private boolean hasStung;
        private int age;
        private BeePhase phase = BeePhase.INTRO;
        private int phaseTicks;

        private SwarmBee(
                Bee entity,
                Location loc,
                int index,
                double orbitAngle,
                double orbitRadius,
                double speedMul,
                double phaseOffset,
                int introTicks,
                double orbitDir,
                int aggroDelay) {
            this.entity = entity;
            this.loc = loc.clone();
            this.index = index;
            this.orbitAngle = orbitAngle;
            this.orbitRadius = orbitRadius;
            this.speedMul = speedMul;
            this.phaseOffset = phaseOffset;
            this.orbitDir = orbitDir;
            this.aggroDelay = aggroDelay;
            this.phaseTicks = introTicks;
            this.stingCooldown = aggroDelay;
        }
    }
}
