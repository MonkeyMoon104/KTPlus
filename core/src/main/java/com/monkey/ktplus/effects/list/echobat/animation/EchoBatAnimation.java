package com.monkey.ktplus.effects.list.echobat.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.echobat.animation.util.EchoBatDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Bat;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class EchoBatAnimation {
    private static final int DEFAULT_BAT_COUNT = 10;
    private static final double DEFAULT_SCREECH_RADIUS = 6.0;
    private static final int DEFAULT_DARKNESS_TICKS = 40;
    private static final double DEFAULT_SEEK_RANGE = 14.0;
    private static final int DEFAULT_DURATION = 170;
    private static final int INTRO_TICKS = 16;
    private static final int SCREECH_COOLDOWN = 28;
    private static final double SPEED = 0.38;

    private static final Color ECHO = Color.fromRGB(20, 90, 95);
    private static final Color SCULK = Color.fromRGB(10, 55, 60);
    private static final Color TEAL = Color.fromRGB(40, 160, 155);
    private static final Color PULSE = Color.fromRGB(70, 210, 200);

    private enum Phase {
        INTRO,
        SEEK,
        SCREECH,
        RETREAT,
        DONE
    }

    private EchoBatAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.8, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("echobat");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int batCount = perks == null
                ? DEFAULT_BAT_COUNT
                : Math.max(4, Math.min(20, perks.getInt("bat-count", DEFAULT_BAT_COUNT)));
        double screechRadius = perks == null
                ? DEFAULT_SCREECH_RADIUS
                : Math.max(2.0, perks.getDouble("screech-radius", DEFAULT_SCREECH_RADIUS));
        int darknessTicks = perks == null
                ? DEFAULT_DARKNESS_TICKS
                : Math.max(0, perks.getInt("darkness-ticks", DEFAULT_DARKNESS_TICKS));
        double seekRange = perks == null
                ? DEFAULT_SEEK_RANGE
                : Math.max(4.0, perks.getDouble("seek-range", DEFAULT_SEEK_RANGE));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(80, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("echobat");
        double screechDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 2.0;
        double damageRadius = damageCfg.enabled() && damageCfg.radius() > 0.0 ? damageCfg.radius() : 4.0;

        PotionEffectType darkness = PotionTypes.resolve("DARKNESS", "DARKNESS");
        if (darkness == null) {
            darkness = PotionTypes.resolve("BLINDNESS", "BLINDNESS");
        }

        List<EchoBat> bats = new ArrayList<>(batCount);
        for (int i = 0; i < batCount; i++) {
            double angle = (Math.PI * 2.0 * i) / batCount + Math.random() * 0.2;
            double radius = 1.3 + (i % 4) * 0.35;
            Location spawn = origin.clone().add(
                    Math.cos(angle) * radius,
                    0.4 + (i % 3) * 0.25,
                    Math.sin(angle) * radius);
            Bat entity = EchoBatDisplays.spawnBat(spawn);
            ItemDisplay shard = null;
            if (entity == null) {
                shard = EchoBatDisplays.spawnShard(spawn, 0.35f + (i % 3) * 0.05f);
                if (shard == null) {
                    continue;
                }
                session.trackEntity(shard);
            } else {
                session.trackEntity(entity);
            }
            bats.add(new EchoBat(
                    entity,
                    shard,
                    spawn,
                    i,
                    angle,
                    radius,
                    INTRO_TICKS + (i % 4) * 2));
        }

        if (bats.isEmpty()) {
            return;
        }

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        final String loopSound = "BLOCK_SCULK_SENSOR_CLICKING";

        session.onCleanup(() -> {
            for (EchoBat bat : bats) {
                removeBat(bat);
            }
            bats.clear();
            if (killer != null && killer.isOnline()) {
                EntityCompat.stopSound(killer, loopSound, "BLOCKS");
            }
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_BAT_TAKEOFF", origin, 0.75f, 0.85f);
        visuals.sound("BLOCK_SCULK_SENSOR_CLICKING", origin, 0.55f, 1.4f);
        visuals.dust(origin, TEAL, 1.3f, ParticleScale.scale(12), 0.45, 0.35, 0.45, 0.0);
        session.resetDeadline(durationTicks + 40L);

        final PotionEffectType darknessType = darkness;
        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || bats.isEmpty()) {
                for (EchoBat bat : bats) {
                    puffOut(visuals, bat);
                }
                bats.clear();
                if (killer != null && killer.isOnline()) {
                    EntityCompat.stopSound(killer, loopSound, "BLOCKS");
                }
                visuals.sound("ENTITY_BAT_DEATH", origin, 0.55f, 0.9f);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            int seeking = 0;
            Iterator<EchoBat> it = bats.iterator();
            while (it.hasNext()) {
                EchoBat bat = it.next();
                if (!batAlive(bat) || bat.phase == Phase.DONE) {
                    puffOut(visuals, bat);
                    it.remove();
                    continue;
                }
                if (updateBat(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        origin,
                        bat,
                        seekRange,
                        screechRadius,
                        damageRadius,
                        screechDamage,
                        darknessTicks,
                        darknessType,
                        current)) {
                    seeking++;
                }
                if (bat.phase == Phase.DONE) {
                    puffOut(visuals, bat);
                    it.remove();
                }
            }

            if (current % 9 == 0) {
                visuals.sound(loopSound, origin, 0.28f, 1.35f + (float) (Math.random() * 0.25));
            }
            if (current % 14 == 0) {
                visuals.sound("ENTITY_BAT_AMBIENT", origin, 0.2f, 0.75f);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format("&3🦇 ECHOBAT &8| &b%d &8| &3SEEK &f%d", bats.size(), seeking));
            }
            return true;
        });
    }

    private static boolean updateBat(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location hive,
            EchoBat bat,
            double seekRange,
            double screechRadius,
            double damageRadius,
            double screechDamage,
            int darknessTicks,
            PotionEffectType darkness,
            int tick) {
        boolean seeking = false;
        switch (bat.phase) {
            case INTRO -> {
                double spin = bat.orbitAngle + tick * 0.18 * (bat.index % 2 == 0 ? 1 : -1);
                double y = Math.sin(tick * 0.22 + bat.index) * 0.35 + 0.4;
                bat.loc = hive.clone().add(
                        Math.cos(spin) * bat.orbitRadius,
                        y,
                        Math.sin(spin) * bat.orbitRadius);
                bat.yaw = (float) Math.toDegrees(spin + Math.PI * 0.5);
                bat.phaseTicks--;
                if (bat.phaseTicks <= 0) {
                    bat.phase = Phase.SEEK;
                    bat.screechCooldown = bat.index % SCREECH_COOLDOWN;
                }
            }
            case SEEK -> {
                seeking = true;
                Player target = resolveTarget(world, bat, killer, victimId, hive, seekRange, session);
                if (target != null) {
                    Location aim = target.getLocation().clone().add(
                            Math.cos(tick * 0.1 + bat.index) * 0.4,
                            1.4 + Math.sin(tick * 0.18 + bat.index) * 0.25,
                            Math.sin(tick * 0.1 + bat.index) * 0.4);
                    Vector to = aim.toVector().subtract(bat.loc.toVector());
                    double dist = to.length();
                    if (dist > 0.001) {
                        Vector step = to.normalize().multiply(Math.min(SPEED, Math.max(0.1, dist * 0.28)));
                        step.setY(step.getY() + Math.sin(tick * 0.35 + bat.index) * 0.04);
                        bat.loc.add(step);
                        bat.yaw = (float) Math.toDegrees(Math.atan2(-step.getX(), step.getZ()));
                    }
                    if (dist < screechRadius * 0.55 && bat.screechCooldown <= 0 && !bat.hasHit) {
                        bat.hasHit = true;
                        doScreech(
                                session,
                                visuals,
                                world,
                                killer,
                                victimId,
                                bat.loc,
                                screechRadius,
                                damageRadius,
                                screechDamage,
                                darknessTicks,
                                darkness);
                        bat.phase = Phase.DONE;
                    }
                } else {
                    bat.loc.add(
                            Math.cos(tick * 0.12 + bat.index) * 0.08,
                            Math.sin(tick * 0.18 + bat.index) * 0.04,
                            Math.sin(tick * 0.12 + bat.index) * 0.08);
                }
            }
            case SCREECH, RETREAT -> {
                bat.phase = Phase.DONE;
            }
            default -> {
            }
        }

        if (bat.screechCooldown > 0) {
            bat.screechCooldown--;
        }
        placeBat(bat);
        drawTrail(visuals, bat, tick);
        return seeking;
    }

    private static void doScreech(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location at,
            double screechRadius,
            double damageRadius,
            double screechDamage,
            int darknessTicks,
            PotionEffectType darkness) {
        visuals.sound("BLOCK_SCULK_SHRIEKER_SHRIEK", at, 0.55f, 1.55f);
        visuals.sound("ENTITY_BAT_HURT", at, 0.7f, 0.65f);
        visuals.dust(at, TEAL, 1.5f, ParticleScale.scale(14), 0.55, 0.4, 0.55, 0.0);
        visuals.dust(at, ECHO, 1.2f, ParticleScale.scale(10), 0.7, 0.5, 0.7, 0.0);
        visuals.dust(at, PULSE, 1.0f, ParticleScale.scale(6), 0.35, 0.25, 0.35, 0.0);
        visuals.particle("SONIC_BOOM", at, 1, 0.0, 0.0, 0.0, 0.0, null);
        visuals.particle("CLOUD", at, ParticleScale.scale(4), 0.3, 0.2, 0.3, 0.01, null);

        double screechSq = screechRadius * screechRadius;
        double damageSq = damageRadius * damageRadius;
        for (Player player : findNearby(world, at, killer, victimId, screechRadius, session)) {
            if (player.getLocation().distanceSquared(at) > screechSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (darkness != null && darknessTicks > 0) {
                player.addPotionEffect(new PotionEffect(darkness, darknessTicks, 0, true, true, true));
            }
            if (screechDamage > 0.0
                    && killer != null
                    && player.getLocation().distanceSquared(at) <= damageSq) {
                player.damage(screechDamage, killer);
            }
            visuals.dust(player.getLocation().clone().add(0, 1.2, 0), SCULK, 1.0f, 4, 0.15, 0.2, 0.15, 0.0);
        }
    }

    private static void drawTrail(VisualEffectService visuals, EchoBat bat, int tick) {
        if (tick % 2 == bat.index % 2) {
            visuals.dust(bat.loc, TEAL, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % 3 == bat.index % 3) {
            visuals.dust(bat.loc.clone().add(0, 0.1, 0), ECHO, 0.65f, 1, 0.02, 0.02, 0.02, 0.0);
            visuals.dust(bat.loc, SCULK, 0.55f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void puffOut(VisualEffectService visuals, EchoBat bat) {
        visuals.dust(bat.loc, TEAL, 1.3f, ParticleScale.scale(8), 0.25, 0.25, 0.25, 0.0);
        visuals.dust(bat.loc, SCULK, 1.0f, ParticleScale.scale(5), 0.3, 0.2, 0.3, 0.0);
        visuals.particle("CLOUD", bat.loc, 3, 0.12, 0.12, 0.12, 0.01, null);
        removeBat(bat);
    }

    private static void placeBat(EchoBat bat) {
        if (bat.entity != null) {
            EchoBatDisplays.placeBat(bat.entity, bat.loc, bat.yaw);
        }
        if (bat.shard != null) {
            float spin = bat.yaw * 0.02f;
            EchoBatDisplays.placeShard(bat.shard, bat.loc, 0.38f, spin, spin * 0.6f, spin * 0.4f);
        }
    }

    private static boolean batAlive(EchoBat bat) {
        if (bat.entity != null) {
            return bat.entity.isValid() && !bat.entity.isDead();
        }
        return bat.shard != null && bat.shard.isValid() && !bat.shard.isDead();
    }

    private static void removeBat(EchoBat bat) {
        EchoBatDisplays.removeBat(bat.entity);
        EchoBatDisplays.removeShard(bat.shard);
    }

    private static Player resolveTarget(
            World world,
            EchoBat bat,
            Player killer,
            UUID victimId,
            Location hive,
            double radius,
            EffectSession session) {
        if (bat.targetId != null) {
            for (Player player : world.getPlayers()) {
                if (player.getUniqueId().equals(bat.targetId)
                        && player.isOnline()
                        && !player.isDead()
                        && player.getWorld().equals(world)
                        && player.getLocation().distanceSquared(hive) <= radius * radius) {
                    return player;
                }
            }
            bat.targetId = null;
        }
        List<Player> nearby = findNearby(world, hive, killer, victimId, radius, session);
        if (nearby.isEmpty()) {
            return null;
        }
        Player next = nearby.get(bat.index % nearby.size());
        bat.targetId = next.getUniqueId();
        return next;
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

    private static final class EchoBat {
        private final Bat entity;
        private final ItemDisplay shard;
        private Location loc;
        private final int index;
        private double orbitAngle;
        private final double orbitRadius;
        private UUID targetId;
        private float yaw;
        private int screechCooldown;
        private boolean hasHit;
        private Phase phase = Phase.INTRO;
        private int phaseTicks;

        private EchoBat(
                Bat entity,
                ItemDisplay shard,
                Location loc,
                int index,
                double orbitAngle,
                double orbitRadius,
                int introTicks) {
            this.entity = entity;
            this.shard = shard;
            this.loc = loc.clone();
            this.index = index;
            this.orbitAngle = orbitAngle;
            this.orbitRadius = orbitRadius;
            this.phaseTicks = introTicks;
        }
    }
}
