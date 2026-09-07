package com.monkey.ktplus.effects.list.forgeanvil.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.forgeanvil.animation.util.ForgeAnvilDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import org.bukkit.util.Vector;

public final class ForgeAnvilAnimation {
    private static final int DEFAULT_SHADOW = 30;
    private static final double DEFAULT_SLAM = 7.0;
    private static final double DEFAULT_WAVE = 7.0;
    private static final int DEFAULT_SPARKS = 20;
    private static final int TOTAL_TICKS = 160;
    private static final int FALL_TICKS = 16;
    private static final double START_HEIGHT = 22.0;
    private static final String[] SOUNDS = {
        "BLOCK_ANVIL_LAND",
        "BLOCK_ANVIL_PLACE",
        "BLOCK_ANVIL_USE",
        "ENTITY_IRON_GOLEM_ATTACK",
        "ENTITY_GENERIC_EXPLODE",
        "BLOCK_FIRE_EXTINGUISH"
    };

    private static final Color STEEL = Color.fromRGB(160, 165, 175);
    private static final Color HOT = Color.fromRGB(255, 140, 40);
    private static final Color SPARK = Color.fromRGB(255, 220, 120);
    private static final Color SHADOW = Color.fromRGB(40, 40, 45);

    private ForgeAnvilAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("forgeanvil");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int shadowTicks = perks == null
                ? DEFAULT_SHADOW
                : Math.max(10, perks.getInt("shadow-ticks", DEFAULT_SHADOW));
        double slamDamage = perks == null
                ? DEFAULT_SLAM
                : Math.max(2.0, perks.getDouble("slam-damage", DEFAULT_SLAM));
        double waveRadius = perks == null
                ? DEFAULT_WAVE
                : Math.max(3.0, perks.getDouble("wave-radius", DEFAULT_WAVE));
        int sparkCount = perks == null
                ? DEFAULT_SPARKS
                : Math.max(8, perks.getInt("spark-count", DEFAULT_SPARKS));

        EffectDamageConfig damageCfg = context.config().effectDamage("forgeanvil");
        double damage = damageCfg.enabled() ? Math.max(slamDamage, damageCfg.value()) : slamDamage;
        double radius = damageCfg.enabled() ? Math.max(waveRadius, damageCfg.radius()) : waveRadius;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        BlockDisplay anvil = ForgeAnvilDisplays.spawnAnvil(origin.clone().add(0, START_HEIGHT, 0));
        if (anvil != null) {
            session.trackEntity(anvil);
        }

        List<Spark> sparks = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        Set<UUID> hitOnce = new HashSet<>();
        boolean[] slammed = {false};
        boolean[] finished = {false};
        double[] lastY = {START_HEIGHT};
        int descent = shadowTicks + FALL_TICKS;

        session.onCleanup(() -> {
            stopSounds(origin);
            ForgeAnvilDisplays.remove(anvil);
            for (Spark s : sparks) {
                ForgeAnvilDisplays.remove(s.display);
            }
            sparks.clear();
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(TOTAL_TICKS + 30L);

        visuals.sound("BLOCK_ANVIL_PLACE", origin, 0.7f, 0.45f);
        visuals.sound("ENTITY_IRON_GOLEM_HURT", origin, 0.5f, 0.4f);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL_TICKS) {
                finish(visuals, origin, anvil, sparks, killer);
                finished[0] = true;
                return false;
            }

            if (!slammed[0]) {
                double t = Math.min(1.0, (current + 1) / (double) descent);
                double ease = t * t * t;
                double y = START_HEIGHT * (1.0 - ease);
                if (y > lastY[0]) {
                    y = lastY[0];
                }
                lastY[0] = y;
                float scale = 3.0f + (float) ease * 0.8f;
                ForgeAnvilDisplays.placeAnvil(anvil, origin.clone().add(0, y, 0), scale, (float) (current * 0.01));

                drawShadow(visuals, origin, current, shadowTicks, radius);
                if (current < shadowTicks && current % 5 == 0) {
                    visuals.sound("BLOCK_ANVIL_USE", origin, 0.35f, 0.5f + (float) t * 0.3f);
                }
                if (current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&7⚒ FORGEANVIL &8| &f%s &7%d%%",
                                    current < shadowTicks ? "SHADOW" : "FALL",
                                    (int) (t * 100)));
                }

                if (current >= descent) {
                    slammed[0] = true;
                    ForgeAnvilDisplays.placeAnvil(anvil, origin.clone().add(0, 0.05, 0), 3.8f, 0.0f);
                    doSlam(session, visuals, world, origin, killer, victimId, damage, radius, sparkCount, sparks, hitOnce);
                }
                return true;
            }

            updateSparks(visuals, sparks);
            if (current % 4 == 0) {
                drawResidualSparks(visuals, origin, radius, current);
            }
            if (current % 3 == 0) {
                PerkActionBar.show(killer, "&7⚒ FORGEANVIL &8| &eSPARK WAVE &f" + sparks.size());
            }
            if (current > descent + 55 && sparks.isEmpty()) {
                finish(visuals, origin, anvil, sparks, killer);
                finished[0] = true;
                return false;
            }
            return true;
        });
    }

    private static void drawShadow(
            VisualEffectService visuals, Location origin, int current, int shadowTicks, double radius) {
        double progress = Math.min(1.0, (current + 1) / (double) shadowTicks);
        double r = radius * (0.3 + progress * 0.8);
        int pts = ParticleScale.scale(14 + (int) (progress * 10));
        for (int i = 0; i < pts; i++) {
            double a = (Math.PI * 2.0 * i) / pts + current * 0.02;
            Location p = origin.clone().add(Math.cos(a) * r, 0.06, Math.sin(a) * r);
            visuals.dust(p, SHADOW, 1.4f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        visuals.particle("SMOKE", origin.clone().add(0, 0.2, 0), ParticleScale.scale(3), r * 0.25, 0.05, r * 0.25, 0.0, null);
    }

    private static void doSlam(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double damage,
            double radius,
            int sparkCount,
            List<Spark> sparks,
            Set<UUID> hitOnce) {
        visuals.sound("BLOCK_ANVIL_LAND", origin, 1.6f, 0.55f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 1.1f, 0.7f);
        visuals.sound("ENTITY_IRON_GOLEM_ATTACK", origin, 1.2f, 0.5f);
        visuals.dust(origin.clone().add(0, 0.4, 0), STEEL, 2.0f, ParticleScale.scale(28), 1.4, 0.35, 1.4, 0.0);
        visuals.dust(origin.clone().add(0, 0.5, 0), HOT, 1.6f, ParticleScale.scale(16), 1.0, 0.3, 1.0, 0.0);
        visuals.particle("EXPLOSION", origin.clone().add(0, 0.6, 0), ParticleScale.scale(4), 0.8, 0.2, 0.8, 0.0, null);
        visuals.particle("LAVA", origin.clone().add(0, 0.3, 0), ParticleScale.scale(10), 0.8, 0.1, 0.8, 0.0, null);

        for (int ring = 0; ring < 3; ring++) {
            double r = radius * (0.35 + ring * 0.3);
            int pts = ParticleScale.scale(16);
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI * 2.0 * i) / pts;
                Location p = origin.clone().add(Math.cos(a) * r, 0.12, Math.sin(a) * r);
                visuals.dust(p, SPARK, 1.5f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        for (int i = 0; i < sparkCount; i++) {
            double a = Math.random() * Math.PI * 2.0;
            Location spawn = origin.clone().add(Math.cos(a) * 0.6, 0.5, Math.sin(a) * 0.6);
            ItemDisplay display = ForgeAnvilDisplays.spawnSpark(spawn);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            Vector vel = new Vector(Math.cos(a), 0.45 + Math.random() * 0.5, Math.sin(a))
                    .multiply(0.55 + Math.random() * 0.45);
            sparks.add(new Spark(display, spawn, vel));
        }

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
            if (hitOnce.contains(player.getUniqueId())) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            hitOnce.add(player.getUniqueId());
            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            Vector away = player.getLocation().toVector().subtract(origin.toVector());
            away.setY(0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(1.2).setY(0.55);
            player.setVelocity(away);
        }
    }

    private static void updateSparks(VisualEffectService visuals, List<Spark> sparks) {
        Iterator<Spark> it = sparks.iterator();
        while (it.hasNext()) {
            Spark s = it.next();
            if (s.display == null || !s.display.isValid() || s.display.isDead()) {
                it.remove();
                continue;
            }
            s.age++;
            s.velocity.setY(s.velocity.getY() - 0.045);
            s.loc.add(s.velocity);
            ForgeAnvilDisplays.placeSpark(s.display, s.loc, 0.3f + (s.age % 4) * 0.02f, s.age * 0.4f);
            if (s.age % 2 == 0) {
                visuals.dust(s.loc, SPARK, 0.9f, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (s.age > 40 || s.loc.getY() < s.spawnY - 0.2) {
                visuals.dust(s.loc, HOT, 1.1f, 3, 0.1, 0.1, 0.1, 0.0);
                ForgeAnvilDisplays.remove(s.display);
                it.remove();
            }
        }
    }

    private static void drawResidualSparks(
            VisualEffectService visuals, Location origin, double radius, int tick) {
        int pts = ParticleScale.scale(8);
        for (int i = 0; i < pts; i++) {
            double a = tick * 0.08 + (Math.PI * 2.0 * i) / pts;
            Location p = origin.clone().add(Math.cos(a) * radius * 0.7, 0.1, Math.sin(a) * radius * 0.7);
            visuals.dust(p, STEEL, 1.0f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            BlockDisplay anvil,
            List<Spark> sparks,
            Player killer) {
        visuals.sound("BLOCK_FIRE_EXTINGUISH", origin, 0.7f, 0.8f);
        visuals.particle("SMOKE", origin.clone().add(0, 1, 0), ParticleScale.scale(12), 0.8, 0.4, 0.8, 0.02, null);
        stopSounds(origin);
        ForgeAnvilDisplays.remove(anvil);
        for (Spark s : sparks) {
            ForgeAnvilDisplays.remove(s.display);
        }
        sparks.clear();
        PerkActionBar.clear(killer);
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Spark {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final double spawnY;
        private int age;

        private Spark(ItemDisplay display, Location loc, Vector velocity) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.spawnY = loc.getY();
        }
    }
}
