package com.monkey.ktplus.effects.list.magnet.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.magnet.animation.util.MagnetDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class MagnetAnimation {
    private static final double DEFAULT_PULL_RADIUS = 14.0;
    private static final double DEFAULT_PULL_STRENGTH = 0.18;
    private static final int DEFAULT_DURATION = 120;
    private static final int DEFAULT_MAX_TARGETS = 6;
    private static final double DEFAULT_CORE_DAMAGE_RADIUS = 2.2;
    private static final int RING_COUNT = 2;
    private static final int RING_PIECES = 10;

    private static final Color DUST_PURPLE = Color.fromRGB(120, 70, 220);
    private static final Color DUST_BLUE = Color.fromRGB(60, 110, 255);
    private static final Color DUST_CYAN = Color.fromRGB(90, 190, 255);
    private static final Color DUST_IRON = Color.fromRGB(160, 165, 175);

    private MagnetAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("magnet");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double pullRadius = perks == null
                ? DEFAULT_PULL_RADIUS
                : Math.max(4.0, perks.getDouble("pull-radius", DEFAULT_PULL_RADIUS));
        double pullStrength = perks == null
                ? DEFAULT_PULL_STRENGTH
                : Math.max(0.05, perks.getDouble("pull-strength", DEFAULT_PULL_STRENGTH));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(40, perks.getInt("duration-ticks", DEFAULT_DURATION));
        int maxTargets = perks == null
                ? DEFAULT_MAX_TARGETS
                : Math.max(1, perks.getInt("max-targets", DEFAULT_MAX_TARGETS));
        double coreDamageRadius = perks == null
                ? DEFAULT_CORE_DAMAGE_RADIUS
                : Math.max(0.8, perks.getDouble("core-damage-radius", DEFAULT_CORE_DAMAGE_RADIUS));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("magnet");
        double coreDamage = damageCfg.enabled()
                ? Math.max(0.0, damageCfg.value())
                : (perks == null ? 2.0 : perks.getDouble("core-damage", 2.0));

        Location magnet = origin.clone().add(0.0, 2.35, 0.0);
        ItemDisplay core = MagnetDisplays.spawnCore(magnet);
        if (core != null) {
            session.trackEntity(core);
        }

        List<RingPiece> ring = new ArrayList<>(RING_COUNT * RING_PIECES);
        for (int r = 0; r < RING_COUNT; r++) {
            double radius = 1.35 + r * 0.85;
            float scale = r == 0 ? 0.32f : 0.22f;
            for (int i = 0; i < RING_PIECES; i++) {
                double angle = (Math.PI * 2.0 * i) / RING_PIECES + r * 0.31;
                Material mat = (i + r) % 2 == 0 ? Material.IRON_NUGGET : Material.RAW_IRON;
                Location spawn = magnet.clone().add(Math.cos(angle) * radius, Math.sin(angle * 2) * 0.15, Math.sin(angle) * radius);
                ItemDisplay display = MagnetDisplays.spawnRingPiece(spawn, mat, scale * 0.35f);
                if (display == null) {
                    continue;
                }
                session.trackEntity(display);
                ring.add(new RingPiece(display, r, i, angle, radius, scale, (i % 5) * 0.17 + r * 0.4));
            }
        }

        Set<UUID> coreHit = new HashSet<>();
        Set<Item> lockedItems = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            releaseLockedItems(lockedItems);
            MagnetDisplays.remove(core);
            for (RingPiece piece : ring) {
                MagnetDisplays.remove(piece.display);
            }
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_BEACON_ACTIVATE", magnet, 0.85f, 1.35f);
        visuals.sound("BLOCK_ANVIL_LAND", magnet, 0.35f, 1.6f);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks) {
                dissipate(visuals, magnet, core, ring);
                releaseLockedItems(lockedItems);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            double bob = Math.sin(current * 0.12) * 0.12;
            Location coreAt = magnet.clone().add(0.0, bob, 0.0);
            MagnetDisplays.place(core, coreAt, 0.95f + (float) Math.sin(current * 0.08) * 0.08f, current * 0.08f, 0.15f, current * 0.04f);

            updateRings(ring, coreAt, current);
            drawField(visuals, coreAt, current, pullRadius);
            int pulled = pullTargets(
                    session,
                    visuals,
                    world,
                    coreAt,
                    killer,
                    victimId,
                    pullRadius,
                    pullStrength,
                    maxTargets,
                    coreDamageRadius,
                    coreDamage,
                    coreHit,
                    lockedItems,
                    current);

            if (current % 8 == 0) {
                visuals.sound("BLOCK_BEACON_AMBIENT", coreAt, 0.35f, 1.5f + (float) (Math.random() * 0.2));
                visuals.sound("BLOCK_NOTE_BLOCK_CHIME", coreAt, 0.22f, 0.55f + (float) (Math.random() * 0.35));
            }
            if (current % 14 == 0) {
                visuals.sound("ENTITY_ENDERMAN_TELEPORT", coreAt, 0.18f, 1.7f);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&5🧲 MAGNET &8| &dPULL &f%d &8| &7%d&8/&7%d",
                                pulled,
                                Math.max(0, durationTicks - current),
                                durationTicks));
            }
            return true;
        });
    }

    private static void updateRings(List<RingPiece> ring, Location coreAt, int tick) {
        for (RingPiece piece : ring) {
            double spin = piece.baseAngle + tick * (0.11 + piece.ring * 0.035) * (piece.ring % 2 == 0 ? 1 : -1);
            double wobble = Math.sin(tick * 0.17 + piece.phase) * 0.22;
            double y = Math.cos(tick * 0.13 + piece.phase) * 0.35 + piece.ring * 0.2;
            Location at = coreAt.clone().add(
                    Math.cos(spin) * (piece.radius + wobble * 0.15),
                    y,
                    Math.sin(spin) * (piece.radius + wobble * 0.15));
            float yaw = (float) (spin + Math.PI * 0.5);
            float pitch = (float) (Math.sin(tick * 0.2 + piece.phase) * 0.45);
            float roll = (float) (tick * 0.09 + piece.phase);
            MagnetDisplays.place(piece.display, at, piece.scale, yaw, pitch, roll);
        }
    }

    private static void drawField(VisualEffectService visuals, Location core, int tick, double pullRadius) {
        visuals.dust(core, DUST_PURPLE, 1.4f, ParticleScale.scale(4), 0.18, 0.22, 0.18, 0.0);
        visuals.dust(core.clone().add(0, 0.4, 0), DUST_BLUE, 1.1f, ParticleScale.scale(3), 0.35, 0.25, 0.35, 0.0);
        if (tick % 2 == 0) {
            visuals.dust(core, DUST_CYAN, 0.85f, ParticleScale.scale(2), 0.55, 0.35, 0.55, 0.0);
        }

        int arcs = 5;
        for (int i = 0; i < arcs; i++) {
            double angle = tick * 0.09 + (Math.PI * 2.0 * i) / arcs;
            double radius = 1.2 + (i % 3) * 0.55 + Math.sin(tick * 0.15 + i) * 0.25;
            double height = Math.sin(tick * 0.2 + i * 1.1) * 0.9;
            Location point = core.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            Color color = i % 2 == 0 ? DUST_PURPLE : DUST_BLUE;
            visuals.dust(point, color, 0.95f + (i % 3) * 0.15f, 1, 0.0, 0.0, 0.0, 0.0);
            if (tick % 3 == i % 3) {
                Location mid = core.clone().add(Math.cos(angle) * radius * 0.55, height * 0.5, Math.sin(angle) * radius * 0.55);
                visuals.dust(mid, DUST_IRON, 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        if (tick % 4 == 0) {
            double ringR = Math.min(pullRadius * 0.45, 4.5 + (tick % 20) * 0.12);
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0 + tick * 0.04;
                Location rim = core.clone().add(Math.cos(a) * ringR, -0.6 + Math.sin(a * 3) * 0.15, Math.sin(a) * ringR);
                visuals.dust(rim, DUST_BLUE, 0.75f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        visuals.particle("ENCHANT", core, ParticleScale.scale(6), 0.55, 0.45, 0.55, 0.4, null);
        if (tick % 5 == 0) {
            visuals.particle("CRIT", core, ParticleScale.scale(4), 0.4, 0.35, 0.4, 0.05, null);
        }
    }

    private static int pullTargets(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location core,
            Player killer,
            UUID victimId,
            double pullRadius,
            double pullStrength,
            int maxTargets,
            double coreDamageRadius,
            double coreDamage,
            Set<UUID> coreHit,
            Set<Item> lockedItems,
            int tick) {
        List<Player> targets = findNearby(world, core, killer, victimId, pullRadius, session);
        if (targets.size() > maxTargets) {
            targets = new ArrayList<>(targets.subList(0, maxTargets));
        }

        double coreSq = coreDamageRadius * coreDamageRadius;
        for (Player player : targets) {
            Location feet = player.getLocation();
            Vector to = core.toVector().subtract(feet.toVector());
            double dist = to.length();
            if (dist < 0.05) {
                continue;
            }
            double falloff = 1.0 - Math.min(1.0, dist / pullRadius);
            Vector pull = to.normalize().multiply(pullStrength * (0.55 + falloff * 1.2));
            pull.setY(Math.min(0.22, pull.getY() + 0.04 * falloff));
            player.setVelocity(player.getVelocity().multiply(0.55).add(pull));

            if (tick % 3 == 0) {
                Location trail = feet.clone().add(0, 1.0, 0);
                visuals.dust(trail, DUST_PURPLE, 0.9f, 1, 0.0, 0.0, 0.0, 0.0);
                visuals.dust(trail.clone().add(pull.clone().normalize().multiply(0.35)), DUST_CYAN, 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
            }

            if (coreDamage > 0.0
                    && killer != null
                    && feet.distanceSquared(core) <= coreSq
                    && !coreHit.contains(player.getUniqueId())
                    && session.allowsWorldMutation(killer, feet)) {
                coreHit.add(player.getUniqueId());
                final UUID id = player.getUniqueId();
                session.runLater(12L, () -> coreHit.remove(id));
                player.damage(coreDamage, killer);
                visuals.sound("ENTITY_PLAYER_HURT", feet, 0.7f, 0.85f);
                visuals.dust(feet.clone().add(0, 1, 0), DUST_PURPLE, 1.4f, 6, 0.2, 0.25, 0.2, 0.0);
            }
        }

        if (tick % 2 == 0) {
            pullItems(world, core, killer, pullRadius, pullStrength * 1.35, session, lockedItems);
        }
        return targets.size();
    }

    private static final int MAGNET_PICKUP_LOCK = 32767;
    private static final int MAGNET_PICKUP_RELEASE = 10;

    private static void pullItems(
            World world,
            Location core,
            Player killer,
            double radius,
            double strength,
            EffectSession session,
            Set<Item> lockedItems) {
        double radiusSq = radius * radius;
        for (Item item : world.getEntitiesByClass(Item.class)) {
            if (!item.isValid() || item.isDead()) {
                continue;
            }
            Location at = item.getLocation();
            if (at.distanceSquared(core) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, at)) {
                continue;
            }
            if (lockedItems.add(item)) {
                item.setPickupDelay(MAGNET_PICKUP_LOCK);
            } else {
                
                item.setPickupDelay(MAGNET_PICKUP_LOCK);
            }
            Vector to = core.toVector().subtract(at.toVector());
            if (to.lengthSquared() < 1.0e-4) {
                continue;
            }
            item.setVelocity(to.normalize().multiply(strength));
        }
    }

    private static void releaseLockedItems(Set<Item> lockedItems) {
        for (Item item : lockedItems) {
            if (item != null && item.isValid() && !item.isDead()) {
                item.setPickupDelay(MAGNET_PICKUP_RELEASE);
            }
        }
        lockedItems.clear();
    }

    private static List<Player> findNearby(
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            EffectSession session) {
        double radiusSq = radius * radius;
        List<Player> found = new ArrayList<>();
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
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            found.add(player);
        }
        found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)));
        return found;
    }

    private static void dissipate(
            VisualEffectService visuals, Location magnet, ItemDisplay core, List<RingPiece> ring) {
        visuals.sound("BLOCK_BEACON_DEACTIVATE", magnet, 0.9f, 1.2f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", magnet, 0.35f, 1.6f);
        visuals.dust(magnet, DUST_PURPLE, 1.6f, ParticleScale.scale(18), 0.6, 0.5, 0.6, 0.0);
        visuals.dust(magnet, DUST_BLUE, 1.2f, ParticleScale.scale(12), 0.8, 0.6, 0.8, 0.0);
        visuals.particle("FLASH", magnet, 1, 0.0, 0.0, 0.0, 0.0, null);
        MagnetDisplays.remove(core);
        for (RingPiece piece : ring) {
            MagnetDisplays.remove(piece.display);
        }
        ring.clear();
    }

    private static final class RingPiece {
        private final ItemDisplay display;
        private final int ring;
        private final int index;
        private final double baseAngle;
        private final double radius;
        private final float scale;
        private final double phase;

        private RingPiece(
                ItemDisplay display,
                int ring,
                int index,
                double baseAngle,
                double radius,
                float scale,
                double phase) {
            this.display = display;
            this.ring = ring;
            this.index = index;
            this.baseAngle = baseAngle;
            this.radius = radius;
            this.scale = scale;
            this.phase = phase;
        }
    }
}
