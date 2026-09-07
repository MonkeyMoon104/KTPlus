package com.monkey.ktplus.effects.list.earthquake.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.list.earthquake.animation.util.EarthquakeDebris;
import com.monkey.ktplus.effects.list.earthquake.animation.util.EarthquakeDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class EarthquakeAnimation {
    private static final int DEFAULT_SHAKE_TICKS = 70;
    private static final int DEFAULT_MAX_BLOCKS = 64;
    private static final double DEFAULT_RADIUS = 8.0;
    private static final double DEFAULT_AMPLITUDE = 0.14;
    private static final double DEFAULT_FREQUENCY = 1.15;
    private static final double DEFAULT_WAVE_WIDTH = 1.65;
    private static final double DEFAULT_BOUNCE_STRENGTH = 0.95;
    private static final int DEBRIS_TICKS = 10;

    private EarthquakeAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location center = context.location().clone().add(0.5, 0.0, 0.5);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("earthquake");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int shakeTicks = perks == null
                ? DEFAULT_SHAKE_TICKS
                : Math.max(30, perks.getInt("shake-ticks", DEFAULT_SHAKE_TICKS));
        int maxBlocks = perks == null
                ? DEFAULT_MAX_BLOCKS
                : Math.max(8, perks.getInt("max-blocks", DEFAULT_MAX_BLOCKS));
        double radius = perks == null ? DEFAULT_RADIUS : Math.max(2.0, perks.getDouble("radius", DEFAULT_RADIUS));
        double amplitude = perks == null
                ? DEFAULT_AMPLITUDE
                : Math.max(0.01, perks.getDouble("amplitude", DEFAULT_AMPLITUDE));
        double frequency = perks == null
                ? DEFAULT_FREQUENCY
                : Math.max(0.3, perks.getDouble("frequency", DEFAULT_FREQUENCY));
        double waveWidth = perks == null
                ? DEFAULT_WAVE_WIDTH
                : Math.max(0.8, perks.getDouble("wave-width", DEFAULT_WAVE_WIDTH));
        double bounceStrength = perks == null
                ? DEFAULT_BOUNCE_STRENGTH
                : Math.max(0.3, perks.getDouble("bounce-strength", DEFAULT_BOUNCE_STRENGTH));
        boolean debris = perks == null || perks.getBoolean("debris", true);

        Player killer = context.killer();
        EffectDamageConfig damage = context.config().effectDamage("earthquake");

        List<Block> surface = EarthquakeDisplays.sampleSurface(world, center, killer, session, radius, maxBlocks);
        if (surface.isEmpty()) {
            visuals.sound("ENTITY_GENERIC_EXPLODE", center, 0.8f, 0.55f);
            BuiltInDamageService.apply(session, killer, center, damage);
            return;
        }

        List<ShakingBlock> shaking = new ArrayList<>(surface.size());
        List<BlockData> debrisSamples = new ArrayList<>(surface.size());

        for (Block block : surface) {
            BlockData data = block.getBlockData().clone();
            debrisSamples.add(data);
            Location base = block.getLocation().clone();

            BlockDisplay display = EarthquakeDisplays.spawn(base, data);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            double dist = horizontalDistance(center, base.clone().add(0.5, 0.0, 0.5));
            double phase = Math.random() * Math.PI * 2.0;
            shaking.add(new ShakingBlock(display, base, dist, phase));
        }

        session.onCleanup(() -> {
            for (ShakingBlock entry : shaking) {
                EarthquakeDisplays.remove(entry.display);
            }
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_WARDEN_SONIC_BOOM", center, 0.55f, 0.45f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 1.0f, 0.4f);
        visuals.sound("BLOCK_GRASS_BREAK", center, 1.2f, 0.55f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] finishedWave = {false};
        
        Set<UUID> bouncedOut = new HashSet<>();
        Set<UUID> bouncedIn = new HashSet<>();
        int total = shakeTicks + DEBRIS_TICKS + 5;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                cleanupDisplays(shaking);
                PerkActionBar.clear(killer);
                return false;
            }

            if (current < shakeTicks) {
                double progress = (current + 1) / (double) shakeTicks;
                boolean outward = progress <= 0.5;
                double leg = outward ? (progress / 0.5) : ((1.0 - progress) / 0.5);
                double ringDist = leg * radius;
                String stage = outward ? "OUT" : "BACK";

                for (ShakingBlock entry : shaking) {
                    if (entry.display == null || !entry.display.isValid() || entry.display.isDead()) {
                        continue;
                    }
                    double gap = Math.abs(entry.dist - ringDist);
                    double influence = 1.0 - Math.min(1.0, gap / waveWidth);
                    if (influence <= 0.02) {
                        
                        EarthquakeDisplays.place(entry.display, entry.base, 0.0f);
                        continue;
                    }
                    double localAmp = amplitude * influence * influence;
                    double wave = Math.abs(Math.sin(current * frequency + entry.phase)) * localAmp;
                    double jitterX =
                            Math.sin(current * frequency * 1.6 + entry.phase) * localAmp * 0.18;
                    double jitterZ =
                            Math.cos(current * frequency * 1.3 + entry.phase * 0.8) * localAmp * 0.18;
                    Location at = entry.base.clone().add(jitterX, wave, jitterZ);
                    EarthquakeDisplays.place(entry.display, at, (float) (entry.phase * 0.04));
                }

                spawnWaveRingFx(visuals, center, ringDist, current);
                if (current % 5 == 0) {
                    visuals.sound(
                            "BLOCK_STONE_HIT",
                            center.clone().add(0, 0.2, 0),
                            0.75f,
                            0.4f + (float) (ringDist / radius) * 0.35f);
                    visuals.sound("BLOCK_GRAVEL_HIT", center, 0.5f, 0.55f);
                }

                bouncePlayersOnWave(
                        session,
                        world,
                        center,
                        killer,
                        ringDist,
                        waveWidth,
                        bounceStrength,
                        outward,
                        outward ? bouncedOut : bouncedIn,
                        damage);

                if (current % 3 == 0 && killer != null && killer.isOnline()) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&6⛰ EARTHQUAKE &8| &eWAVE &f%s &8| &7RING &f%.1f&8/&f%.0f &8| &cBOUNCED &f%d",
                                    stage,
                                    ringDist,
                                    radius,
                                    bouncedOut.size() + bouncedIn.size()));
                }
                return true;
            }

            if (!finishedWave[0]) {
                finishedWave[0] = true;
                cleanupDisplays(shaking);
                for (Block block : surface) {
                    session.restoreTemporaryBlock(block);
                }

                visuals.sound("ENTITY_GENERIC_EXPLODE", center, 1.8f, 0.5f);
                visuals.sound("ENTITY_WARDEN_ATTACK_IMPACT", center, 1.0f, 0.65f);
                visuals.sound("ENTITY_GENERIC_EXPLODE", center, 1.2f, 0.75f);
                visuals.particle("EXPLOSION_EMITTER", center.clone().add(0, 0.4, 0), 2, 0.25, 0.15, 0.25, 0.0, null);
                visuals.blockParticle(
                        "BLOCK",
                        center.clone().add(0, 0.3, 0),
                        ParticleScale.scale(48),
                        radius * 0.4,
                        0.3,
                        radius * 0.4,
                        0.1,
                        Material.DIRT);

                if (debris) {
                    EarthquakeDebris.burst(session, killer, center, debrisSamples);
                }

                BuiltInDamageService.apply(session, killer, center, damage);
                burstBounce(session, world, center, killer, radius, bounceStrength * 1.15, damage);

                PerkActionBar.show(
                        killer,
                        String.format(
                                "&6⛰ EARTHQUAKE &8| &cBURST &8| &7BOUNCED &f%d",
                                bouncedOut.size() + bouncedIn.size()));
            }

            return true;
        });
    }

    private static void cleanupDisplays(List<ShakingBlock> shaking) {
        for (ShakingBlock entry : shaking) {
            EarthquakeDisplays.remove(entry.display);
        }
        shaking.clear();
    }

    private static void spawnWaveRingFx(
            VisualEffectService visuals, Location center, double ringDist, int tick) {
        int points = Math.max(8, ParticleScale.scale(10 + (int) (ringDist * 1.5)));
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points + tick * 0.05;
            Location at = center.clone().add(Math.cos(angle) * ringDist, 0.12, Math.sin(angle) * ringDist);
            visuals.blockParticle("BLOCK", at, 2, 0.12, 0.04, 0.12, 0.01, Material.DIRT);
            if (i % 2 == 0) {
                visuals.particle("CLOUD", at, 1, 0.04, 0.02, 0.04, 0.0, null);
            }
        }
    }

    private static void bouncePlayersOnWave(
            EffectSession session,
            World world,
            Location center,
            Player killer,
            double ringDist,
            double waveWidth,
            double strength,
            boolean outward,
            Set<UUID> alreadyBounced,
            EffectDamageConfig damage) {
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (alreadyBounced.contains(player.getUniqueId())) {
                continue;
            }
            double dist = horizontalDistance(center, player.getLocation());
            if (Math.abs(dist - ringDist) > waveWidth) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            alreadyBounced.add(player.getUniqueId());
            Vector away = player.getLocation().toVector().subtract(center.toVector());
            away.setY(0.0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(Math.random() - 0.5, 0.0, Math.random() - 0.5);
            }
            away.normalize();
            double push = outward ? strength : strength * 0.85;
            away.multiply(push).setY(0.45 + strength * 0.25);
            player.setVelocity(away);
            applyBounceDamage(player, killer, damage);
        }
    }

    private static void burstBounce(
            EffectSession session,
            World world,
            Location center,
            Player killer,
            double radius,
            double strength,
            EffectDamageConfig damage) {
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            Vector away = player.getLocation().toVector().subtract(center.toVector());
            away.setY(0.0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(0.0, 0.0, 1.0);
            }
            away.normalize().multiply(strength * 1.2).setY(0.55);
            player.setVelocity(away);
            applyBounceDamage(player, killer, damage);
        }
    }

    private static void applyBounceDamage(Player player, Player killer, EffectDamageConfig damage) {
        double amount = damage != null && damage.enabled() ? Math.max(0.5, damage.value()) : 1.5;
        if (amount <= 0.0) {
            return;
        }
        if (killer != null) {
            player.damage(amount, killer);
        } else {
            player.damage(amount);
        }
    }

    private static double horizontalDistance(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final class ShakingBlock {
        private final BlockDisplay display;
        private final Location base;
        private final double dist;
        private final double phase;

        private ShakingBlock(BlockDisplay display, Location base, double dist, double phase) {
            this.display = display;
            this.base = base.clone();
            this.dist = dist;
            this.phase = phase;
        }
    }
}
