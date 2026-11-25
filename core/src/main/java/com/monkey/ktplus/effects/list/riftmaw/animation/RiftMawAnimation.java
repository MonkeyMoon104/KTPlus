package com.monkey.ktplus.effects.list.riftmaw.animation;

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
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class RiftMawAnimation {
    private static final int OPEN = 50;
    private static final int PULL = 60;
    private static final int SPIT = 35;
    private static final Color VOID = Color.fromRGB(30, 10, 50);
    private static final Color RIFT = Color.fromRGB(130, 50, 200);

    private RiftMawAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.02, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        EffectDamageConfig damageCfg = context.config().effectDamage("riftmaw");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 6.0;
        double radius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 7.0;
        Player killer = context.killer();
        int total = OPEN + PULL + SPIT;

        List<BlockDisplay> frame = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double a = (Math.PI * 2.0 * i) / 8.0;
            BlockDisplay seg = DisplayFactory.block(
                    origin.clone().add(Math.cos(a) * 1.5, -0.5, Math.sin(a) * 1.5),
                    Material.END_PORTAL_FRAME,
                    0.32f,
                    0.2f,
                    0.32f);
            if (seg == null) {
                continue;
            }
            DisplayFactory.glow(seg, RIFT);
            session.trackEntity(seg);
            frame.add(seg);
        }
        BlockDisplay maw = DisplayFactory.block(origin.clone().add(0, -0.3, 0), Material.OBSIDIAN, 2.2f, 0.06f, 2.2f);
        if (maw != null) {
            DisplayFactory.glow(maw, VOID);
            session.trackEntity(maw);
        }

        session.onCleanup(() -> {
            DisplayFactory.remove(maw);
            for (BlockDisplay b : frame) {
                DisplayFactory.remove(b);
            }
        });
        session.resetDeadline(total + 25L);
        visuals.sound("BLOCK_END_PORTAL_SPAWN", origin, 0.8f, 0.75f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] spat = {false};

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                return false;
            }

            if (current < OPEN) {
                double t = (current + 1) / (double) OPEN;
                float scale = 0.3f + (float) t * 1.9f;
                DisplayFactory.place(maw, origin.clone().add(0, -0.25 + t * 0.05, 0), scale, 0.08f, scale, current * 0.08f);
                for (int i = 0; i < frame.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / frame.size() + current * 0.05;
                    double r = 0.5 + t * 1.4;
                    Location at = origin.clone().add(Math.cos(a) * r, -0.35, Math.sin(a) * r);
                    DisplayFactory.place(frame.get(i), at, 0.35f, 0.25f, 0.35f, (float) a);
                }
                if (current % 4 == 0) {
                    visuals.particle("PORTAL", origin.clone().add(0, 0.1, 0), 6, 0.4, 0.05, 0.4, 0.02, null);
                }
            } else if (current < OPEN + PULL) {
                int local = current - OPEN;
                double t = (local + 1) / (double) PULL;
                DisplayFactory.place(maw, origin.clone().add(0, -0.2 - t * 0.15, 0), 2.0f, 0.1f, 2.0f, 0);
                visuals.dust(origin, VOID, 1.2f, 4, 0.25, 0.1, 0.25, 0);
                pullDown(session, world, origin, killer, radius, 0.14 + t * 0.08);
                FxGeometry.helix(origin, 0.5, -1.5 * t, 2, 12, at -> visuals.particle("REVERSE_PORTAL", at, 1, 0, 0, 0, 0, null));
            } else {
                int local = current - OPEN - PULL;
                if (!spat[0]) {
                    spat[0] = true;
                    visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.65f, 1.15f);
                    visuals.particle("REVERSE_PORTAL", origin.clone().add(0, 0.5, 0), ParticleScale.scale(18), 0.5, 0.4, 0.5, 0.06, null);
                    spitEntities(session, world, origin, killer, radius, damage);
                }
                double t = (local + 1) / (double) SPIT;
                float shrink = (float) (1.0 - t);
                DisplayFactory.place(maw, origin.clone().add(0, 0.2 + t * 2.0, 0), 2.0f * shrink, 0.08f, 2.0f * shrink, 0);
                for (BlockDisplay b : frame) {
                    DisplayFactory.scale(b, 0.3f * shrink, 0.2f * shrink, 0.3f * shrink);
                }
                if (local >= SPIT - 1) {
                    DisplayFactory.remove(maw);
                    for (BlockDisplay b : frame) {
                        DisplayFactory.remove(b);
                    }
                    frame.clear();
                }
            }
            return true;
        });
    }

    private static void pullDown(
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
            v.setY(-Math.abs(v.getY()) - pull);
            if (v.length() > 0.5) {
                v.normalize().multiply(pull);
            }
            EffectGameplay.velocity(session, entity, v, killer);
        }
    }

    private static void spitEntities(
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
            Vector up = entity.getLocation().toVector().subtract(origin.toVector());
            up.setY(1.1);
            if (up.lengthSquared() > 0.01) {
                up.normalize().multiply(1.2);
            }
            EffectGameplay.velocity(session, entity, up, killer);
        }
    }
}