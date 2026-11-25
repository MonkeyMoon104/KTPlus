package com.monkey.ktplus.effects.list.voidimplode.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectGameplay;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.visual.display.DisplayFactory;
import com.monkey.ktplus.effects.visual.display.FxGeometry;
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

public final class VoidImplodeAnimation {
    private static final int ORBIT_TICKS = 55;
    private static final int SUCK_TICKS = 50;
    private static final int POP_TICKS = 18;
    private static final int DEBRIS = 14;
    private static final double MAX_SHELL = 2.65;
    private static final Color VOID = Color.fromRGB(18, 4, 38);
    private static final Color PURPLE = Color.fromRGB(115, 35, 175);

    private VoidImplodeAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.15, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("voidimplode");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int orbitTicks = perks == null ? ORBIT_TICKS : Math.max(35, perks.getInt("orbit-ticks", ORBIT_TICKS));
        int suckTicks = perks == null ? SUCK_TICKS : Math.max(30, perks.getInt("suck-ticks", SUCK_TICKS));
        int spiralTicks = orbitTicks + suckTicks;
        int total = spiralTicks + POP_TICKS;

        EffectDamageConfig damageCfg = context.config().effectDamage("voidimplode");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 8.0;
        double radius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 8.0;
        Player killer = context.killer();

        List<Debris> debris = new ArrayList<>();
        Material[] mats = {Material.ENDER_PEARL, Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.ENDER_EYE};
        for (int i = 0; i < DEBRIS; i++) {
            ItemDisplay display = DisplayFactory.item(origin, mats[i % mats.length], 0.44f, 0.44f, 0.44f);
            if (display == null) {
                continue;
            }
            DisplayFactory.glow(display, PURPLE);
            session.trackEntity(display);
            double phase = (Math.PI * 2.0 * i) / DEBRIS;
            debris.add(new Debris(display, phase, 2.15 + (i % 4) * 0.32, 0.75 + (i % 3) * 0.22));
        }

        session.onCleanup(() -> {
            for (Debris d : debris) {
                DisplayFactory.remove(d.display);
            }
        });
        session.resetDeadline(total + 28L);
        visuals.sound("BLOCK_PORTAL_AMBIENT", origin, 0.75f, 0.62f);
        visuals.sound("ENTITY_ENDERMAN_TELEPORT", origin, 0.55f, 0.55f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] popped = {false};

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total && popped[0]) {
                return false;
            }

            if (current < spiralTicks) {
                double t = (current + 1) / (double) spiralTicks;
                double shrink = easeIn(t);
                double spinRate = 0.085 + shrink * 0.095;
                float shellR = (float) (MAX_SHELL * (1.0 - shrink * 0.92) + 0.15);

                if (current % 2 == 0) {
                    visuals.sphereShell("DUST", origin, shellR, 5, 16, t > 0.55 ? PURPLE : VOID);
                }

                for (Debris d : debris) {
                    double a = d.phase + current * spinRate * (1.0 + t * 0.35);
                    double r = d.radius * (1.0 - shrink * 0.97);
                    double y = d.height * (1.0 - shrink);
                    Location at = origin.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    float scale = (float) (0.44 - shrink * 0.3);
                    DisplayFactory.place(d.display, at, scale, scale, scale, (float) a);
                }

                if (current % 4 == 0) {
                    visuals.dust(origin, PURPLE, 0.6f + (float) t * 0.5f, 4, 0.12, 0.12, 0.12, 0);
                    FxGeometry.helix(origin, 0.35, shellR, 2, 14, at -> visuals.particle("PORTAL", at, 1, 0, 0, 0, 0, null));
                }
                return true;
            }

            if (!popped[0]) {
                popped[0] = true;
                visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.6f, 1.25f);
                visuals.sound("BLOCK_RESPAWN_ANCHOR_DEPLETE", origin, 0.95f, 0.78f);
                visuals.particle("REVERSE_PORTAL", origin, ParticleScale.scale(22), 0.45, 0.45, 0.45, 0.08, null);
                visuals.sphereShell("DRAGON_BREATH", origin, 2.3f, 5, 18, PURPLE);
                for (Debris d : debris) {
                    DisplayFactory.remove(d.display);
                }
                debris.clear();
                popKnockback(session, world, origin, killer, radius, damage);
            }
            return !popped[0] || current < total;
        });
    }

    private static void popKnockback(
            EffectSession session, World world, Location origin, Player killer, double radius, double damage) {
        double rSq = radius * radius;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead() || !entity.getWorld().equals(world)) {
                continue;
            }
            if (killer != null && entity.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (entity.getLocation().distanceSquared(origin) > rSq) {
                continue;
            }
            EffectGameplay.damage(session, entity, damage, killer);
            Vector out = entity.getLocation().toVector().subtract(origin.toVector());
            if (out.lengthSquared() < 0.01) {
                out = new Vector(0, 1, 0);
            } else {
                out.normalize().multiply(1.1);
            }
            out.setY(Math.max(0.45, out.getY() + 0.55));
            EffectGameplay.velocity(session, entity, out, killer);
        }
    }

    private static double easeIn(double t) {
        return t * t;
    }

    private static final class Debris {
        private final ItemDisplay display;
        private final double phase;
        private final double radius;
        private final double height;

        private Debris(ItemDisplay display, double phase, double radius, double height) {
            this.display = display;
            this.phase = phase;
            this.radius = radius;
            this.height = height;
        }
    }
}