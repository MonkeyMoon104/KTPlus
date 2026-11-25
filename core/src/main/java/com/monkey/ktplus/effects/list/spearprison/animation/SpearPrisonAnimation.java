package com.monkey.ktplus.effects.list.spearprison.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectGameplay;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.visual.display.DisplayFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SpearPrisonAnimation {
    private static final int RISE = 45;
    private static final int CLOSE = 55;
    private static final int BURST = 28;
    private static final int SPEARS = 12;
    private static final Color SEA = Color.fromRGB(60, 170, 220);

    private SpearPrisonAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        EffectDamageConfig damageCfg = context.config().effectDamage("spearprison");
        double crushDamage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 5.0;
        double radius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 5.5;
        Player killer = context.killer();
        int total = RISE + CLOSE + BURST;

        List<Spear> spears = new ArrayList<>();
        for (int i = 0; i < SPEARS; i++) {
            double angle = (Math.PI * 2.0 * i) / SPEARS;
            Location ground = origin.clone().add(Math.cos(angle) * 3.2, 0, Math.sin(angle) * 3.2);
            ItemDisplay spear = DisplayFactory.item(ground.clone().add(0, -2.5, 0), Material.TRIDENT, 0.6f, 0.6f, 0.6f);
            if (spear == null) {
                continue;
            }
            DisplayFactory.glow(spear, SEA);
            session.trackEntity(spear);
            spears.add(new Spear(spear, ground, angle));
        }

        session.onCleanup(() -> {
            for (Spear s : spears) {
                DisplayFactory.remove(s.display);
            }
        });
        session.resetDeadline(total + 25L);
        visuals.sound("ITEM_TRIDENT_RETURN", origin, 0.9f, 0.85f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] crushed = {false};
        boolean[] burst = {false};

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                return false;
            }

            if (current < RISE) {
                double t = (current + 1) / (double) RISE;
                for (Spear spear : spears) {
                    double y = -2.5 + t * 3.8;
                    Location at = spear.ground.clone().add(0, y, 0);
                    DisplayFactory.place(spear.display, at, 0.6f, 0.9f, 0.6f, (float) (spear.angle + Math.PI / 2));
                }
                if (current % 5 == 0) {
                    visuals.dust(origin, SEA, 0.8f, 2, 0.2, 0.05, 0.2, 0);
                }
            } else if (current < RISE + CLOSE) {
                int local = current - RISE;
                double t = (local + 1) / (double) CLOSE;
                double cageR = 3.2 * (1.0 - t * 0.72);
                for (Spear spear : spears) {
                    Location at = origin.clone().add(Math.cos(spear.angle) * cageR, 1.5 + t * 0.5, Math.sin(spear.angle) * cageR);
                    float tilt = (float) (spear.angle - Math.PI / 2 - t * 0.4);
                    DisplayFactory.place(spear.display, at, 0.55f, 0.95f, 0.55f, tilt);
                }
                if (local % 4 == 0) {
                    pushInward(session, world, origin, killer, cageR + 0.35, 0.16 + t * 0.12);
                }
                if (local == CLOSE - 1 && !crushed[0]) {
                    crushed[0] = true;
                    visuals.sound("ENTITY_IRON_GOLEM_ATTACK", origin, 0.9f, 1.1f);
                    crush(session, world, origin, killer, cageR + 0.5, crushDamage);
                }
            } else {
                int local = current - RISE - CLOSE;
                if (!burst[0]) {
                    burst[0] = true;
                    visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.7f, 1.2f);
                    visuals.particle("CRIT", origin, ParticleScale.scale(14), 0.5, 0.3, 0.5, 0.02, null);
                }
                double t = (local + 1) / (double) BURST;
                for (Spear spear : spears) {
                    double r = 0.6 + t * 4.5;
                    Location at = origin.clone()
                            .add(Math.cos(spear.angle) * r, 1.2 + t * 1.5, Math.sin(spear.angle) * r);
                    float sc = (float) Math.max(0.05, 1.0 - t);
                    DisplayFactory.place(spear.display, at, 0.5f * sc, 0.85f * sc, 0.5f * sc, (float) spear.angle);
                }
                if (local == 0) {
                    explodeKb(session, world, origin, killer, radius, crushDamage * 0.6, 0.9);
                }
                if (local >= BURST - 1) {
                    for (Spear s : spears) {
                        DisplayFactory.remove(s.display);
                    }
                    spears.clear();
                }
            }
            return true;
        });
    }

    private static void pushInward(
            EffectSession session, World world, Location origin, Player killer, double radius, double speed) {
        double rSq = radius * radius;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead() || killer != null && entity.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (entity.getLocation().distanceSquared(origin) > rSq) {
                continue;
            }
            Vector inward = origin.toVector().subtract(entity.getLocation().toVector());
            inward.setY(0.06);
            if (inward.lengthSquared() > 0.01) {
                inward.normalize().multiply(speed);
            }
            EffectGameplay.velocity(session, entity, inward, killer);
        }
    }

    private static void crush(
            EffectSession session, World world, Location origin, Player killer, double radius, double damage) {
        double rSq = radius * radius;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead() || killer != null && entity.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (entity.getLocation().distanceSquared(origin) > rSq) {
                continue;
            }
            EffectGameplay.damage(session, entity, damage, killer);
        }
    }

    private static void explodeKb(
            EffectSession session,
            World world,
            Location origin,
            Player killer,
            double radius,
            double damage,
            double knock) {
        double rSq = radius * radius;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead() || killer != null && entity.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (entity.getLocation().distanceSquared(origin) > rSq) {
                continue;
            }
            EffectGameplay.damage(session, entity, damage, killer);
            Vector out = entity.getLocation().toVector().subtract(origin.toVector());
            out.setY(0.35);
            if (out.lengthSquared() > 0.01) {
                out.normalize().multiply(knock);
            }
            EffectGameplay.velocity(session, entity, out, killer);
        }
    }

    private static final class Spear {
        private final ItemDisplay display;
        private final Location ground;
        private final double angle;

        private Spear(ItemDisplay display, Location ground, double angle) {
            this.display = display;
            this.ground = ground.clone();
            this.angle = angle;
        }
    }
}