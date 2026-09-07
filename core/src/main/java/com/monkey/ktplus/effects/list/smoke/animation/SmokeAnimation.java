package com.monkey.ktplus.effects.list.smoke.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class SmokeAnimation {
    private static final int DEFAULT_BUILD_TICKS = 80;
    private static final int DEFAULT_ERUPT_TICKS = 28;
    private static final int DEFAULT_MAX_CHAIN_DEPTH = 2;
    private static final int SPLASH_JETS = 10;
    private static final double DEFAULT_CRATER_RADIUS = 2.4;
    private static final double PUFF_MAX_HEIGHT = 16.0;
    private static final double ERUPT_SPLASH_DAMAGE_RADIUS = 9.0;
    
    private static final Color SMOKE_DARK = Color.fromRGB(95, 95, 100);
    private static final Color SMOKE_MID = Color.fromRGB(150, 150, 155);
    private static final Color SMOKE_LIGHT = Color.fromRGB(210, 210, 215);

    private SmokeAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location ground = context.location().clone().add(0.5, 0.05, 0.5);
        if (ground.getWorld() == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("smoke");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        boolean chain = perks == null || perks.getBoolean("chain", true);
        int maxDepth = perks == null
                ? DEFAULT_MAX_CHAIN_DEPTH
                : Math.max(0, perks.getInt("max-chain-depth", DEFAULT_MAX_CHAIN_DEPTH));
        int buildTicks = perks == null ? DEFAULT_BUILD_TICKS : perks.getInt("build-ticks", DEFAULT_BUILD_TICKS);
        int eruptTicks = perks == null ? DEFAULT_ERUPT_TICKS : perks.getInt("erupt-ticks", DEFAULT_ERUPT_TICKS);
        double craterRadius = perks == null
                ? DEFAULT_CRATER_RADIUS
                : perks.getDouble("crater-radius", DEFAULT_CRATER_RADIUS);

        Player killer = context.killer();
        EffectDamageConfig damage = context.config().effectDamage("smoke");
        Set<UUID> alreadyHit = new HashSet<>();
        AtomicInteger activeEruptions = new AtomicInteger();

        session.onCleanup(() -> {
            PerkActionBar.clear(killer);
            stopBuildAmbient(ground);
        });
        session.runTimer(0L, 5L, () -> {
            if (!session.active() || killer == null || !killer.isOnline()) {
                PerkActionBar.clear(killer);
                return false;
            }
            if (activeEruptions.get() <= 0) {
                return true;
            }
            PerkActionBar.show(
                    killer,
                    String.format(
                            "&8🌋 SMOKE &8| &cERUPTIONS &f%d &8| &6CHAIN &f%s",
                            activeEruptions.get(),
                            chain ? "ON" : "OFF"));
            return true;
        });

        erupt(
                session,
                visuals,
                killer,
                ground,
                damage,
                craterRadius,
                buildTicks,
                eruptTicks,
                chain,
                maxDepth,
                0,
                alreadyHit,
                activeEruptions);
    }

    private static void erupt(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Location center,
            EffectDamageConfig damage,
            double craterRadius,
            int buildTicks,
            int eruptTicks,
            boolean chain,
            int maxDepth,
            int depth,
            Set<UUID> alreadyHit,
            AtomicInteger activeEruptions) {
        World world = center.getWorld();
        if (world == null || !session.active()) {
            return;
        }

        activeEruptions.incrementAndGet();
        AtomicInteger tick = new AtomicInteger();
        boolean[] exploded = {false};
        int total = Math.max(1, buildTicks + eruptTicks);
        List<SmokePuff> puffs = new ArrayList<>();

        stopBuildAmbient(center);
        playBuildAmbient(visuals, center);

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                stopBuildAmbient(center);
                activeEruptions.decrementAndGet();
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                stopBuildAmbient(center);
                activeEruptions.decrementAndGet();
                if (activeEruptions.get() <= 0) {
                    PerkActionBar.clear(killer);
                }
                return false;
            }

            if (current < buildTicks) {
                double progress = (current + 1) / (double) Math.max(1, buildTicks);
                spawnCraterRim(visuals, center, craterRadius, progress, current);
                spawnRisingPuffs(visuals, center, craterRadius, puffs, progress);
                if (current > 0 && current % 12 == 0) {
                    playBuildAmbient(visuals, center);
                }
                return true;
            }

            int eruptLocal = current - buildTicks;
            if (!exploded[0]) {
                exploded[0] = true;
                stopBuildAmbient(center);
                explode(
                        session,
                        visuals,
                        killer,
                        center,
                        damage,
                        craterRadius,
                        buildTicks,
                        eruptTicks,
                        chain,
                        maxDepth,
                        depth,
                        alreadyHit,
                        activeEruptions);
            }
            spawnCraterRim(visuals, center, craterRadius, 1.0, current);
            
            updateRisingPuffs(visuals, center, puffs);
            spawnVolcanoSplash(visuals, center, eruptLocal, eruptTicks);
            return true;
        });
    }

    private static void playBuildAmbient(VisualEffectService visuals, Location center) {
        
        visuals.sound("BLOCK_LAVA_POP", center, 0.95f, 0.55f + (float) (Math.random() * 0.25));
        visuals.sound("BLOCK_LAVA_POP", center, 0.55f, 0.35f);
        if (Math.random() < 0.35) {
            visuals.sound("BLOCK_FIRE_EXTINGUISH", center, 0.25f, 0.45f);
        }
    }

    private static void stopBuildAmbient(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            
            EntityCompat.stopSound(player, "BLOCK_LAVA_AMBIENT");
            EntityCompat.stopSound(player, "BLOCK_FIRE_AMBIENT");
            EntityCompat.stopSound(player, "block.lava.ambient");
            EntityCompat.stopSound(player, "block.fire.ambient");
            EntityCompat.stopSound(player, "BLOCK_LAVA_POP");
            EntityCompat.stopSound(player, "BLOCK_FIRE_EXTINGUISH");
        }
    }

    private static void explode(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Location center,
            EffectDamageConfig damage,
            double craterRadius,
            int buildTicks,
            int eruptTicks,
            boolean chain,
            int maxDepth,
            int depth,
            Set<UUID> alreadyHit,
            AtomicInteger activeEruptions) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 2.6f, 0.75f);
        visuals.particle("EXPLOSION_EMITTER", center, 2, 0.2, 0.2, 0.2, 0.0, null);
        visuals.particle("LAVA", center, ParticleScale.scale(30), 1.4, 0.8, 1.4, 0.15, null);
        visuals.particle("LARGE_SMOKE", center, ParticleScale.scale(18), 1.0, 0.6, 1.0, 0.04, null);
        visuals.particle("FLAME", center, ParticleScale.scale(20), 1.0, 0.5, 1.0, 0.08, null);

        if (killer == null || !damage.enabled()) {
            return;
        }

        double radius = ERUPT_SPLASH_DAMAGE_RADIUS;
        double radiusSq = radius * radius;
        List<Player> newlyHit = new ArrayList<>();

        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.equals(killer)) {
                continue;
            }
            if (entity.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, entity.getLocation())) {
                continue;
            }
            entity.damage(damage.value(), killer);
            entity.setFireTicks(Math.max(entity.getFireTicks(), 40));
            if (entity instanceof Player player && alreadyHit.add(player.getUniqueId())) {
                newlyHit.add(player);
            }
        }

        if (!chain || depth >= maxDepth || newlyHit.isEmpty()) {
            return;
        }

        long delay = 8L;
        for (int i = 0; i < newlyHit.size(); i++) {
            Player target = newlyHit.get(i);
            Location next = target.getLocation().clone().add(0.5, 0.05, 0.5);
            long offset = delay + i * 4L;
            session.runLater(offset, () -> {
                if (!session.active() || !target.isOnline() || target.isDead()) {
                    return;
                }
                erupt(
                        session,
                        visuals,
                        killer,
                        next,
                        damage,
                        craterRadius * 0.9,
                        Math.max(40, buildTicks - 20),
                        Math.max(16, eruptTicks - 4),
                        chain,
                        maxDepth,
                        depth + 1,
                        alreadyHit,
                        activeEruptions);
            });
        }
    }

    private static void spawnCraterRim(
            VisualEffectService visuals, Location center, double radius, double progress, int tick) {
        double rim = radius * (0.65 + 0.35 * progress);
        int points = ParticleScale.scale(16);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points + tick * 0.03;
            Location rimPoint = center.clone().add(Math.cos(angle) * rim, 0.08, Math.sin(angle) * rim);
            visuals.dust(rimPoint, SMOKE_DARK, 0.85f, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 2 == 0) {
                visuals.particle("ASH", rimPoint, 1, 0.05, 0.02, 0.05, 0.0, null);
            }
            if (i % 3 == 0) {
                visuals.particle("FLAME", rimPoint, 1, 0.03, 0.04, 0.03, 0.0, null);
            }
        }
        
        if (tick % 2 == 0) {
            visuals.dust(center, SMOKE_DARK, 0.7f, 2, 0.25, 0.02, 0.25, 0.0);
            visuals.particle("LAVA", center, 1, 0.15, 0.05, 0.15, 0.0, null);
        }
    }

    private static void spawnRisingPuffs(
            VisualEffectService visuals,
            Location center,
            double craterRadius,
            List<SmokePuff> puffs,
            double progress) {
        int spawnCount = progress < 0.25 ? 2 : (progress < 0.6 ? 3 : 4);
        for (int i = 0; i < spawnCount; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double dist = Math.random() * craterRadius * 0.45;
            puffs.add(new SmokePuff(
                    Math.cos(angle) * dist,
                    Math.sin(angle) * dist,
                    0,
                    (Math.random() - 0.5) * 0.018,
                    (Math.random() - 0.5) * 0.018));
        }
        updateRisingPuffs(visuals, center, puffs);
    }

    private static void updateRisingPuffs(
            VisualEffectService visuals, Location center, List<SmokePuff> puffs) {
        Iterator<SmokePuff> it = puffs.iterator();
        while (it.hasNext()) {
            SmokePuff puff = it.next();
            puff.age++;
            double height = puff.age * 0.34;
            if (height >= PUFF_MAX_HEIGHT) {
                it.remove();
                continue;
            }
            
            double grow = height / PUFF_MAX_HEIGHT;
            double eased = grow * grow;
            float size = (float) (0.35 + eased * 3.8);
            int count = 2 + (int) (eased * 7);
            double x = puff.x + puff.driftX * puff.age;
            double z = puff.z + puff.driftZ * puff.age;
            Location point = center.clone().add(x, height, z);

            Color color = grow < 0.3 ? SMOKE_DARK : (grow < 0.65 ? SMOKE_MID : SMOKE_LIGHT);
            double ox = 0.02 + eased * 0.55;
            double oy = 0.02 + eased * 0.35;
            visuals.dust(point, color, size, count, ox, oy, ox, 0.0);

            int clouds = 1 + (int) (eased * 3);
            visuals.particle("CLOUD", point, clouds, ox * 0.7, oy * 0.5, ox * 0.7, 0.0, null);

            if (grow > 0.2) {
                double ring = 0.2 + eased * 1.35;
                int ringPoints = grow > 0.5 ? 6 : 4;
                for (int r = 0; r < ringPoints; r++) {
                    double a = (Math.PI * 2.0 * r / ringPoints) + puff.age * 0.18;
                    Location rim = point.clone().add(Math.cos(a) * ring, 0.0, Math.sin(a) * ring);
                    visuals.dust(rim, color, Math.max(0.45f, size * 0.6f), 2, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
        while (puffs.size() > 40) {
            puffs.remove(0);
        }
    }

    private static void spawnVolcanoSplash(
            VisualEffectService visuals, Location center, int eruptTick, int eruptTicks) {
        double life = (eruptTick + 0.5) / (double) Math.max(1, eruptTicks);
        int samples = 9;
        for (int j = 0; j < SPLASH_JETS; j++) {
            double angle = Math.PI * 2.0 * j / SPLASH_JETS + 0.08;
            double peak = 7.5 + (j % 3) * 0.85;
            double reach = 6.8 + (j % 4) * 0.7;
            double rim = 0.4 + (j % 2) * 0.15;
            drawLavaJet(visuals, center, angle, rim, reach, peak, life, samples);
            if (j % 2 == 0) {
                drawLavaJet(
                        visuals,
                        center,
                        angle + Math.PI / SPLASH_JETS,
                        rim + 0.12,
                        reach * 0.75,
                        peak * 0.72,
                        life,
                        6);
            }
        }
        visuals.particle("LAVA", center, ParticleScale.scale(8), 0.25, 0.55, 0.25, 0.12, null);
    }

    private static void drawLavaJet(
            VisualEffectService visuals,
            Location center,
            double angle,
            double rim,
            double reach,
            double peak,
            double life,
            int samples) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double maxT = Math.min(1.0, life * 1.15);
        for (int step = 0; step <= samples; step++) {
            double t = (step / (double) samples) * maxT;
            if (t < 0.02) {
                continue;
            }
            double outward = rim + t * reach;
            double height = peak * (4.0 * t * (1.0 - t));
            Location point = center.clone().add(cos * outward, height, sin * outward);
            visuals.particle("LAVA", point, 1, 0.03, 0.04, 0.03, 0.01, null);
            if (step % 2 == 0) {
                visuals.particle("FLAME", point, 1, 0.02, 0.03, 0.02, 0.01, null);
            }
            if (step % 3 == 0) {
                visuals.particle("FALLING_LAVA", point, 1, 0.02, 0.05, 0.02, 0.0, null);
            }
        }
        double tipOut = rim + maxT * reach;
        double tipY = peak * (4.0 * maxT * (1.0 - maxT));
        Location tip = center.clone().add(cos * tipOut, tipY, sin * tipOut);
        visuals.particle("LAVA", tip, 2, 0.05, 0.06, 0.05, 0.02, null);
        visuals.particle("DRIPPING_LAVA", tip, 1, 0.02, 0.04, 0.02, 0.0, null);
    }

    private static final class SmokePuff {
        private final double x;
        private final double z;
        private final double driftX;
        private final double driftZ;
        private int age;

        private SmokePuff(double x, double z, int age, double driftX, double driftZ) {
            this.x = x;
            this.z = z;
            this.age = age;
            this.driftX = driftX;
            this.driftZ = driftZ;
        }
    }
}
