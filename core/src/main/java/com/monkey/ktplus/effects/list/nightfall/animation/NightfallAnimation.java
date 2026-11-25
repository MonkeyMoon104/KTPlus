package com.monkey.ktplus.effects.list.nightfall.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectGameplay;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.visual.display.DisplayFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class NightfallAnimation {
    private static final int FALL_TICKS = 28;
    private static final int NOVA_TICKS = 32;
    private static final int AFTER_TICKS = 20;
    private static final int TOTAL = FALL_TICKS + NOVA_TICKS + AFTER_TICKS;
    private static final double START_Y = 16.0;
    private static final double IMPACT_Y = 1.1;
    private static final float SCALE = 4.25f;
    private static final float SPIN_PER_TICK = 0.42f;
    private static final Color VOID = Color.fromRGB(8, 8, 12);
    private static final Color CRY = Color.fromRGB(90, 40, 160);
    private static final Color CORONA = Color.fromRGB(255, 190, 80);
    private static final Color NOVA = Color.fromRGB(255, 250, 235);
    private static final Color TEAR = Color.fromRGB(180, 120, 255);

    private NightfallAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        EffectDamageConfig damageCfg = context.config().effectDamage("nightfall");
        double novaDamage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 10.0;
        double radius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 9.0;
        Player killer = context.killer();

        Location spawnAt = origin.clone().add(0, START_Y, 0);
        ItemDisplay monolith = DisplayFactory.item(
                spawnAt,
                new ItemStack(Material.OBSIDIAN),
                SCALE,
                SCALE,
                SCALE,
                display -> {
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
                    display.setViewRange(160.0f);
                    display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                    display.setShadowRadius(1.8f);
                    display.setShadowStrength(0.9f);
                    display.setGlowing(false);
                    display.setInterpolationDuration(0);
                    display.setTeleportDuration(0);
                });
        if (monolith == null) {
            return;
        }
        session.trackEntity(monolith);
        placeFalling(monolith, origin, START_Y, 0.0f);

        session.onCleanup(() -> DisplayFactory.remove(monolith));
        session.resetDeadline(TOTAL + 25L);
        visuals.sound("AMBIENT_CAVE", origin, 0.75f, 0.5f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 0.7f, 0.5f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] novaFired = {false};

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL) {
                return false;
            }

            int novaStart = FALL_TICKS;
            int afterStart = novaStart + NOVA_TICKS;

            if (current < novaStart) {
                double t = (current + 1) / (double) FALL_TICKS;
                
                double eased = t * t * t;
                double y = START_Y + (IMPACT_Y - START_Y) * eased;
                float yaw = current * SPIN_PER_TICK;
                placeFalling(monolith, origin, y, yaw);

                boolean crying = (current % 2) == 0;
                if (monolith.isValid() && !monolith.isDead()) {
                    monolith.setItemStack(new ItemStack(crying ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN));
                }

                Location mid = origin.clone().add(0, y, 0);
                if (current % 2 == 0) {
                    visuals.dust(mid, VOID, 1.3f, 4, 0.45, 0.25, 0.45, 0);
                    visuals.dust(mid, CRY, 1.0f, 3, 0.35, 0.2, 0.35, 0);
                    visuals.particle("SMOKE", mid, 3, 0.35, 0.2, 0.35, 0.01, null);
                }
                if (crying && current % 3 == 0) {
                    visuals.particle("DRIPPING_OBSIDIAN_TEAR", mid, 2, 0.25, 0.15, 0.25, 0.0, null);
                }
                if (current % 5 == 0) {
                    visuals.sound("BLOCK_STONE_BREAK", mid, 0.4f, 0.45f + (float) t * 0.45f);
                }

                if (current == FALL_TICKS - 1) {
                    placeFalling(monolith, origin, IMPACT_Y, yaw);
                    DisplayFactory.remove(monolith);
                    if (!novaFired[0]) {
                        novaFired[0] = true;
                        playImpactBurst(visuals, origin);
                        novaBurst(session, world, origin, killer, radius, novaDamage);
                    }
                }
                return true;
            }

            if (current < afterStart) {
                int local = current - novaStart;
                double t = (local + 1) / (double) NOVA_TICKS;
                float ring = (float) (2.0 + t * 6.2);
                Location lift = origin.clone().add(0, 1.35, 0);
                visuals.sphereShell("DUST", lift, ring, 5, 18, NOVA);
                visuals.sphereShell("DUST", lift, ring * 0.72f, 3, 14, TEAR);
                visuals.circle("END_ROD", origin, ring, 0.25, 22, CORONA);
                if (local % 3 == 0) {
                    visuals.circle("SOUL_FIRE_FLAME", origin.clone().add(0, 0.4, 0), ring * 0.85f, 0.05, 16, null);
                }
                if (local % 4 == 0) {
                    visuals.particle("DRIPPING_OBSIDIAN_TEAR", lift, 6, ring * 0.35, 0.2, ring * 0.35, 0.0, null);
                }
                if (local == 0 || local == 8 || local == 16) {
                    visuals.particle("FLASH", lift, 1, 0, 0, 0, 0, null);
                }
                return true;
            }

            int local = current - afterStart;
            double t = (local + 1) / (double) AFTER_TICKS;
            visuals.sphereShell("DUST", origin.clone().add(0, 1.0, 0), (float) (2.5 + t * 4.5), 3, 12, VOID);
            visuals.dust(origin.clone().add(0, 0.6, 0), CRY, 1.2f, 4, 0.8, 0.25, 0.8, 0);
            return local < AFTER_TICKS;
        });
    }

    private static void playImpactBurst(VisualEffectService visuals, Location origin) {
        Location core = origin.clone().add(0, 1.25, 0);
        visuals.sound("BLOCK_ANVIL_LAND", origin, 1.05f, 0.5f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 1.0f, 0.65f);
        visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", origin, 0.95f, 1.25f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_DEPLETE", origin, 0.9f, 0.7f);
        visuals.sound("BLOCK_GLASS_BREAK", origin, 0.75f, 0.55f);

        visuals.particle("FLASH", core, 5, 0.15, 0.1, 0.15, 0, null);
        visuals.particle("EXPLOSION_EMITTER", core, 1, 0, 0, 0, 0, null);
        visuals.particle(
                "BLOCK",
                core,
                ParticleScale.scale(36),
                0.85,
                0.55,
                0.85,
                0.08,
                null,
                Material.OBSIDIAN.createBlockData());
        visuals.particle(
                "BLOCK",
                core,
                ParticleScale.scale(22),
                0.7,
                0.45,
                0.7,
                0.06,
                null,
                Material.CRYING_OBSIDIAN.createBlockData());
        visuals.particle("END_ROD", core, ParticleScale.scale(36), 0.7, 0.55, 0.7, 0.08, null);
        visuals.particle("FIREWORK", core, ParticleScale.scale(28), 0.65, 0.65, 0.65, 0.08, null);
        visuals.particle("SOUL", core, ParticleScale.scale(18), 0.5, 0.4, 0.5, 0.03, null);
        visuals.particle("DRIPPING_OBSIDIAN_TEAR", core, ParticleScale.scale(20), 0.6, 0.35, 0.6, 0.0, null);
        visuals.dust(core, NOVA, 1.8f, ParticleScale.scale(16), 0.55, 0.4, 0.55, 0);
        visuals.dust(core, TEAR, 1.5f, ParticleScale.scale(12), 0.5, 0.35, 0.5, 0);
        visuals.sphereShell("END_ROD", core, 2.8, 5, 16, CORONA);
        visuals.circle("CRIT", origin.clone().add(0, 0.3, 0), 3.2f, 0.08, 28, CORONA);
    }

    private static void placeFalling(ItemDisplay display, Location origin, double yOffset, float yawRad) {
        if (display == null || !display.isValid() || display.isDead()) {
            return;
        }
        Location at = origin.clone().add(0.0, yOffset, 0.0);
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        display.setInterpolationDuration(0);
        display.setTeleportDuration(0);
        DisplayFactory.place(display, at, SCALE, SCALE, SCALE, yawRad, 0.0f);
        display.setInterpolationDuration(0);
        display.setTeleportDuration(0);
    }

    private static void novaBurst(
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
            Vector out = entity.getLocation().toVector().subtract(origin.toVector());
            out.setY(0.55);
            if (out.lengthSquared() > 0.01) {
                out.normalize().multiply(1.15);
            }
            EffectGameplay.velocity(session, entity, out, killer);
        }
    }
}
