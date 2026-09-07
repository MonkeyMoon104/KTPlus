package com.monkey.ktplus.effects.list.snowballstorm.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.snowballstorm.animation.util.SnowballStormDisplays;
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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class SnowballStormAnimation {
    private static final int DEFAULT_COUNT = 18;
    private static final double DEFAULT_SPREAD = 0.5;
    private static final int DEFAULT_SLOW_TICKS = 40;
    private static final double DEFAULT_SPEED = 0.55;
    private static final int DEFAULT_DURATION = 130;
    private static final double HIT_DISTANCE = 1.05;
    private static final int MAX_FLIGHT = 50;
    private static final double GRAVITY = 0.018;

    private static final Color ICE = Color.fromRGB(180, 230, 255);
    private static final Color SNOW = Color.fromRGB(240, 248, 255);
    private static final Color FROST = Color.fromRGB(120, 190, 230);
    private static final Color MIST = Color.fromRGB(200, 220, 235);

    private SnowballStormAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.15, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("snowballstorm");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int count = perks == null
                ? DEFAULT_COUNT
                : Math.max(6, Math.min(36, perks.getInt("count", DEFAULT_COUNT)));
        double spread = perks == null
                ? DEFAULT_SPREAD
                : Math.max(0.1, perks.getDouble("spread", DEFAULT_SPREAD));
        int slowTicks = perks == null
                ? DEFAULT_SLOW_TICKS
                : Math.max(0, perks.getInt("slow-ticks", DEFAULT_SLOW_TICKS));
        double speed = perks == null
                ? DEFAULT_SPEED
                : Math.max(0.2, perks.getDouble("speed", DEFAULT_SPEED));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(60, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("snowballstorm");
        double hitDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 0.0;
        PotionEffectType slowType = PotionTypes.resolve("SLOWNESS", "SLOW");

        List<Player> targets = findNearby(world, origin, killer, victimId, 14.0, session);
        List<Snowball> balls = new ArrayList<>(count);
        List<ItemDisplay> displays = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Vector dir;
            if (!targets.isEmpty()) {
                Player aim = targets.get(i % targets.size());
                Location aimAt = aim.getLocation().clone().add(0, 1.0, 0);
                dir = aimAt.toVector().subtract(origin.toVector());
                if (dir.lengthSquared() < 1.0e-4) {
                    dir = randomCone(i, spread);
                } else {
                    dir.normalize()
                            .add(new Vector(
                                    (Math.random() - 0.5) * spread,
                                    (Math.random() - 0.15) * spread * 0.45,
                                    (Math.random() - 0.5) * spread))
                            .normalize();
                }
            } else {
                dir = randomCone(i, spread);
            }

            Location spawn = origin.clone().add(
                    (Math.random() - 0.5) * 0.4,
                    (i % 5) * 0.08,
                    (Math.random() - 0.5) * 0.4);
            float scale = 0.35f + (i % 3) * 0.06f;
            ItemDisplay display = SnowballStormDisplays.spawn(spawn, scale);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            int launchDelay = i / 3;
            balls.add(new Snowball(display, spawn, dir.multiply(speed), scale, i, launchDelay));
        }

        if (balls.isEmpty()) {
            return;
        }

        Set<UUID> hitOnce = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            SnowballStormDisplays.removeAll(displays);
            displays.clear();
            balls.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_SNOWBALL_THROW", origin, 1.0f, 0.85f);
        visuals.sound("BLOCK_POWDER_SNOW_PLACE", origin, 0.7f, 1.3f);
        visuals.dust(origin, ICE, 1.3f, ParticleScale.scale(12), 0.4, 0.25, 0.4, 0.0);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || balls.stream().allMatch(b -> b.done)) {
                finish(visuals, origin, balls, displays);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            int flying = 0;
            for (Snowball ball : balls) {
                if (ball.done) {
                    continue;
                }
                if (current < ball.launchDelay) {
                    float hover = ball.scale * (0.9f + (float) Math.sin(current * 0.3 + ball.index) * 0.08f);
                    SnowballStormDisplays.place(
                            ball.display,
                            ball.loc,
                            hover,
                            current * 0.1f,
                            current * 0.08f,
                            current * 0.12f);
                    continue;
                }
                flying++;
                updateBall(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        ball,
                        hitDamage,
                        slowTicks,
                        slowType,
                        hitOnce,
                        current);
            }

            if (current % 8 == 0) {
                visuals.sound("BLOCK_SNOW_BREAK", origin, 0.25f, 1.4f + (float) (Math.random() * 0.3));
            }
            if (current % 5 == 0) {
                visuals.dust(origin, SNOW, 0.9f, ParticleScale.scale(3), 0.8, 0.5, 0.8, 0.0);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format("&b❄ SNOWBALLSTORM &8| &f%d &8| &7FLY", flying));
            }
            return true;
        });
    }

    private static void updateBall(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Snowball ball,
            double hitDamage,
            int slowTicks,
            PotionEffectType slowType,
            Set<UUID> hitOnce,
            int tick) {
        ball.age++;
        if (ball.age >= MAX_FLIGHT) {
            pop(visuals, ball);
            return;
        }

        ball.vel.setY(ball.vel.getY() - GRAVITY);
        ball.loc.add(ball.vel);

        if (ball.loc.getBlock().getType().isSolid()) {
            pop(visuals, ball);
            return;
        }

        float spin = tick * 0.28f + ball.index * 0.35f;
        SnowballStormDisplays.place(ball.display, ball.loc, ball.scale, spin, spin * 0.5f, spin * 0.7f);

        if (tick % 2 == ball.index % 2) {
            visuals.dust(ball.loc, ICE, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
            visuals.dust(ball.loc.clone().add(0, 0.05, 0), SNOW, 0.65f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % 3 == ball.index % 3) {
            visuals.dust(ball.loc, FROST, 0.55f, 1, 0.02, 0.02, 0.02, 0.0);
        }

        for (Player player : findNearby(world, ball.loc, killer, victimId, 2.2, session)) {
            if (player.getLocation().clone().add(0, 1.0, 0).distanceSquared(ball.loc) > HIT_DISTANCE * HIT_DISTANCE) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (hitDamage > 0.0 && killer != null && hitOnce.add(player.getUniqueId())) {
                player.damage(hitDamage, killer);
            }
            if (slowType != null && slowTicks > 0) {
                player.addPotionEffect(new PotionEffect(slowType, slowTicks, 0, true, true, true));
            }
            visuals.sound("ENTITY_PLAYER_HURT_FREEZE", player.getLocation(), 0.75f, 1.3f);
            visuals.dust(player.getLocation().clone().add(0, 1, 0), ICE, 1.3f, ParticleScale.scale(8), 0.25, 0.3, 0.25, 0.0);
            pop(visuals, ball);
            return;
        }
    }

    private static void pop(VisualEffectService visuals, Snowball ball) {
        if (ball.done) {
            return;
        }
        ball.done = true;
        visuals.dust(ball.loc, SNOW, 1.2f, ParticleScale.scale(7), 0.2, 0.2, 0.2, 0.0);
        visuals.dust(ball.loc, MIST, 0.9f, ParticleScale.scale(4), 0.25, 0.15, 0.25, 0.0);
        visuals.particle("CLOUD", ball.loc, 2, 0.1, 0.1, 0.1, 0.01, null);
        visuals.sound("BLOCK_SNOW_BREAK", ball.loc, 0.55f, 1.5f);
        SnowballStormDisplays.remove(ball.display);
    }

    private static void finish(
            VisualEffectService visuals, Location origin, List<Snowball> balls, List<ItemDisplay> displays) {
        visuals.sound("BLOCK_POWDER_SNOW_BREAK", origin, 0.8f, 1.1f);
        visuals.dust(origin, ICE, 1.4f, ParticleScale.scale(14), 0.6, 0.4, 0.6, 0.0);
        for (Snowball ball : balls) {
            if (!ball.done) {
                SnowballStormDisplays.remove(ball.display);
            }
        }
        balls.clear();
        displays.clear();
    }

    private static Vector randomCone(int index, double spread) {
        double yaw = (Math.PI * 2.0 * index) / 12.0 + Math.random() * spread;
        double pitch = 0.15 + Math.random() * 0.35;
        return new Vector(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch))
                .normalize();
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

    private static final class Snowball {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector vel;
        private final float scale;
        private final int index;
        private final int launchDelay;
        private int age;
        private boolean done;

        private Snowball(
                ItemDisplay display, Location loc, Vector vel, float scale, int index, int launchDelay) {
            this.display = display;
            this.loc = loc.clone();
            this.vel = vel.clone();
            this.scale = scale;
            this.index = index;
            this.launchDelay = launchDelay;
        }
    }
}
