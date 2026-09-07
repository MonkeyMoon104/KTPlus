package com.monkey.ktplus.effects.list.mirrorclone.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.mirrorclone.animation.util.MirrorCloneDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class MirrorCloneAnimation {
    private static final int DEFAULT_CLONE_DURATION = 70;
    private static final double DEFAULT_EXPLODE_RADIUS = 3.5;
    private static final double DEFAULT_DAMAGE = 5.0;
    private static final double DEFAULT_AGGRO_RANGE = 18.0;
    private static final double DEFAULT_SPEED = 0.38;
    private static final int SHARD_COUNT = 14;
    private static final int SHARD_LIFE = 28;
    private static final int MAX_TICKS = 160;

    private static final Color MIRROR = Color.fromRGB(210, 230, 245);
    private static final Color GLASS = Color.fromRGB(160, 200, 220);
    private static final Color FLASH = Color.fromRGB(255, 255, 255);

    private MirrorCloneAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("mirrorclone");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int cloneDuration = perks == null
                ? DEFAULT_CLONE_DURATION
                : Math.max(20, perks.getInt("clone-duration", DEFAULT_CLONE_DURATION));
        double explodeRadius = perks == null
                ? DEFAULT_EXPLODE_RADIUS
                : Math.max(1.0, perks.getDouble("explode-radius", DEFAULT_EXPLODE_RADIUS));
        double perkDamage = perks == null
                ? DEFAULT_DAMAGE
                : Math.max(0.0, perks.getDouble("damage", DEFAULT_DAMAGE));
        double aggroRange = perks == null
                ? DEFAULT_AGGRO_RANGE
                : Math.max(4.0, perks.getDouble("aggro-range", DEFAULT_AGGRO_RANGE));
        double speed = perks == null ? DEFAULT_SPEED : Math.max(0.12, perks.getDouble("speed", DEFAULT_SPEED));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("mirrorclone");
        double damage = damageCfg.enabled() ? Math.max(perkDamage, damageCfg.value()) : perkDamage;

        if (killer != null && !session.allowsWorldMutation(killer, origin)) {
            return;
        }

        LivingEntity runner = MirrorCloneDisplays.spawnRunner(origin, killer, speed);
        if (runner == null) {
            return;
        }
        session.trackEntity(runner);

        List<ItemDisplay> shards = new ArrayList<>();
        List<Shard> flying = new ArrayList<>();
        boolean[] exploded = {false};
        LivingEntity[] cloneRef = {runner};

        session.onCleanup(() -> {
            MirrorCloneDisplays.removeRunner(cloneRef[0]);
            MirrorCloneDisplays.removeAll(shards);
        });

        visuals.sound("ENTITY_ENDERMAN_TELEPORT", origin, 0.85f, 1.45f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", origin, 0.7f, 1.2f);
        visuals.dust(origin.clone().add(0, 1, 0), MIRROR, 1.4f, 16, 0.35, 0.6, 0.35, 0.0);
        session.resetDeadline(MAX_TICKS + 20L);

        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            LivingEntity clone = cloneRef[0];

            if (current >= MAX_TICKS && flying.isEmpty()) {
                MirrorCloneDisplays.removeRunner(clone);
                cloneRef[0] = null;
                MirrorCloneDisplays.removeAll(shards);
                return false;
            }

            if (!exploded[0]) {
                if (clone == null || !clone.isValid() || clone.isDead()) {
                    Location failAt = clone != null ? clone.getLocation() : origin;
                    explode(
                            session,
                            visuals,
                            world,
                            killer,
                            victimId,
                            failAt,
                            cloneRef,
                            shards,
                            flying,
                            explodeRadius,
                            damage);
                    exploded[0] = true;
                } else {
                    Player target = findNearest(world, clone.getLocation(), killer, victimId, aggroRange, session);
                    if (clone instanceof Mob mob) {
                        mob.setTarget(target);
                    }

                    double dist = target == null
                            ? Double.MAX_VALUE
                            : target.getLocation().distance(clone.getLocation());
                    if (dist <= 1.35 || current >= cloneDuration) {
                        explode(
                                session,
                                visuals,
                                world,
                                killer,
                                victimId,
                                clone.getLocation(),
                                cloneRef,
                                shards,
                                flying,
                                explodeRadius,
                                damage);
                        exploded[0] = true;
                    } else if (current % 2 == 0) {
                        Location bob = clone.getLocation().clone().add(0.0, 1.0, 0.0);
                        visuals.dust(
                                bob,
                                current % 4 == 0 ? FLASH : GLASS,
                                0.9f,
                                2,
                                0.15,
                                0.35,
                                0.15,
                                0.0);
                        if (current % 8 == 0) {
                            visuals.sound("BLOCK_GLASS_HIT", bob, 0.25f, 1.6f);
                        }
                    }
                }
            }

            flying.removeIf(shard -> updateShard(visuals, shard));
            return !(exploded[0] && flying.isEmpty() && current > cloneDuration + SHARD_LIFE);
        });
    }

    private static void explode(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location at,
            LivingEntity[] cloneRef,
            List<ItemDisplay> shards,
            List<Shard> flying,
            double radius,
            double damage) {
        MirrorCloneDisplays.removeRunner(cloneRef[0]);
        cloneRef[0] = null;

        Location center = at.clone().add(0.0, 1.0, 0.0);
        visuals.particle("FIREWORK", center, 35, 0.5, 0.6, 0.5, 0.08, null);
        visuals.dust(center, MIRROR, 1.6f, 24, 0.55, 0.55, 0.55, 0.0);
        visuals.dust(center, FLASH, 1.2f, 12, 0.3, 0.3, 0.3, 0.0);
        visuals.sound("BLOCK_GLASS_BREAK", center, 1.3f, 0.85f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 0.55f, 1.55f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", center, 1.0f, 1.4f);

        if (killer != null && damage > 0.0) {
            double radiusSq = radius * radius;
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
                if (player.getLocation().distanceSquared(center) > radiusSq) {
                    continue;
                }
                if (!session.allowsWorldMutation(killer, player.getLocation())) {
                    continue;
                }
                player.damage(damage, killer);
                Vector knock = player.getLocation().toVector().subtract(center.toVector());
                if (knock.lengthSquared() > 1.0e-4) {
                    knock.normalize().multiply(0.55).setY(0.28);
                    player.setVelocity(player.getVelocity().add(knock));
                }
            }
        }

        for (int i = 0; i < SHARD_COUNT; i++) {
            double angle = (Math.PI * 2.0 * i) / SHARD_COUNT + Math.random() * 0.2;
            double elev = 0.15 + Math.random() * 0.55;
            Vector vel = new Vector(
                    Math.cos(angle) * (0.35 + Math.random() * 0.35),
                    elev,
                    Math.sin(angle) * (0.35 + Math.random() * 0.35));
            ItemDisplay display = MirrorCloneDisplays.spawnShard(center);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            shards.add(display);
            flying.add(new Shard(display, center.clone(), vel, i));
        }
    }

    private static boolean updateShard(VisualEffectService visuals, Shard shard) {
        if (shard.age++ >= SHARD_LIFE) {
            MirrorCloneDisplays.remove(shard.display);
            return true;
        }
        shard.velocity.setY(shard.velocity.getY() - 0.035);
        shard.loc.add(shard.velocity);
        float spin = shard.age * 0.4f + shard.index;
        MirrorCloneDisplays.place(
                shard.display, shard.loc, spin, spin * 0.6f, spin * 0.3f, 0.22f + (shard.index % 3) * 0.04f);
        if (shard.age % 2 == 0) {
            visuals.dust(shard.loc, shard.index % 2 == 0 ? MIRROR : GLASS, 0.7f, 1, 0, 0, 0, 0);
        }
        return false;
    }

    private static Player findNearest(
            World world,
            Location from,
            Player killer,
            UUID victimId,
            double range,
            EffectSession session) {
        double rangeSq = range * range;
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
            if (player.getLocation().distanceSquared(from) > rangeSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            found.add(player);
        }
        found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(from)));
        return found.isEmpty() ? null : found.get(0);
    }

    private static final class Shard {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final int index;
        private int age;

        private Shard(ItemDisplay display, Location loc, Vector velocity, int index) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.index = index;
        }
    }
}
