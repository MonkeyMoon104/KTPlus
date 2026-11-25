package com.monkey.ktplus.effects.list.swordrain.animation;

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
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SwordRainAnimation {
    private static final int SWORD_COUNT = 20;
    private static final int RISE_TICKS = 26;
    private static final int BURST_HOLD = 4;
    private static final int FALL_PER_SWORD = 13;
    private static final int STAGGER = 2;
    private static final int STICK_TICKS = 28;
    private static final int SINK_TICKS = 24;
    private static final double SKY = 15.5;
    private static final double PLANT_CENTER_OFFSET = 0.22;
    private static final float TIP_DOWN = (float) Math.PI;
    private static final float TIP_UP = 0.0f;
    private static final Color STEEL = Color.fromRGB(170, 195, 225);
    private static final Color CRIT = Color.fromRGB(255, 245, 200);
    private static final Color SIGNAL = Color.fromRGB(120, 200, 255);

    private SwordRainAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.08, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("swordrain");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int swordCount = perks == null ? SWORD_COUNT : Math.max(12, Math.min(28, perks.getInt("sword-count", SWORD_COUNT)));
        int fallTicks = perks == null ? FALL_PER_SWORD : Math.max(8, Math.min(24, perks.getInt("fall-ticks", FALL_PER_SWORD)));
        int stickTicks = perks == null ? STICK_TICKS : Math.max(30, perks.getInt("hold-ticks", STICK_TICKS));
        int stagger = perks == null ? STAGGER : Math.max(1, Math.min(5, perks.getInt("stagger-ticks", STAGGER)));

        EffectDamageConfig damageCfg = context.config().effectDamage("swordrain");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 3.0;
        double radius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 7.5;
        double knock = 0.55;
        Player killer = context.killer();

        int rainStart = RISE_TICKS + BURST_HOLD;

        ItemDisplay signal = DisplayFactory.item(origin.clone().add(0, 0.2, 0), Material.NETHERITE_SWORD, 0.95f, 0.95f, 0.95f);
        if (signal != null) {
            signal.setGlowing(false);
            signal.setInterpolationDuration(0);
            signal.setTeleportDuration(0);
            session.trackEntity(signal);
            placeStill(signal, origin.clone().add(0, 0.25, 0), 0.0f, TIP_UP, 0.95f);
        }

        List<FallingSword> swords = new ArrayList<>();
        Material[] blades = {Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.IRON_SWORD};
        for (int i = 0; i < swordCount; i++) {
            double angle = (Math.PI * 2.0 * i) / swordCount + Math.random() * 0.2;
            int band = i % 3;
            double dist;
            if (band == 0) {
                dist = 1.7 + (i % 3) * 0.3 + Math.random() * 0.4;
            } else if (band == 1) {
                dist = 3.0 + (i % 4) * 0.35 + Math.random() * 0.5;
            } else {
                dist = 4.2 + (i % 4) * 0.4 + Math.random() * 0.65;
            }
            double rawX = origin.getX() + Math.cos(angle) * dist;
            double rawZ = origin.getZ() + Math.sin(angle) * dist;
            Location ground = resolveSolidSurface(world, rawX, origin.getY(), rawZ);
            double plantY = ground.getY() + PLANT_CENTER_OFFSET;
            double startY = ground.getY() + SKY;
            Location spawn = new Location(world, ground.getX(), startY, ground.getZ());
            ItemDisplay display = DisplayFactory.item(spawn, blades[i % blades.length], 0.75f, 0.75f, 0.75f);
            if (display == null) {
                continue;
            }
            display.setGlowing(false);
            display.setInterpolationDuration(0);
            display.setTeleportDuration(0);
            placeStill(display, spawn, (float) angle, TIP_DOWN, 0.01f);
            session.trackEntity(display);
            int delay = rainStart + i * stagger;
            swords.add(new FallingSword(display, ground, startY, plantY, delay, (float) angle));
        }

        int lastLand = rainStart;
        for (FallingSword sword : swords) {
            lastLand = Math.max(lastLand, sword.delay + fallTicks);
        }
        int stickStart = lastLand;
        int sinkStart = stickStart + stickTicks;
        int total = sinkStart + SINK_TICKS;

        session.onCleanup(() -> {
            DisplayFactory.remove(signal);
            for (FallingSword s : swords) {
                DisplayFactory.remove(s.display);
            }
        });
        session.resetDeadline(total + 30L);
        visuals.sound("ITEM_TRIDENT_THROW", origin, 0.9f, 1.15f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] burstDone = {false};
        boolean[] shockwaveDone = {false};

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                return false;
            }

            if (current < RISE_TICKS) {
                double t = (current + 1) / (double) RISE_TICKS;
                double y = 0.25 + t * (SKY * 0.55);
                Location at = origin.clone().add(0, y, 0);
                placeStill(signal, at, current * 0.08f, TIP_UP, 0.95f);
                visuals.dust(at.clone().add(0, -0.25, 0), SIGNAL, 1.2f, 4, 0.05, 0.18, 0.05, 0);
                visuals.dust(at.clone().add(0, -0.55, 0), STEEL, 0.95f, 2, 0.04, 0.12, 0.04, 0);
                visuals.particle("END_ROD", at.clone().add(0, -0.4, 0), 2, 0.03, 0.12, 0.03, 0.01, null);
                if (current % 6 == 0) {
                    visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", at, 0.35f, 1.3f + (float) t * 0.3f);
                }
                return true;
            }

            if (current < rainStart) {
                if (!burstDone[0]) {
                    burstDone[0] = true;
                    Location apex = origin.clone().add(0, SKY * 0.55, 0);
                    DisplayFactory.remove(signal);
                    visuals.sound("ENTITY_GENERIC_EXPLODE", apex, 0.75f, 1.15f);
                    visuals.sound("ITEM_TRIDENT_THUNDER", apex, 0.85f, 1.25f);
                    visuals.sound("ENTITY_PLAYER_ATTACK_CRIT", apex, 1.0f, 0.9f);
                    visuals.particle("FLASH", apex, 2, 0, 0, 0, 0, null);
                    visuals.particle("CRIT", apex, ParticleScale.scale(28), 0.55, 0.45, 0.55, 0.08, null);
                    visuals.particle("SWEEP_ATTACK", apex, 10, 0.6, 0.3, 0.6, 0.02, null);
                    visuals.dust(apex, CRIT, 1.5f, ParticleScale.scale(14), 0.5, 0.35, 0.5, 0);
                    visuals.sphereShell("END_ROD", apex, 2.2, 4, 14, SIGNAL);
                }
                return true;
            }

            if (current < stickStart) {
                for (FallingSword sword : swords) {
                    if (sword.removed || sword.planted) {
                        continue;
                    }
                    if (current < sword.delay) {
                        continue;
                    }
                    int local = current - sword.delay;
                    if (local == 0) {
                        Location top = sword.ground.clone();
                        top.setY(sword.startY);
                        placeStill(sword.display, top, sword.yaw, TIP_DOWN, 0.75f);
                    }
                    if (local >= fallTicks) {
                        plantSwordStill(sword);
                        visuals.sound("ITEM_TRIDENT_HIT", sword.ground, 0.4f, 0.85f + (float) Math.random() * 0.25f);
                        visuals.particle("CRIT", sword.ground.clone().add(0, 0.12, 0), 4, 0.08, 0.04, 0.08, 0.01, null);
                        continue;
                    }
                    double t = local / (double) fallTicks;
                    double eased = t * t;
                    double y = sword.startY - (sword.startY - sword.plantY) * eased;
                    Location at = sword.ground.clone();
                    at.setY(y);
                    placeStill(sword.display, at, sword.yaw, TIP_DOWN, 0.75f);
                    drawFallTrail(visuals, sword, y);
                    sword.lastTrailY = y;
                }

                cullUnsupported(swords, visuals);

                if (!shockwaveDone[0] && current >= stickStart - 1) {
                    shockwaveDone[0] = true;
                    visuals.sound("ITEM_TRIDENT_THUNDER", origin, 0.8f, 1.05f);
                    visuals.sound("BLOCK_ANVIL_LAND", origin, 0.7f, 0.95f);
                    visuals.circle("SWEEP_ATTACK", origin, (float) radius, 0.12, 26, null);
                    visuals.particle("CRIT", origin.clone().add(0, 0.25, 0), ParticleScale.scale(14), 0.45, 0.12, 0.45, 0.02, null);
                    shockwaveEntities(session, world, origin, killer, radius, damage, knock);
                    for (FallingSword sword : swords) {
                        if (!sword.removed && !sword.planted) {
                            plantSwordStill(sword);
                        }
                    }
                }
                return true;
            }

            if (current < sinkStart) {
                if (current == stickStart) {
                    for (FallingSword sword : swords) {
                        if (!sword.removed) {
                            plantSwordStill(sword);
                        }
                    }
                }
                cullUnsupported(swords, visuals);
                return true;
            }

            cullUnsupported(swords, visuals);

            int sink = current - sinkStart;
            double sinkProgress = (sink + 1) / (double) SINK_TICKS;
            float fade = (float) Math.max(0.08, 1.0 - sinkProgress);
            for (FallingSword sword : swords) {
                if (sword.removed) {
                    continue;
                }
                Location planted = sword.ground.clone();
                planted.setY(sword.plantY - sinkProgress * 0.45);
                placeStill(sword.display, planted, sword.yaw, TIP_DOWN, 0.7f * fade);
            }
            if (sink == SINK_TICKS - 1) {
                for (FallingSword s : swords) {
                    DisplayFactory.remove(s.display);
                }
                swords.clear();
                visuals.sound("BLOCK_GRAVEL_BREAK", origin, 0.6f, 0.9f);
            }
            return sink < SINK_TICKS;
        });
    }

    private static void cullUnsupported(List<FallingSword> swords, VisualEffectService visuals) {
        for (FallingSword sword : swords) {
            if (sword.removed || !sword.planted) {
                continue;
            }
            if (supportStillSolid(sword)) {
                continue;
            }
            despawnSword(sword, visuals);
        }
    }

    private static boolean supportStillSolid(FallingSword sword) {
        World world = sword.ground.getWorld();
        if (world == null) {
            return false;
        }
        Block support = world.getBlockAt(sword.supportX, sword.supportY, sword.supportZ);
        return isSolid(support.getType());
    }

    private static void despawnSword(FallingSword sword, VisualEffectService visuals) {
        sword.removed = true;
        Location at = sword.ground.clone().add(0.0, 0.25, 0.0);
        visuals.sound("ENTITY_ITEM_BREAK", at, 0.55f, 0.85f + (float) Math.random() * 0.2f);
        visuals.particle("CRIT", at, 8, 0.12, 0.18, 0.12, 0.02, null);
        visuals.dust(at, STEEL, 1.0f, 4, 0.1, 0.15, 0.1, 0);
        DisplayFactory.remove(sword.display);
    }

    private static Location resolveSolidSurface(World world, double x, double aroundY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int start = Math.min(world.getMaxHeight() - 2, (int) Math.floor(aroundY) + 8);
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(aroundY) - 16);
        for (int y = start; y >= minY; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (isSolid(block.getType())) {
                return new Location(world, x, y + 1.0, z);
            }
        }
        return new Location(world, x, aroundY, z);
    }

    private static boolean isSolid(Material material) {
        if (material == null || material.isAir()) {
            return false;
        }
        try {
            return material.isSolid();
        } catch (NoSuchMethodError ignored) {
            String name = material.name();
            return !name.contains("WATER")
                    && !name.contains("LAVA")
                    && !name.contains("SAPLING")
                    && !name.contains("FLOWER")
                    && !name.contains("TORCH")
                    && !name.contains("RAIL")
                    && !name.contains("CARPET")
                    && !name.contains("PRESSURE")
                    && !name.contains("BUTTON")
                    && !name.contains("SIGN")
                    && !name.contains("BANNER")
                    && !name.contains("SNOW")
                    && material != Material.SHORT_GRASS
                    && material != Material.TALL_GRASS;
        }
    }

    private static void drawFallTrail(VisualEffectService visuals, FallingSword sword, double currentY) {
        double fromY = sword.lastTrailY > 0.0 ? sword.lastTrailY : currentY + 0.9;
        if (fromY < currentY) {
            fromY = currentY + 0.9;
        }
        int steps = Math.max(3, (int) Math.ceil(Math.abs(fromY - currentY) * 3.5));
        for (int i = 0; i <= steps; i++) {
            double lerp = i / (double) steps;
            double y = fromY + (currentY - fromY) * lerp;
            Location point = sword.ground.clone();
            point.setY(y + 0.35);
            visuals.dust(point, STEEL, 1.15f, 2, 0.02, 0.04, 0.02, 0);
            visuals.dust(point, SIGNAL, 0.85f, 1, 0.015, 0.03, 0.015, 0);
            if (i % 2 == 0) {
                visuals.particle("END_ROD", point, 1, 0.01, 0.02, 0.01, 0.0, null);
            }
        }
        Location tip = sword.ground.clone();
        tip.setY(currentY + 0.55);
        visuals.particle("CRIT", tip, 3, 0.04, 0.1, 0.04, 0.02, null);
    }

    private static void plantSwordStill(FallingSword sword) {
        if (sword.removed) {
            return;
        }
        Location planted = sword.ground.clone();
        planted.setY(sword.plantY);
        placeStill(sword.display, planted, sword.yaw, TIP_DOWN, 0.75f);
        sword.planted = true;
    }

    private static void placeStill(ItemDisplay display, Location at, float yaw, float pitch, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        display.setInterpolationDuration(0);
        display.setTeleportDuration(0);
        DisplayFactory.place(display, at, scale, scale, scale, yaw, pitch);
        display.setInterpolationDuration(0);
        display.setTeleportDuration(0);
    }

    private static void shockwaveEntities(
            EffectSession session,
            World world,
            Location origin,
            Player killer,
            double radius,
            double damage,
            double knock) {
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
            Vector push = entity.getLocation().toVector().subtract(origin.toVector());
            push.setY(0);
            if (push.lengthSquared() < 0.01) {
                push = new Vector(0, 0, 1);
            } else {
                push.normalize().multiply(knock);
            }
            push.setY(0.28);
            EffectGameplay.velocity(session, entity, push, killer);
        }
    }

    private static final class FallingSword {
        private final ItemDisplay display;
        private final Location ground;
        private final double startY;
        private final double plantY;
        private final int delay;
        private final float yaw;
        private final int supportX;
        private final int supportY;
        private final int supportZ;
        private boolean planted;
        private boolean removed;
        private double lastTrailY;

        private FallingSword(
                ItemDisplay display, Location ground, double startY, double plantY, int delay, float yaw) {
            this.display = display;
            this.ground = ground.clone();
            this.startY = startY;
            this.plantY = plantY;
            this.delay = delay;
            this.yaw = yaw;
            this.supportX = ground.getBlockX();
            this.supportY = (int) Math.floor(ground.getY()) - 1;
            this.supportZ = ground.getBlockZ();
            this.lastTrailY = startY;
        }
    }
}
