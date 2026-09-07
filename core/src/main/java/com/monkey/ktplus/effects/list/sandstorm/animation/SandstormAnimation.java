package com.monkey.ktplus.effects.list.sandstorm.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.sandstorm.animation.util.SandstormDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class SandstormAnimation {
    private static final double DEFAULT_WAVE_WIDTH = 8.0;
    private static final double DEFAULT_WAVE_LENGTH = 14.0;
    private static final double DEFAULT_WAVE_SPEED = 0.28;
    private static final double DEFAULT_PUSH_STRENGTH = 0.55;
    private static final double DEFAULT_BLIND_CHANCE = 0.35;
    private static final int DEFAULT_DURATION = 100;
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;

    private static final Color SAND_LIGHT = Color.fromRGB(230, 205, 140);
    private static final Color SAND_MID = Color.fromRGB(200, 165, 95);
    private static final Color SAND_DARK = Color.fromRGB(160, 120, 60);
    private static final Color DUST_BROWN = Color.fromRGB(120, 90, 50);

    private static final Material[] GRAIN_MATS = {
        Material.SAND,
        Material.RED_SAND,
        Material.SANDSTONE,
        Material.YELLOW_CONCRETE_POWDER,
        Material.ORANGE_CONCRETE_POWDER
    };

    private static final String[] EFFECT_SOUNDS = {
        "ITEM_ELYTRA_FLYING",
        "ENTITY_BREEZE_IDLE_GROUND",
        "ENTITY_BREEZE_WIND_BURST",
        "BLOCK_SAND_BREAK",
        "BLOCK_SAND_PLACE"
    };

    private SandstormAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("sandstorm");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double waveWidth = perks == null
                ? DEFAULT_WAVE_WIDTH
                : Math.max(3.0, perks.getDouble("wave-width", DEFAULT_WAVE_WIDTH));
        double waveLength = perks == null
                ? DEFAULT_WAVE_LENGTH
                : Math.max(4.0, perks.getDouble("wave-length", DEFAULT_WAVE_LENGTH));
        double waveSpeed = perks == null
                ? DEFAULT_WAVE_SPEED
                : Math.max(0.1, perks.getDouble("wave-speed", DEFAULT_WAVE_SPEED));
        double pushStrength = perks == null
                ? DEFAULT_PUSH_STRENGTH
                : Math.max(0.1, perks.getDouble("push-strength", DEFAULT_PUSH_STRENGTH));
        double blindChance = perks == null
                ? DEFAULT_BLIND_CHANCE
                : Math.max(0.0, Math.min(1.0, perks.getDouble("blind-chance", DEFAULT_BLIND_CHANCE)));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(40, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("sandstorm");
        double damageValue = damageCfg.enabled()
                ? Math.max(0.0, damageCfg.value())
                : (perks == null ? 3.0 : perks.getDouble("damage-value", 3.0));
        double damageRadius = damageCfg.enabled()
                ? Math.max(1.0, damageCfg.radius())
                : 6.0;
        PotionEffectType blindness = PotionTypes.resolve("BLINDNESS", "BLINDNESS");

        final Vector forward = resolveForward(killer);
        Vector rightVec = forward.clone().crossProduct(new Vector(0, 1, 0));
        final Vector right;
        if (rightVec.lengthSquared() < 1.0e-6) {
            right = new Vector(1, 0, 0);
        } else {
            right = rightVec.normalize();
        }

        Location front = origin.clone();
        List<Grain> grains = new ArrayList<>(COLUMNS * ROWS);
        for (int c = 0; c < COLUMNS; c++) {
            double across = ((c / (double) (COLUMNS - 1)) - 0.5) * waveWidth;
            for (int r = 0; r < ROWS; r++) {
                double height = 0.35 + r * 0.55 + (c % 3) * 0.08;
                double depth = (r % 3) * 0.25 + (c % 2) * 0.12;
                Location spawn = origin.clone()
                        .add(right.clone().multiply(across))
                        .add(forward.clone().multiply(-depth))
                        .add(0.0, height, 0.0);
                Material mat = GRAIN_MATS[(c + r) % GRAIN_MATS.length];
                float scale = 0.35f + (r % 3) * 0.08f + (c % 2) * 0.05f;
                ItemDisplay display = SandstormDisplays.spawnGrain(spawn, mat, scale * 0.4f);
                if (display == null) {
                    continue;
                }
                session.trackEntity(display);
                grains.add(new Grain(
                        display,
                        across,
                        height,
                        depth,
                        scale,
                        (c * 0.37 + r * 0.19),
                        c,
                        r));
            }
        }

        Set<UUID> hitCooldown = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        double[] traveled = {0.0};

        session.onCleanup(() -> {
            stopSounds(front);
            for (Grain grain : grains) {
                SandstormDisplays.remove(grain.display);
            }
            PerkActionBar.clear(killer);
        });

        visuals.sound("ITEM_ELYTRA_FLYING", origin, 0.55f, 0.7f);
        visuals.sound("BLOCK_SAND_BREAK", origin, 1.0f, 0.75f);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || traveled[0] >= waveLength) {
                dissipate(visuals, front, grains);
                stopSounds(front);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            traveled[0] += waveSpeed;
            front.add(forward.clone().multiply(waveSpeed));
            snapNearGround(world, front);

            updateGrains(visuals, grains, front, forward, right, current);
            drawStorm(visuals, front, forward, right, waveWidth, current);
            applyForce(
                    session,
                    visuals,
                    world,
                    front,
                    forward,
                    right,
                    killer,
                    victimId,
                    waveWidth,
                    pushStrength,
                    blindChance,
                    blindness,
                    damageValue,
                    damageRadius,
                    hitCooldown,
                    current);

            if (current % 6 == 0) {
                visuals.sound("BLOCK_SAND_PLACE", front, 0.45f, 0.7f + (float) Math.random() * 0.4f);
                visuals.sound("ITEM_ELYTRA_FLYING", front, 0.2f, 0.55f);
            }
            if (current % 12 == 0) {
                visuals.sound("ENTITY_BREEZE_IDLE_GROUND", front, 0.3f, 0.8f);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&e🌪 SANDSTORM &8| &6ADVANCE &f%.0f&8/&f%.0f &8| &7%d",
                                traveled[0],
                                waveLength,
                                Math.max(0, durationTicks - current)));
            }
            return true;
        });
    }

    private static Vector resolveForward(Player killer) {
        if (killer != null && killer.isOnline()) {
            Vector dir = killer.getLocation().getDirection().clone();
            dir.setY(0);
            if (dir.lengthSquared() > 1.0e-4) {
                return dir.normalize();
            }
        }
        double angle = Math.random() * Math.PI * 2.0;
        return new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();
    }

    private static void updateGrains(
            VisualEffectService visuals,
            List<Grain> grains,
            Location front,
            Vector forward,
            Vector right,
            int tick) {
        for (Grain grain : grains) {
            double sway = Math.sin(tick * 0.18 + grain.phase) * 0.35;
            double lift = Math.sin(tick * 0.22 + grain.phase * 1.3) * 0.25;
            double scatter = Math.cos(tick * 0.15 + grain.col) * 0.2;
            Location at = front.clone()
                    .add(right.clone().multiply(grain.across + sway))
                    .add(forward.clone().multiply(-grain.depth + scatter))
                    .add(0.0, grain.height + lift, 0.0);
            float yaw = (float) (Math.atan2(forward.getX(), forward.getZ()) + tick * 0.05 + grain.phase);
            float pitch = (float) (Math.sin(tick * 0.2 + grain.phase) * 0.5);
            float roll = (float) (tick * 0.08 + grain.phase);
            float scale = grain.scale * (0.9f + (float) Math.sin(tick * 0.25 + grain.phase) * 0.12f);
            SandstormDisplays.place(grain.display, at, scale, yaw, pitch, roll);
            if (tick % 2 == grain.row % 2) {
                visuals.dust(at, grain.row % 2 == 0 ? SAND_MID : SAND_LIGHT, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void drawStorm(
            VisualEffectService visuals,
            Location front,
            Vector forward,
            Vector right,
            double waveWidth,
            int tick) {
        visuals.dust(front.clone().add(0, 1.2, 0), SAND_MID, 1.4f, ParticleScale.scale(8), waveWidth * 0.2, 0.7, waveWidth * 0.2, 0.0);
        visuals.dust(front.clone().add(0, 0.6, 0), SAND_DARK, 1.1f, ParticleScale.scale(6), waveWidth * 0.25, 0.45, waveWidth * 0.25, 0.0);
        if (tick % 2 == 0) {
            visuals.dust(front.clone().add(0, 1.8, 0), SAND_LIGHT, 0.95f, ParticleScale.scale(5), waveWidth * 0.18, 0.55, waveWidth * 0.18, 0.0);
        }

        for (int i = 0; i < 10; i++) {
            double across = ((i / 9.0) - 0.5) * waveWidth;
            double height = 0.2 + (i % 4) * 0.45 + Math.sin(tick * 0.2 + i) * 0.2;
            Location scrape = front.clone()
                    .add(right.clone().multiply(across))
                    .add(forward.clone().multiply(-0.4 + Math.sin(tick * 0.15 + i) * 0.3))
                    .add(0.0, height, 0.0);
            Color color = i % 3 == 0 ? SAND_LIGHT : (i % 3 == 1 ? SAND_MID : DUST_BROWN);
            visuals.dust(scrape, color, 1.0f, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 2 == tick % 2) {
                Location ground = front.clone().add(right.clone().multiply(across)).add(0.0, 0.05, 0.0);
                visuals.dust(ground, DUST_BROWN, 0.9f, 1, 0.08, 0.02, 0.08, 0.0);
            }
        }

        if (tick % 3 == 0) {
            visuals.particle("CLOUD", front.clone().add(0, 1.0, 0), ParticleScale.scale(6), waveWidth * 0.15, 0.4, waveWidth * 0.15, 0.01, null);
            visuals.blockParticle(
                    "BLOCK",
                    front.clone().add(0, 0.2, 0),
                    ParticleScale.scale(4),
                    waveWidth * 0.12,
                    0.1,
                    waveWidth * 0.12,
                    0.0,
                    Material.SAND);
        }
    }

    private static void applyForce(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location front,
            Vector forward,
            Vector right,
            Player killer,
            UUID victimId,
            double waveWidth,
            double pushStrength,
            double blindChance,
            PotionEffectType blindness,
            double damageValue,
            double damageRadius,
            Set<UUID> hitCooldown,
            int tick) {
        double halfWidth = waveWidth * 0.55;
        double depth = 2.4;
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
            Location feet = player.getLocation();
            Vector relative = feet.toVector().subtract(front.toVector());
            double along = relative.dot(forward);
            double across = relative.dot(right);
            if (along < -0.8 || along > depth || Math.abs(across) > halfWidth) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, feet)) {
                continue;
            }

            Vector push = forward.clone().multiply(pushStrength);
            push.setY(0.12 + Math.max(0.0, 0.18 - along * 0.05));
            push.add(right.clone().multiply(Math.sin(tick * 0.2 + across) * 0.08));
            player.setVelocity(player.getVelocity().multiply(0.45).add(push));

            if (tick % 2 == 0) {
                visuals.dust(feet.clone().add(0, 1, 0), SAND_MID, 0.95f, 2, 0.1, 0.15, 0.1, 0.0);
            }

            if (damageValue > 0.0
                    && killer != null
                    && !hitCooldown.contains(player.getUniqueId())
                    && feet.distanceSquared(front) <= damageRadius * damageRadius) {
                hitCooldown.add(player.getUniqueId());
                final UUID id = player.getUniqueId();
                session.runLater(10L, () -> hitCooldown.remove(id));
                player.damage(damageValue, killer);
                visuals.sound("ENTITY_PLAYER_HURT", feet, 0.65f, 0.9f);
                if (blindness != null && Math.random() < blindChance) {
                    player.addPotionEffect(new PotionEffect(blindness, 35, 0, true, true, true));
                    visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", feet, 0.4f, 0.7f);
                }
            }
        }
    }

    private static void snapNearGround(World world, Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int startY = Math.min(world.getMaxHeight() - 2, loc.getBlockY() + 2);
        int minY = Math.max(world.getMinHeight(), loc.getBlockY() - 4);
        for (int y = startY; y >= minY; y--) {
            if (!world.getBlockAt(x, y, z).getType().isAir()
                    && world.getBlockAt(x, y, z).getType().isSolid()
                    && (world.getBlockAt(x, y + 1, z).getType().isAir()
                            || !world.getBlockAt(x, y + 1, z).getType().isSolid())) {
                loc.setY(y + 1.05);
                return;
            }
        }
    }

    private static void dissipate(VisualEffectService visuals, Location front, List<Grain> grains) {
        visuals.sound("BLOCK_SAND_BREAK", front, 1.1f, 0.65f);
        visuals.sound("ENTITY_BREEZE_WIND_BURST", front, 0.5f, 0.9f);
        visuals.dust(front.clone().add(0, 1, 0), SAND_LIGHT, 1.6f, ParticleScale.scale(22), 1.2, 0.8, 1.2, 0.0);
        visuals.dust(front.clone().add(0, 0.5, 0), SAND_DARK, 1.2f, ParticleScale.scale(14), 1.4, 0.5, 1.4, 0.0);
        for (Grain grain : grains) {
            SandstormDisplays.remove(grain.display);
        }
        grains.clear();
        stopSounds(front);
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : EFFECT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Grain {
        private final ItemDisplay display;
        private final double across;
        private final double height;
        private final double depth;
        private final float scale;
        private final double phase;
        private final int col;
        private final int row;

        private Grain(
                ItemDisplay display,
                double across,
                double height,
                double depth,
                float scale,
                double phase,
                int col,
                int row) {
            this.display = display;
            this.across = across;
            this.height = height;
            this.depth = depth;
            this.scale = scale;
            this.phase = phase;
            this.col = col;
            this.row = row;
        }
    }
}
