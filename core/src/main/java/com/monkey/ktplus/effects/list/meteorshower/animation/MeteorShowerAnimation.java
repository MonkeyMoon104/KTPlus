package com.monkey.ktplus.effects.list.meteorshower.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.meteorshower.animation.util.MeteorShowerDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class MeteorShowerAnimation {
    private static final int MAX_TICKS = 160;
    private static final int DEFAULT_COUNT = 7;
    private static final double DEFAULT_SPREAD = 8.0;
    private static final double DEFAULT_IMPACT = 4.0;
    private static final double FALL_SPEED = 0.55;
    private static final double IMPACT_RADIUS = 2.4;

    private static final Color EMBER = Color.fromRGB(255, 120, 40);
    private static final Color CORE = Color.fromRGB(255, 220, 120);
    private static final Color ASH = Color.fromRGB(70, 55, 45);

    private static final String[] AMBIENT_SOUNDS = {
        "ENTITY_BLAZE_SHOOT",
        "ENTITY_GENERIC_EXPLODE",
        "BLOCK_FIRE_AMBIENT",
        "ENTITY_FIREWORK_ROCKET_BLAST"
    };

    private MeteorShowerAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("meteorshower");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int count = perks == null
                ? DEFAULT_COUNT
                : Math.max(5, Math.min(8, perks.getInt("meteor-count", DEFAULT_COUNT)));
        double spread = perks == null ? DEFAULT_SPREAD : Math.max(3.0, perks.getDouble("spread", DEFAULT_SPREAD));
        double impactDamageBase = perks == null
                ? DEFAULT_IMPACT
                : Math.max(0.0, perks.getDouble("impact-damage", DEFAULT_IMPACT));
        boolean craterVfx = perks == null || perks.getBoolean("crater-vfx", true);

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("meteorshower");
        final double impactDamage = damageCfg.enabled()
                ? Math.max(impactDamageBase, damageCfg.value())
                : impactDamageBase;

        List<Meteor> meteors = new ArrayList<>(count);
        List<ItemDisplay> displays = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / count + Math.random() * 0.4;
            double dist = 1.5 + Math.random() * spread;
            double tx = origin.getX() + Math.cos(angle) * dist;
            double tz = origin.getZ() + Math.sin(angle) * dist;
            double groundY = findGroundY(world, tx, origin.getY(), tz);
            Location target = new Location(world, tx, groundY + 0.2, tz);
            Location spawn = target.clone().add(
                    (Math.random() - 0.5) * 2.5,
                    12.0 + Math.random() * 6.0,
                    (Math.random() - 0.5) * 2.5);
            ItemDisplay display = MeteorShowerDisplays.spawn(
                    spawn, MeteorShowerDisplays.meteorMaterial(i), 0.55f + (float) (Math.random() * 0.35));
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            meteors.add(new Meteor(display, spawn.clone(), target, i * 8 + (int) (Math.random() * 6), i));
        }

        if (meteors.isEmpty()) {
            return;
        }

        session.onCleanup(() -> {
            stopAmbient(origin);
            MeteorShowerDisplays.removeAll(displays);
        });

        visuals.sound("ENTITY_BLAZE_SHOOT", origin, 1.0f, 0.55f);
        visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", origin, 0.6f, 0.7f);
        session.resetDeadline(MAX_TICKS + 20L);

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopAmbient(origin);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= MAX_TICKS || meteors.stream().allMatch(m -> m.done)) {
                stopAmbient(origin);
                MeteorShowerDisplays.removeAll(displays);
                finished[0] = true;
                return false;
            }

            for (Meteor meteor : meteors) {
                updateMeteor(session, visuals, world, killer, victimId, meteor, impactDamage, craterVfx, current);
            }
            if (current % 12 == 0) {
                visuals.sound("BLOCK_FIRE_AMBIENT", origin, 0.25f, 0.8f);
            }
            return true;
        });
    }

    private static void updateMeteor(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Meteor meteor,
            double impactDamage,
            boolean craterVfx,
            int globalTick) {
        if (meteor.done) {
            return;
        }
        if (globalTick < meteor.delay) {
            return;
        }

        int age = meteor.age++;
        Vector to = meteor.target.toVector().subtract(meteor.loc.toVector());
        double dist = to.length();
        if (dist <= 0.45 || meteor.loc.getY() <= meteor.target.getY() + 0.15) {
            impact(session, visuals, world, killer, victimId, meteor, impactDamage, craterVfx);
            MeteorShowerDisplays.remove(meteor.display);
            meteor.done = true;
            return;
        }

        Vector step = to.normalize().multiply(Math.min(FALL_SPEED + age * 0.012, dist));
        meteor.loc.add(step);
        float spin = age * 0.35f + meteor.index;
        float scale = 0.65f + (float) Math.sin(age * 0.2) * 0.08f;
        MeteorShowerDisplays.place(meteor.display, meteor.loc, spin, 0.9f, spin * 0.4f, scale);

        visuals.dust(meteor.loc, age % 2 == 0 ? EMBER : CORE, 1.2f, 2, 0.05, 0.05, 0.05, 0.0);
        if (age % 2 == 0) {
            visuals.particle("CLOUD", meteor.loc, 1, 0.04, 0.04, 0.04, 0.0, null);
        }
        if (age % 5 == 0) {
            visuals.sound("ENTITY_BLAZE_SHOOT", meteor.loc, 0.25f, 1.4f);
        }
    }

    private static void impact(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Meteor meteor,
            double impactDamage,
            boolean craterVfx) {
        Location at = meteor.target.clone();
        visuals.sound("ENTITY_GENERIC_EXPLODE", at, 0.95f, 1.15f);
        visuals.sound("ENTITY_FIREWORK_ROCKET_BLAST", at, 0.55f, 0.85f);
        visuals.dust(at, EMBER, 1.8f, ParticleScale.scale(16), 0.55, 0.25, 0.55, 0.0);
        visuals.dust(at, CORE, 1.4f, ParticleScale.scale(10), 0.4, 0.2, 0.4, 0.0);
        visuals.particle("CLOUD", at.clone().add(0, 0.4, 0), ParticleScale.scale(14), 0.5, 0.3, 0.5, 0.03, null);

        if (craterVfx) {
            for (int i = 0; i < 10; i++) {
                double a = (Math.PI * 2.0 * i) / 10.0;
                double r = 0.6 + (i % 3) * 0.35;
                Location ring = at.clone().add(Math.cos(a) * r, 0.08, Math.sin(a) * r);
                visuals.dust(ring, ASH, 1.1f, 2, 0.05, 0.02, 0.05, 0.0);
            }
        }

        if (killer == null || impactDamage <= 0.0) {
            return;
        }
        double radiusSq = IMPACT_RADIUS * IMPACT_RADIUS;
        Set<UUID> hit = new HashSet<>();
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
            if (player.getLocation().distanceSquared(at) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (!hit.add(player.getUniqueId())) {
                continue;
            }
            player.damage(impactDamage, killer);
            Vector away = player.getLocation().toVector().subtract(at.toVector());
            away.setY(0.0);
            if (away.lengthSquared() < 1.0e-4) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(0.55).setY(0.4);
            player.setVelocity(player.getVelocity().add(away));
        }
    }

    private static double findGroundY(World world, double x, double aroundY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int start = Math.min(world.getMaxHeight() - 2, (int) Math.floor(aroundY) + 4);
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(aroundY) - 8);
        for (int y = start; y >= minY; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (block.getType().isSolid()) {
                return y + 1.0;
            }
        }
        return aroundY;
    }

    private static void stopAmbient(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Meteor {
        private final ItemDisplay display;
        private final Location loc;
        private final Location target;
        private final int delay;
        private final int index;
        private int age;
        private boolean done;

        private Meteor(ItemDisplay display, Location loc, Location target, int delay, int index) {
            this.display = display;
            this.loc = loc;
            this.target = target;
            this.delay = delay;
            this.index = index;
        }
    }
}
