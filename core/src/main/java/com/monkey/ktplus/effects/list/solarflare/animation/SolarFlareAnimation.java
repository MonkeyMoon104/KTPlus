package com.monkey.ktplus.effects.list.solarflare.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.solarflare.animation.util.SolarFlareDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
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
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class SolarFlareAnimation {
    private static final int MAX_TICKS = 140;
    private static final int DEFAULT_CHARGE = 25;
    private static final double DEFAULT_RING_SPEED = 0.35;
    private static final int DEFAULT_BURN = 3;
    private static final int DEFAULT_BLIND = 25;
    private static final int RING_PIECES = 16;
    private static final double MAX_RING = 9.0;

    private static final Color WHITE = Color.fromRGB(255, 255, 245);
    private static final Color GOLD = Color.fromRGB(255, 210, 90);
    private static final Color ORANGE = Color.fromRGB(255, 140, 40);

    private static final String[] AMBIENT_SOUNDS = {
        "BLOCK_BEACON_AMBIENT",
        "BLOCK_BEACON_ACTIVATE",
        "ENTITY_BLAZE_SHOOT",
        "ENTITY_GENERIC_EXPLODE",
        "BLOCK_FIRE_AMBIENT"
    };

    private SolarFlareAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.15, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("solarflare");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int chargeTicks = perks == null
                ? DEFAULT_CHARGE
                : Math.max(10, perks.getInt("charge-ticks", DEFAULT_CHARGE));
        double ringSpeed = perks == null
                ? DEFAULT_RING_SPEED
                : Math.max(0.1, perks.getDouble("ring-speed", DEFAULT_RING_SPEED));
        int burnSeconds = perks == null
                ? DEFAULT_BURN
                : Math.max(0, perks.getInt("burn-seconds", DEFAULT_BURN));
        int blindTicks = perks == null
                ? DEFAULT_BLIND
                : Math.max(0, perks.getInt("blind-ticks", DEFAULT_BLIND));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("solarflare");
        double ringDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 5.0;
        PotionEffectType blindness = PotionTypes.resolve("BLINDNESS", "BLINDNESS");

        Location coreAt = origin.clone().add(0, 1.8, 0);
        ItemDisplay core = SolarFlareDisplays.spawn(coreAt, SolarFlareDisplays.coreMaterial(), 0.85f);
        if (core != null) {
            session.trackEntity(core);
        }

        List<ItemDisplay> ring = new ArrayList<>(RING_PIECES);
        List<RingBit> bits = new ArrayList<>(RING_PIECES);
        for (int i = 0; i < RING_PIECES; i++) {
            ItemDisplay piece = SolarFlareDisplays.spawn(coreAt, SolarFlareDisplays.ringMaterial(i), 0.28f);
            if (piece == null) {
                continue;
            }
            session.trackEntity(piece);
            ring.add(piece);
            bits.add(new RingBit(piece, (Math.PI * 2.0 * i) / RING_PIECES, i));
        }

        session.onCleanup(() -> {
            stopAmbient(origin);
            SolarFlareDisplays.remove(core);
            SolarFlareDisplays.removeAll(ring);
        });

        visuals.sound("BLOCK_BEACON_ACTIVATE", coreAt, 1.0f, 1.6f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", coreAt, 0.7f, 1.4f);
        session.resetDeadline(MAX_TICKS + 20L);

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        boolean[] flashed = {false};
        double[] ringRadius = {0.4};
        Set<UUID> hitIds = new HashSet<>();

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopAmbient(origin);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= MAX_TICKS || ringRadius[0] > MAX_RING) {
                finish(visuals, origin, core, ring);
                finished[0] = true;
                return false;
            }

            if (current < chargeTicks) {
                float charge = current / (float) chargeTicks;
                float scale = 0.5f + charge * 1.1f;
                SolarFlareDisplays.place(core, coreAt, current * 0.12f, 0.2f, current * 0.08f, scale);
                for (RingBit bit : bits) {
                    double a = bit.baseAngle + current * 0.15;
                    double r = 0.6 + charge * 0.8;
                    Location at = coreAt.clone().add(Math.cos(a) * r, Math.sin(a * 2) * 0.15, Math.sin(a) * r);
                    SolarFlareDisplays.place(bit.display, at, (float) a, 0.3f, current * 0.1f, 0.22f + charge * 0.2f);
                }
                visuals.dust(coreAt, WHITE, 1.0f + charge, ParticleScale.scale(4), 0.2, 0.2, 0.2, 0.0);
                if (current % 5 == 0) {
                    visuals.sound("BLOCK_BEACON_AMBIENT", coreAt, 0.35f, 1.4f + charge);
                }
                return true;
            }

            if (!flashed[0]) {
                flashed[0] = true;
                whiteFlash(visuals, coreAt);
                visuals.sound("ENTITY_GENERIC_EXPLODE", coreAt, 1.1f, 1.5f);
                visuals.sound("ENTITY_BLAZE_SHOOT", coreAt, 0.9f, 0.7f);
            }

            ringRadius[0] += ringSpeed;
            SolarFlareDisplays.place(core, coreAt, current * 0.2f, 0.4f, current * 0.12f, 1.35f);

            for (RingBit bit : bits) {
                double a = bit.baseAngle + current * 0.08;
                Location at = origin.clone().add(
                        Math.cos(a) * ringRadius[0],
                        0.35 + Math.sin(current * 0.2 + bit.index) * 0.12,
                        Math.sin(a) * ringRadius[0]);
                SolarFlareDisplays.place(bit.display, at, (float) a, 0.9f, current * 0.15f, 0.4f);
            }

            drawFireRing(visuals, origin, ringRadius[0], current);
            hurtRing(
                    session,
                    visuals,
                    world,
                    killer,
                    victimId,
                    origin,
                    ringRadius[0],
                    ringDamage,
                    burnSeconds,
                    blindTicks,
                    blindness,
                    hitIds);

            if (current % 8 == 0) {
                visuals.sound("BLOCK_FIRE_AMBIENT", origin, 0.35f, 1.2f);
            }
            return true;
        });
    }

    private static void whiteFlash(VisualEffectService visuals, Location at) {
        visuals.dust(at, WHITE, 2.2f, ParticleScale.scale(40), 1.4, 1.0, 1.4, 0.0);
        visuals.dust(at, GOLD, 1.8f, ParticleScale.scale(24), 1.0, 0.7, 1.0, 0.0);
        visuals.particle("CLOUD", at, ParticleScale.scale(28), 1.2, 0.8, 1.2, 0.04, null);
    }

    private static void drawFireRing(VisualEffectService visuals, Location origin, double radius, int tick) {
        int points = ParticleScale.scale(12 + (int) radius);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points + tick * 0.05;
            Location p = origin.clone().add(Math.cos(a) * radius, 0.2 + Math.sin(tick * 0.25 + i) * 0.15, Math.sin(a) * radius);
            Color color = i % 3 == 0 ? WHITE : (i % 3 == 1 ? GOLD : ORANGE);
            visuals.dust(p, color, 1.3f, 1, 0, 0, 0, 0);
        }
        visuals.particle("CLOUD", origin.clone().add(0, 0.4, 0), 4, radius * 0.2, 0.1, radius * 0.2, 0.01, null);
    }

    private static void hurtRing(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location origin,
            double radius,
            double damage,
            int burnSeconds,
            int blindTicks,
            PotionEffectType blindness,
            Set<UUID> hitIds) {
        if (killer == null) {
            return;
        }
        double inner = Math.max(0.0, radius - 1.1);
        double outer = radius + 0.85;
        double innerSq = inner * inner;
        double outerSq = outer * outer;
        for (Player player : world.getPlayers()) {
            if (player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (hitIds.contains(player.getUniqueId())) {
                continue;
            }
            double d = player.getLocation().distanceSquared(origin);
            if (d < innerSq || d > outerSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            hitIds.add(player.getUniqueId());
            if (damage > 0.0) {
                player.damage(damage, killer);
            }
            if (burnSeconds > 0) {
                player.setFireTicks(Math.max(player.getFireTicks(), burnSeconds * 20));
            }
            if (blindness != null && blindTicks > 0) {
                player.addPotionEffect(new PotionEffect(blindness, blindTicks, 0, true, true, true));
            }
            Vector away = player.getLocation().toVector().subtract(origin.toVector());
            away.setY(0.0);
            if (away.lengthSquared() < 1.0e-4) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(0.65).setY(0.35);
            player.setVelocity(player.getVelocity().add(away));
            visuals.dust(player.getLocation().add(0, 1, 0), GOLD, 1.5f, 8, 0.2, 0.25, 0.2, 0.0);
        }
    }

    private static void finish(
            VisualEffectService visuals, Location origin, ItemDisplay core, List<ItemDisplay> ring) {
        stopAmbient(origin);
        visuals.dust(origin.clone().add(0, 1.5, 0), WHITE, 1.6f, ParticleScale.scale(16), 0.8, 0.6, 0.8, 0.0);
        visuals.particle("CLOUD", origin.clone().add(0, 1.2, 0), ParticleScale.scale(14), 0.7, 0.5, 0.7, 0.02, null);
        SolarFlareDisplays.remove(core);
        SolarFlareDisplays.removeAll(ring);
    }

    private static void stopAmbient(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class RingBit {
        private final ItemDisplay display;
        private final double baseAngle;
        private final int index;

        private RingBit(ItemDisplay display, double baseAngle, int index) {
            this.display = display;
            this.baseAngle = baseAngle;
            this.index = index;
        }
    }
}
