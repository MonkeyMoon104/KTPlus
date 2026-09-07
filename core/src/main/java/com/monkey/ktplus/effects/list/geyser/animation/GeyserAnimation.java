package com.monkey.ktplus.effects.list.geyser.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.geyser.animation.util.GeyserDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class GeyserAnimation {
    private static final double DEFAULT_HEIGHT = 7.0;
    private static final int DEFAULT_LIFT_TICKS = 16;
    private static final double DEFAULT_SLAM_DAMAGE = 4.0;
    private static final double DEFAULT_RADIUS = 3.5;
    private static final int DEFAULT_WET_SLOW_AMP = 1;
    private static final int DEFAULT_WET_SLOW_TICKS = 40;
    private static final int COLUMN_SEGMENTS = 10;
    private static final int RISE_TICKS = 10;
    private static final int SLAM_TICKS = 6;
    private static final int FADE_TICKS = 6;
    private static final int DEBRIS_COUNT = 14;
    private static final int DEBRIS_FLING_TICKS = 10;

    private static final Color DEEP = Color.fromRGB(30, 90, 180);
    private static final Color MID = Color.fromRGB(70, 170, 230);
    private static final Color FOAM = Color.fromRGB(220, 240, 255);

    private GeyserAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("geyser");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double height = perks == null ? DEFAULT_HEIGHT : Math.max(3.0, perks.getDouble("height", DEFAULT_HEIGHT));
        int liftTicks = perks == null
                ? DEFAULT_LIFT_TICKS
                : Math.max(6, perks.getInt("lift-ticks", DEFAULT_LIFT_TICKS));
        double slamDamage = perks == null
                ? DEFAULT_SLAM_DAMAGE
                : Math.max(0.0, perks.getDouble("slam-damage", DEFAULT_SLAM_DAMAGE));
        double radius = perks == null ? DEFAULT_RADIUS : Math.max(1.0, perks.getDouble("radius", DEFAULT_RADIUS));
        int wetAmp = perks == null
                ? DEFAULT_WET_SLOW_AMP
                : Math.max(0, perks.getInt("wet-slow-amplifier", DEFAULT_WET_SLOW_AMP));
        int wetTicks = perks == null
                ? DEFAULT_WET_SLOW_TICKS
                : Math.max(10, perks.getInt("wet-slow-ticks", DEFAULT_WET_SLOW_TICKS));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("geyser");
        double damage = damageCfg.enabled() ? Math.max(slamDamage, damageCfg.value()) : slamDamage;
        PotionEffectType slowType = PotionTypes.resolve("SLOWNESS", "SLOW");

        List<ItemDisplay> column = new ArrayList<>(COLUMN_SEGMENTS);
        for (int i = 0; i < COLUMN_SEGMENTS; i++) {
            ItemDisplay display = GeyserDisplays.spawn(
                    origin.clone().add(0, 0.2, 0), GeyserDisplays.columnMaterial(i), 0.35f);
            if (display != null) {
                session.trackEntity(display);
                column.add(display);
            }
        }
        if (column.isEmpty()) {
            return;
        }

        List<DebrisBit> debris = spawnDebris(session, world, origin, killer, radius);
        Set<UUID> lifted = new HashSet<>();
        boolean[] slammed = {false};
        boolean[] cleaned = {false};

        Runnable cleanupAll = () -> {
            if (cleaned[0]) {
                return;
            }
            cleaned[0] = true;
            GeyserDisplays.removeAll(column);
            for (DebrisBit bit : debris) {
                GeyserDisplays.removeDebris(bit.display);
            }
            debris.clear();
        };

        session.onCleanup(cleanupAll);

        visuals.sound("ENTITY_GENERIC_SPLASH", origin, 1.2f, 0.75f);
        visuals.sound("WEATHER_RAIN", origin, 0.7f, 1.2f);

        AtomicInteger tick = new AtomicInteger();
        int holdStart = RISE_TICKS;
        int slamStart = holdStart + liftTicks;
        int fadeStart = slamStart + SLAM_TICKS;
        int total = fadeStart + FADE_TICKS;
        session.resetDeadline(total + 12L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                cleanupAll.run();
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                cleanupAll.run();
                return false;
            }

            double columnHeight;
            if (current < RISE_TICKS) {
                columnHeight = height * ((current + 1) / (double) RISE_TICKS);
            } else if (current < slamStart) {
                columnHeight = height + Math.sin(current * 0.35) * 0.22;
            } else if (current < fadeStart) {
                double slamProgress = (current - slamStart + 1) / (double) SLAM_TICKS;
                columnHeight = height * (1.0 - slamProgress * 0.92);
            } else {
                double fade = 1.0 - (current - fadeStart) / (double) FADE_TICKS;
                columnHeight = Math.max(0.15, height * 0.1 * fade);
            }

            updateColumn(visuals, origin, column, columnHeight, current);

            if (current < slamStart) {
                liftPlayers(session, visuals, world, origin, killer, victimId, radius, height, lifted, current);
                trembleDebris(debris, origin, current);
            }

            if (current >= slamStart && !slammed[0]) {
                slammed[0] = true;
                flingDebris(debris, origin);
                slam(
                        session,
                        visuals,
                        world,
                        origin,
                        killer,
                        victimId,
                        radius,
                        damage,
                        slowType,
                        wetAmp,
                        wetTicks,
                        lifted);
            }

            if (slammed[0]) {
                updateFlingDebris(debris, current - slamStart);
                if (current - slamStart >= DEBRIS_FLING_TICKS) {
                    for (DebrisBit bit : debris) {
                        GeyserDisplays.removeDebris(bit.display);
                    }
                    debris.clear();
                }
            }

            if (current >= fadeStart) {
                
                GeyserDisplays.removeAll(column);
            }

            if (current % 3 == 0) {
                visuals.particle(
                        "BUBBLE",
                        origin.clone().add(0, columnHeight * 0.5, 0),
                        8,
                        0.25,
                        columnHeight * 0.25,
                        0.25,
                        0.02,
                        null);
                visuals.particle(
                        "BUBBLE_POP",
                        origin.clone().add(0, Math.max(0.3, columnHeight - 0.2), 0),
                        4,
                        0.2,
                        0.1,
                        0.2,
                        0.01,
                        null);
            }
            if (current % 5 == 0 && current < slamStart) {
                visuals.sound("ENTITY_PLAYER_SPLASH", origin, 0.35f, 1.1f + (current % 14) * 0.02f);
            }
            return true;
        });
    }

    private static List<DebrisBit> spawnDebris(
            EffectSession session, World world, Location origin, Player killer, double radius) {
        List<DebrisBit> bits = new ArrayList<>();
        List<Block> surface = GeyserDisplays.sampleSurface(world, origin, killer, session, radius + 1.2, DEBRIS_COUNT);
        for (Block block : surface) {
            BlockData data = block.getBlockData().clone();
            Location base = block.getLocation().clone().add(0.5, 1.05, 0.5);
            BlockDisplay display = GeyserDisplays.spawnDebris(base, data);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            double phase = Math.random() * Math.PI * 2.0;
            float scale = 0.45f + (float) (Math.random() * 0.4);
            bits.add(new DebrisBit(display, base, phase, scale));
        }
        return bits;
    }

    private static void trembleDebris(List<DebrisBit> debris, Location origin, int current) {
        for (DebrisBit bit : debris) {
            if (bit.display == null || !bit.display.isValid() || bit.display.isDead()) {
                continue;
            }
            double shake = 0.08 + Math.sin(current * 0.55 + bit.phase) * 0.12;
            double jitterX = Math.sin(current * 0.7 + bit.phase) * 0.1;
            double jitterZ = Math.cos(current * 0.65 + bit.phase * 1.3) * 0.1;
            double lift = 0.15 + Math.abs(Math.sin(current * 0.4 + bit.phase)) * 0.35;
            Location at = bit.base.clone().add(jitterX, lift + shake, jitterZ);
            float yaw = (float) (Math.sin(current * 0.3 + bit.phase) * 0.35);
            float pitch = (float) (Math.cos(current * 0.28 + bit.phase) * 0.25);
            GeyserDisplays.placeDebris(bit.display, at, yaw, pitch, yaw * 0.5f, bit.scale);
            bit.loc = at;
        }
    }

    private static void flingDebris(List<DebrisBit> debris, Location origin) {
        for (DebrisBit bit : debris) {
            Vector away = bit.base.toVector().subtract(origin.toVector());
            away.setY(0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize();
            double horiz = 0.35 + Math.random() * 0.55;
            double vert = 0.45 + Math.random() * 0.55;
            bit.velocity = away.multiply(horiz).setY(vert);
            bit.flinging = true;
        }
    }

    private static void updateFlingDebris(List<DebrisBit> debris, int age) {
        for (DebrisBit bit : debris) {
            if (!bit.flinging || bit.display == null || !bit.display.isValid() || bit.display.isDead()) {
                continue;
            }
            if (bit.velocity == null) {
                continue;
            }
            bit.velocity.setY(bit.velocity.getY() - 0.06);
            bit.loc.add(bit.velocity);
            float spin = age * 0.45f + (float) bit.phase;
            GeyserDisplays.placeDebris(bit.display, bit.loc, spin, spin * 0.7f, spin * 0.4f, bit.scale * 0.92f);
        }
    }

    private static void updateColumn(
            VisualEffectService visuals,
            Location origin,
            List<ItemDisplay> column,
            double columnHeight,
            int current) {
        int n = column.size();
        for (int i = 0; i < n; i++) {
            double t = (i + 0.5) / n;
            double y = t * columnHeight;
            double swirl = current * 0.22 + i * 0.55;
            double radius = 0.15 + Math.sin(t * Math.PI) * 0.35;
            Location at = origin.clone().add(Math.cos(swirl) * radius, y, Math.sin(swirl) * radius);
            float scale = 0.45f + (float) Math.sin(t * Math.PI) * 0.25f;
            GeyserDisplays.place(column.get(i), at, (float) swirl, current * 0.05f, (float) (i * 0.2), scale);
            if (i % 2 == 0) {
                visuals.dust(at, t > 0.7 ? FOAM : (t > 0.35 ? MID : DEEP), 0.95f, 1, 0, 0, 0, 0);
            }
        }
        visuals.dust(origin.clone().add(0, columnHeight, 0), FOAM, 1.25f, 3, 0.15, 0.05, 0.15, 0.0);
    }

    private static void liftPlayers(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            double height,
            Set<UUID> lifted,
            int current) {
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
            lifted.add(player.getUniqueId());
            double lift = 0.38 + Math.sin(current * 0.35) * 0.06;
            Vector vel = player.getVelocity().clone();
            vel.setY(Math.max(vel.getY(), lift));
            Vector toCore = origin.toVector().subtract(player.getLocation().toVector());
            toCore.setY(0);
            if (toCore.lengthSquared() > 0.04) {
                toCore.normalize().multiply(0.05);
                vel.add(toCore);
            }
            player.setVelocity(vel);
            if (current % 3 == 0) {
                visuals.dust(player.getLocation().add(0, 1, 0), MID, 0.8f, 2, 0.1, 0.2, 0.1, 0.0);
            }
            if (player.getLocation().getY() > origin.getY() + height + 1.5) {
                Vector damp = player.getVelocity();
                damp.setY(Math.min(damp.getY(), 0.05));
                player.setVelocity(damp);
            }
        }
    }

    private static void slam(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            double damage,
            PotionEffectType slowType,
            int wetAmp,
            int wetTicks,
            Set<UUID> lifted) {
        visuals.particle("SPLASH", origin.clone().add(0, 0.4, 0), 50, radius * 0.4, 0.3, radius * 0.4, 0.15, null);
        visuals.particle("BUBBLE_POP", origin.clone().add(0, 0.6, 0), 30, radius * 0.35, 0.4, radius * 0.35, 0.05, null);
        visuals.dust(origin.clone().add(0, 0.5, 0), FOAM, 1.5f, 20, radius * 0.35, 0.25, radius * 0.35, 0.0);
        visuals.dust(origin.clone().add(0, 0.3, 0), DEEP, 1.2f, 14, radius * 0.3, 0.15, radius * 0.3, 0.0);
        visuals.sound("ENTITY_GENERIC_SPLASH", origin, 1.5f, 0.6f);
        visuals.sound("ENTITY_PLAYER_SPLASH_HIGH_SPEED", origin, 1.0f, 0.85f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.35f, 1.6f);

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
            boolean inRange = player.getLocation().distanceSquared(origin) <= radiusSq;
            boolean wasLifted = lifted.contains(player.getUniqueId());
            if (!inRange && !wasLifted) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            player.setVelocity(new Vector(0, -1.35, 0));
            if (killer != null && damage > 0.0) {
                player.damage(damage, killer);
            }
            if (slowType != null) {
                player.addPotionEffect(new PotionEffect(slowType, wetTicks, wetAmp, true, true, true));
            }
            visuals.particle("SPLASH", player.getLocation().add(0, 0.5, 0), 12, 0.3, 0.2, 0.3, 0.05, null);
        }
    }

    private static final class DebrisBit {
        private final BlockDisplay display;
        private final Location base;
        private final double phase;
        private final float scale;
        private Location loc;
        private @org.jspecify.annotations.Nullable Vector velocity;
        private boolean flinging;

        private DebrisBit(BlockDisplay display, Location base, double phase, float scale) {
            this.display = display;
            this.base = base.clone();
            this.phase = phase;
            this.scale = scale;
            this.loc = base.clone();
        }
    }
}
