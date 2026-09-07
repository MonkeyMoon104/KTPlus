package com.monkey.ktplus.effects.list.sparkler.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.sparkler.animation.util.SparklerDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.MaterialCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SparklerAnimation {
    private static final int DEFAULT_SPARK_COUNT = 12;
    private static final int DEFAULT_BOUNCE_TICKS = 80;
    private static final int DEFAULT_BURN_SECONDS = 2;
    private static final double DEFAULT_RADIUS = 4.0;
    private static final int DEFAULT_DURATION = 140;
    private static final double GRAVITY = 0.045;
    private static final double BOUNCE_DAMP = 0.62;

    private static final Color GOLD = Color.fromRGB(255, 190, 40);
    private static final Color ORANGE = Color.fromRGB(255, 120, 20);
    private static final Color EMBER = Color.fromRGB(220, 60, 15);
    private static final Color WHITE_HOT = Color.fromRGB(255, 240, 180);

    private SparklerAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.35, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("sparkler");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int sparkCount = perks == null
                ? DEFAULT_SPARK_COUNT
                : Math.max(4, Math.min(28, perks.getInt("spark-count", DEFAULT_SPARK_COUNT)));
        int bounceTicks = perks == null
                ? DEFAULT_BOUNCE_TICKS
                : Math.max(30, perks.getInt("bounce-ticks", DEFAULT_BOUNCE_TICKS));
        int burnSeconds = perks == null
                ? DEFAULT_BURN_SECONDS
                : Math.max(0, perks.getInt("burn-seconds", DEFAULT_BURN_SECONDS));
        double radius = perks == null
                ? DEFAULT_RADIUS
                : Math.max(1.5, perks.getDouble("radius", DEFAULT_RADIUS));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(60, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("sparkler");
        double hitDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 0.0;
        double damageRadius = damageCfg.enabled() && damageCfg.radius() > 0.0 ? damageCfg.radius() : 3.0;
        int fireRestoreTicks = Math.max(20, burnSeconds * 20);

        double groundY = origin.getY();
        List<Spark> sparks = new ArrayList<>(sparkCount);
        List<ItemDisplay> displays = new ArrayList<>(sparkCount);
        Material[] mats = {
            Material.BLAZE_POWDER, Material.MAGMA_CREAM, Material.FIRE_CHARGE, Material.GLOWSTONE_DUST
        };

        for (int i = 0; i < sparkCount; i++) {
            double angle = (Math.PI * 2.0 * i) / sparkCount + Math.random() * 0.25;
            double speed = 0.18 + Math.random() * 0.22;
            Vector vel = new Vector(
                    Math.cos(angle) * speed,
                    0.28 + Math.random() * 0.22,
                    Math.sin(angle) * speed);
            Location spawn = origin.clone().add(
                    Math.cos(angle) * 0.35,
                    0.2 + (i % 3) * 0.08,
                    Math.sin(angle) * 0.35);
            float scale = 0.28f + (i % 4) * 0.05f;
            ItemDisplay display = SparklerDisplays.spawn(spawn, mats[i % mats.length], scale);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            sparks.add(new Spark(display, spawn, vel, scale, groundY, i));
        }

        if (sparks.isEmpty()) {
            return;
        }

        Set<UUID> burned = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            SparklerDisplays.removeAll(displays);
            displays.clear();
            sparks.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("ITEM_FIRECHARGE_USE", origin, 0.9f, 1.35f);
        visuals.sound("ENTITY_BLAZE_SHOOT", origin, 0.55f, 1.6f);
        visuals.dust(origin, GOLD, 1.5f, ParticleScale.scale(14), 0.35, 0.25, 0.35, 0.0);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || sparks.isEmpty()) {
                dissipate(visuals, origin, sparks, displays);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            int alive = 0;
            for (Spark spark : sparks) {
                if (spark.done) {
                    continue;
                }
                alive++;
                updateSpark(session, visuals, spark, current, bounceTicks, killer, fireRestoreTicks);
                if (spark.done) {
                    SparklerDisplays.remove(spark.display);
                    continue;
                }
                burnNearby(
                        session,
                        visuals,
                        world,
                        spark.loc,
                        killer,
                        victimId,
                        radius,
                        burnSeconds,
                        hitDamage,
                        damageRadius,
                        burned,
                        current);
            }

            if (current % 6 == 0) {
                visuals.sound("BLOCK_FIRE_AMBIENT", origin, 0.28f, 1.4f + (float) (Math.random() * 0.3));
            }
            if (current % 10 == 0) {
                visuals.sound("BLOCK_NOTE_BLOCK_CHIME", origin, 0.2f, 1.7f + (float) (Math.random() * 0.25));
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&6✨ SPARKLER &8| &e%d &8| &6%d&8/&6%d",
                                alive,
                                Math.max(0, durationTicks - current),
                                durationTicks));
            }
            return true;
        });
    }

    private static void updateSpark(
            EffectSession session,
            VisualEffectService visuals,
            Spark spark,
            int tick,
            int bounceTicks,
            Player killer,
            int fireRestoreTicks) {
        spark.age++;
        if (spark.age >= bounceTicks) {
            spark.done = true;
            visuals.dust(spark.loc, ORANGE, 1.2f, ParticleScale.scale(6), 0.15, 0.15, 0.15, 0.0);
            placeControlledFlame(session, killer, spark.loc, fireRestoreTicks);
            spark.flamePlaced = true;
            return;
        }

        spark.vel.setY(spark.vel.getY() - GRAVITY);
        spark.loc.add(spark.vel);

        if (spark.loc.getY() <= spark.groundY) {
            spark.loc.setY(spark.groundY);
            if (Math.abs(spark.vel.getY()) > 0.06) {
                spark.vel.setY(-spark.vel.getY() * BOUNCE_DAMP);
                spark.vel.multiply(0.88);
                spark.bounces++;
                visuals.sound("BLOCK_FIRE_EXTINGUISH", spark.loc, 0.25f, 1.8f);
                visuals.dust(spark.loc, GOLD, 1.1f, ParticleScale.scale(5), 0.2, 0.05, 0.2, 0.0);
                visuals.particle("LAVA", spark.loc, 1, 0.05, 0.02, 0.05, 0.0, null);
                visuals.particle("FLAME", spark.loc, 3, 0.08, 0.05, 0.08, 0.01, null);
                placeControlledFlame(session, killer, spark.loc, fireRestoreTicks);
                spark.flamePlaced = true;
            } else {
                spark.vel.setY(0.0);
                spark.vel.multiply(0.92);
                if (spark.age % 4 == 0) {
                    placeControlledFlame(session, killer, spark.loc, fireRestoreTicks);
                    spark.flamePlaced = true;
                }
            }
        }

        float spin = tick * 0.35f + spark.index * 0.4f;
        float pulse = spark.scale * (0.9f + (float) Math.sin(tick * 0.4 + spark.index) * 0.15f);
        SparklerDisplays.place(spark.display, spark.loc, pulse, spin, spin * 0.6f, spin * 0.4f);

        if (tick % 2 == spark.index % 2) {
            visuals.dust(spark.loc, GOLD, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
            visuals.dust(spark.loc.clone().add(0, 0.08, 0), ORANGE, 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % 3 == spark.index % 3) {
            visuals.dust(spark.loc, WHITE_HOT, 0.55f, 1, 0.02, 0.02, 0.02, 0.0);
            visuals.dust(spark.loc.clone().add(spark.vel.clone().multiply(-0.4)), EMBER, 0.6f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void burnNearby(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location at,
            Player killer,
            UUID victimId,
            double radius,
            int burnSeconds,
            double hitDamage,
            double damageRadius,
            Set<UUID> burned,
            int tick) {
        double radiusSq = radius * radius;
        for (Player player : findNearby(world, at, killer, victimId, radius, session)) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (player.getLocation().distanceSquared(at) > radiusSq) {
                continue;
            }
            Location feet = player.getLocation();
            if (killer != null && !session.allowsWorldMutation(killer, feet)) {
                continue;
            }
            if (burnSeconds > 0) {
                player.setFireTicks(Math.max(player.getFireTicks(), burnSeconds * 20));
            }
            if (hitDamage > 0.0
                    && feet.distanceSquared(at) <= damageRadius * damageRadius
                    && !burned.contains(player.getUniqueId())
                    && killer != null
                    && !player.getUniqueId().equals(killer.getUniqueId())) {
                burned.add(player.getUniqueId());
                final UUID id = player.getUniqueId();
                session.runLater(14L, () -> burned.remove(id));
                player.damage(hitDamage, killer);
                visuals.sound("ENTITY_PLAYER_HURT_ON_FIRE", feet, 0.7f, 1.2f);
                visuals.dust(feet.clone().add(0, 0.3, 0), ORANGE, 1.3f, 6, 0.2, 0.15, 0.2, 0.0);
            }
            if (tick % 4 == 0) {
                visuals.dust(feet.clone().add(0, 0.15, 0), EMBER, 0.9f, 2, 0.15, 0.05, 0.15, 0.0);
            }
        }
    }

    private static List<Player> findNearby(
            World world, Location origin, Player killer, UUID victimId, double radius, EffectSession session) {
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

    private static void placeControlledFlame(
            EffectSession session, Player killer, Location feet, int restoreTicks) {
        if (feet.getWorld() == null || restoreTicks <= 0) {
            return;
        }
        Block ground = feet.getBlock();
        Block flame = ground;
        if (!MaterialCompat.isAir(flame.getType())
                && flame.getType() != Material.FIRE
                && flame.getType() != Material.SOUL_FIRE) {
            flame = ground.getRelative(BlockFace.UP);
        }
        if (flame.getType() == Material.FIRE || flame.getType() == Material.SOUL_FIRE) {
            return;
        }
        if (!MaterialCompat.isAir(flame.getType())) {
            return;
        }
        if (killer != null && !session.allowsWorldMutation(killer, flame.getLocation())) {
            return;
        }
        session.temporaryBlock(killer, flame, Material.FIRE, Math.max(20L, restoreTicks), false);
    }

    private static void dissipate(
            VisualEffectService visuals, Location origin, List<Spark> sparks, List<ItemDisplay> displays) {
        visuals.sound("BLOCK_FIRE_EXTINGUISH", origin, 0.85f, 0.9f);
        visuals.dust(origin, GOLD, 1.6f, ParticleScale.scale(16), 0.55, 0.35, 0.55, 0.0);
        visuals.dust(origin, ORANGE, 1.3f, ParticleScale.scale(10), 0.7, 0.4, 0.7, 0.0);
        visuals.particle("CLOUD", origin, ParticleScale.scale(6), 0.35, 0.2, 0.35, 0.02, null);
        for (Spark spark : sparks) {
            SparklerDisplays.remove(spark.display);
        }
        sparks.clear();
        displays.clear();
    }

    private static final class Spark {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector vel;
        private final float scale;
        private final double groundY;
        private final int index;
        private int age;
        private int bounces;
        private boolean done;
        private boolean flamePlaced;

        private Spark(ItemDisplay display, Location loc, Vector vel, float scale, double groundY, int index) {
            this.display = display;
            this.loc = loc.clone();
            this.vel = vel.clone();
            this.scale = scale;
            this.groundY = groundY;
            this.index = index;
        }
    }
}
