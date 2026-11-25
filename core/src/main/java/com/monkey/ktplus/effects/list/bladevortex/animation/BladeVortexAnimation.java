package com.monkey.ktplus.effects.list.bladevortex.animation;

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

public final class BladeVortexAnimation {
    private static final int FORM_TICKS = 15;
    private static final int PULL_TICKS = 35;
    private static final int SLASH_TICKS = 20;
    private static final int FADE_TICKS = 25;
    private static final int TOTAL = FORM_TICKS + PULL_TICKS + SLASH_TICKS + FADE_TICKS;
    private static final int BLADES = 10;
    private static final float TIP_OUT = (float) (Math.PI / 2.0);
    private static final Color STEEL = Color.fromRGB(175, 200, 230);

    private BladeVortexAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("bladevortex");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int total = perks == null ? TOTAL : Math.max(95, Math.min(140, perks.getInt("duration-ticks", TOTAL)));

        EffectDamageConfig damageCfg = context.config().effectDamage("bladevortex");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 4.0;
        double radius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 6.0;
        Player killer = context.killer();

        List<ItemDisplay> blades = new ArrayList<>();
        for (int i = 0; i < BLADES; i++) {
            ItemDisplay blade = DisplayFactory.item(origin, Material.DIAMOND_SWORD, 0.55f, 0.55f, 0.55f);
            if (blade == null) {
                continue;
            }
            DisplayFactory.glow(blade, STEEL);
            session.trackEntity(blade);
            blades.add(blade);
        }

        session.onCleanup(() -> {
            for (ItemDisplay b : blades) {
                DisplayFactory.remove(b);
            }
        });
        session.resetDeadline(total + 20L);
        visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", origin, 1.0f, 0.8f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] slashed = {false};
        int pullStart = FORM_TICKS;
        int slashStart = pullStart + PULL_TICKS;
        int fadeStart = slashStart + SLASH_TICKS;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                return false;
            }

            if (current < pullStart) {
                double t = (current + 1) / (double) FORM_TICKS;
                for (int i = 0; i < blades.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / blades.size();
                    double r = 4.2 - t * 1.6;
                    Location at = origin.clone().add(Math.cos(a) * r, 2.2 - t * 0.8, Math.sin(a) * r);
                    float sc = 0.35f + (float) t * 0.22f;
                    DisplayFactory.place(blades.get(i), at, sc, sc, sc, (float) (a + Math.PI / 2), TIP_OUT);
                }
                if (current % 5 == 0) {
                    visuals.dust(origin.clone().add(0, 1.8, 0), STEEL, 0.7f, 2, 0.15, 0.1, 0.15, 0);
                }
                return true;
            }

            if (current < slashStart) {
                int local = current - pullStart;
                double t = (local + 1) / (double) PULL_TICKS;
                double spin = current * 0.22;
                double r = 2.6 * (1.0 - easeIn(t) * 0.62);
                for (int i = 0; i < blades.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / blades.size() + spin;
                    double y = 0.55 + Math.sin(a * 2 + current * 0.12) * 0.4;
                    Location at = origin.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    DisplayFactory.place(blades.get(i), at, 0.54f, 0.54f, 0.54f, (float) (a + Math.PI / 2), TIP_OUT);
                }
                if (local % 2 == 0) {
                    pullIn(session, world, origin, killer, radius, 0.09 + t * 0.06);
                    visuals.dust(origin, STEEL, 0.85f + (float) t * 0.4f, 3, 0.22, 0.12, 0.22, 0);
                }
                if (local == 0) {
                    visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", origin, 1.05f, 1.15f);
                }
                return true;
            }

            if (current < fadeStart) {
                int local = current - slashStart;
                double burst = (local + 1) / (double) SLASH_TICKS;
                double spin = current * 0.28;
                double r = 1.1 + Math.sin(burst * Math.PI) * 0.35;
                for (int i = 0; i < blades.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / blades.size() + spin;
                    Location at = origin.clone().add(Math.cos(a) * r, 0.9 + burst * 0.5, Math.sin(a) * r);
                    DisplayFactory.place(blades.get(i), at, 0.58f, 0.58f, 0.58f, (float) (a + Math.PI / 2), TIP_OUT);
                }
                if (local == 0 && !slashed[0]) {
                    slashed[0] = true;
                    visuals.sound("ENTITY_PLAYER_ATTACK_CRIT", origin, 1.25f, 0.88f);
                    visuals.particle("SWEEP_ATTACK", origin, 3, 0, 0, 0, 0, null);
                    visuals.particle("CRIT", origin, ParticleScale.scale(20), 0.45, 0.2, 0.45, 0.02, null);
                    slashNearby(session, world, origin, killer, radius * 0.9, damage, 0.85);
                }
                if (local % 3 == 0) {
                    visuals.circle("SWEEP_ATTACK", origin, (float) (radius * 0.45), 0.1, 16, null);
                }
                return true;
            }

            int local = current - fadeStart;
            double fadeT = (local + 1) / (double) FADE_TICKS;
            float fade = (float) Math.max(0.08, 1.0 - fadeT);
            double spin = current * 0.1;
            for (int i = 0; i < blades.size(); i++) {
                double a = (Math.PI * 2.0 * i) / blades.size() + spin;
                double r = 1.8 + fadeT * 2.2;
                Location at = origin.clone().add(Math.cos(a) * r, 1.2 + fadeT * 0.8, Math.sin(a) * r);
                DisplayFactory.place(blades.get(i), at, 0.5f * fade, 0.5f * fade, 0.5f * fade, (float) (a + Math.PI / 2), TIP_OUT);
            }
            if (local == FADE_TICKS - 1) {
                for (ItemDisplay b : blades) {
                    DisplayFactory.remove(b);
                }
                blades.clear();
            }
            return local < FADE_TICKS;
        });
    }

    private static void pullIn(
            EffectSession session, World world, Location origin, Player killer, double radius, double pull) {
        double rSq = radius * radius;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead() || killer != null && entity.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (entity.getLocation().distanceSquared(origin) > rSq) {
                continue;
            }
            Vector v = origin.toVector().subtract(entity.getLocation().toVector());
            v.setY(0);
            if (v.lengthSquared() > 0.01) {
                v.normalize().multiply(pull);
                EffectGameplay.velocity(session, entity, v, killer);
            }
        }
    }

    private static void slashNearby(
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
            Vector away = entity.getLocation().toVector().subtract(origin.toVector());
            away.setY(0.2);
            if (away.lengthSquared() > 0.01) {
                away.normalize().multiply(knock);
            }
            EffectGameplay.velocity(session, entity, away, killer);
        }
    }

    private static double easeIn(double t) {
        return t * t;
    }
}