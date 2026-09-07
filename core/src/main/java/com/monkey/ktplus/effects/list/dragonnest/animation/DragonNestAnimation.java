package com.monkey.ktplus.effects.list.dragonnest.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.dragonnest.animation.util.DragonNestDisplays;
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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class DragonNestAnimation {
    private static final int DEFAULT_RINGS = 3;
    private static final double DEFAULT_ORBIT_SPEED = 0.2;
    private static final double DEFAULT_ADVANCE_SPEED = 0.12;
    private static final int DEFAULT_BURN_SECONDS = 3;
    private static final int DEFAULT_HATCH_TICKS = 40;
    private static final int DEFAULT_MAX_LEVEL = 3;
    private static final int DEFAULT_DURATION_TICKS = 160;
    private static final int HIT_COOLDOWN = 14;

    private static final Color MAGENTA = Color.fromRGB(190, 40, 210);
    private static final Color PURPLE = Color.fromRGB(110, 30, 180);
    private static final Color DEEP = Color.fromRGB(55, 10, 90);
    private static final Color HOT = Color.fromRGB(255, 90, 200);

    private DragonNestAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.15, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("dragonnest");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int ringCount = perks == null ? DEFAULT_RINGS : Math.max(1, perks.getInt("rings", DEFAULT_RINGS));
        double orbitSpeed = perks == null
                ? DEFAULT_ORBIT_SPEED
                : Math.max(0.05, perks.getDouble("orbit-speed", DEFAULT_ORBIT_SPEED));
        double advanceSpeed = perks == null
                ? DEFAULT_ADVANCE_SPEED
                : Math.max(0.04, perks.getDouble("advance-speed", DEFAULT_ADVANCE_SPEED));
        int burnSeconds = perks == null
                ? DEFAULT_BURN_SECONDS
                : Math.max(1, perks.getInt("burn-seconds", DEFAULT_BURN_SECONDS));
        int hatchTicks = perks == null
                ? DEFAULT_HATCH_TICKS
                : Math.max(10, perks.getInt("hatch-ticks", DEFAULT_HATCH_TICKS));
        int maxLevel = perks == null
                ? DEFAULT_MAX_LEVEL
                : Math.max(1, perks.getInt("max-level", DEFAULT_MAX_LEVEL));
        final int durationTicks = perks == null
                ? DEFAULT_DURATION_TICKS
                : Math.max(40, perks.getInt("duration-ticks", DEFAULT_DURATION_TICKS));

        EffectDamageConfig damageCfg = context.config().effectDamage("dragonnest");
        double damageValue = damageCfg.enabled() ? Math.max(0.5, damageCfg.value()) : 5.0;
        double damageRadius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 8.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        ItemDisplay egg = DragonNestDisplays.spawnEgg(origin.clone().add(0, 0.6, 0));
        if (egg != null) {
            session.trackEntity(egg);
        }

        AtomicInteger level = new AtomicInteger(1);
        AtomicInteger tick = new AtomicInteger();
        Set<UUID> hitCooldown = new HashSet<>();
        List<BreathRing> rings = new ArrayList<>(ringCount);
        boolean[] hatched = {false};
        boolean[] finished = {false};

        session.onCleanup(() -> {
            DragonNestDisplays.remove(egg);
            for (BreathRing ring : rings) {
                for (ItemDisplay orb : ring.orbs) {
                    DragonNestDisplays.remove(orb);
                }
            }
            rings.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_ENDER_DRAGON_GROWL", origin, 1.3f, 0.75f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 0.9f, 0.55f);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks) {
                finish(visuals, origin, egg, rings, killer);
                finished[0] = true;
                return false;
            }

            int lvl = level.get();
            if (!hatched[0]) {
                hatchPhase(visuals, origin, egg, current, hatchTicks);
                if (current >= hatchTicks) {
                    hatched[0] = true;
                    spawnRings(session, origin, rings, ringCount, lvl);
                    visuals.sound("ENTITY_ENDER_DRAGON_FLAP", origin, 1.4f, 0.85f);
                    visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.75f, 1.25f);
                    visuals.particle("DRAGON_BREATH", origin.clone().add(0, 1.2, 0), ParticleScale.scale(40), 0.8, 0.6, 0.8, 0.04, null);
                }
                return true;
            }

            double ringMul = 1.0 + (lvl - 1) * 0.35;
            updateRings(
                    session,
                    visuals,
                    world,
                    origin,
                    killer,
                    victimId,
                    rings,
                    current,
                    hatchTicks,
                    orbitSpeed * (1.0 + (lvl - 1) * 0.12),
                    advanceSpeed * (1.0 + (lvl - 1) * 0.1),
                    ringMul,
                    damageValue * (0.85 + lvl * 0.15),
                    damageRadius * ringMul,
                    burnSeconds,
                    hitCooldown,
                    level,
                    maxLevel);

            drawNestAura(visuals, origin, current, lvl);

            if (current % 3 == 0 && killer != null && killer.isOnline()) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&5🐉 DRAGON NEST &8| &dLVL &f%d&8/&f%d &8| &5RINGS &f%d &8| &cBURN &f%ds",
                                lvl,
                                maxLevel,
                                rings.size(),
                                burnSeconds));
            }
            return true;
        });
    }

    private static void hatchPhase(
            VisualEffectService visuals, Location origin, ItemDisplay egg, int current, int hatchTicks) {
        float pulse = 0.7f + (float) Math.sin(current * 0.35) * 0.18f + current / (float) hatchTicks * 0.55f;
        float yaw = current * 0.12f;
        DragonNestDisplays.place(egg, origin.clone().add(0, 0.55 + Math.sin(current * 0.2) * 0.08, 0), pulse, yaw, 0.15f);
        if (current % 4 == 0) {
            visuals.dust(origin.clone().add(0, 1.0, 0), PURPLE, 1.4f, 6, 0.35, 0.35, 0.35, 0.0);
            visuals.particle("PORTAL", origin.clone().add(0, 0.8, 0), 8, 0.3, 0.4, 0.3, 0.2, null);
        }
        if (current % 8 == 0) {
            visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 0.55f, 0.7f + current * 0.01f);
        }
        double crack = current / (double) hatchTicks;
        int cracks = ParticleScale.scale(4 + (int) (crack * 10));
        for (int i = 0; i < cracks; i++) {
            double a = (Math.PI * 2.0 * i) / cracks + current * 0.08;
            double r = 0.6 + crack * 1.4;
            Location p = origin.clone().add(Math.cos(a) * r, 0.2 + crack * 0.8, Math.sin(a) * r);
            visuals.dust(p, i % 2 == 0 ? MAGENTA : DEEP, 1.1f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void spawnRings(
            EffectSession session, Location origin, List<BreathRing> rings, int ringCount, int level) {
        for (int i = 0; i < ringCount; i++) {
            BreathRing ring = new BreathRing(i, 1.2 + i * 1.1, i * 0.55);
            int orbs = 6 + i * 2 + level;
            for (int o = 0; o < orbs; o++) {
                double a = (Math.PI * 2.0 * o) / orbs + i * 0.4;
                Location at = origin.clone().add(Math.cos(a) * ring.radius, 0.9, Math.sin(a) * ring.radius);
                ItemDisplay orb = DragonNestDisplays.spawnBreathOrb(at);
                if (orb != null) {
                    session.trackEntity(orb);
                    ring.orbs.add(orb);
                    ring.angles.add(a);
                }
            }
            rings.add(ring);
        }
    }

    private static void updateRings(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<BreathRing> rings,
            int current,
            int hatchTicks,
            double orbitSpeed,
            double advanceSpeed,
            double ringMul,
            double damage,
            double hitRadius,
            int burnSeconds,
            Set<UUID> hitCooldown,
            AtomicInteger level,
            int maxLevel) {
        int age = current - hatchTicks;
        for (BreathRing ring : rings) {
            ring.radius += advanceSpeed * (0.7 + ring.index * 0.15);
            double height = 0.7 + Math.sin(age * 0.08 + ring.phase) * 0.35 + ring.index * 0.15;
            for (int i = 0; i < ring.orbs.size(); i++) {
                ItemDisplay orb = ring.orbs.get(i);
                if (orb == null || !orb.isValid() || orb.isDead()) {
                    continue;
                }
                double angle = ring.angles.get(i) + age * orbitSpeed * (1.0 + ring.index * 0.08);
                ring.angles.set(i, angle);
                double r = ring.radius * ringMul;
                Location at = origin.clone().add(Math.cos(angle) * r, height, Math.sin(angle) * r);
                float scale = 0.28f + ring.index * 0.05f + level.get() * 0.04f;
                DragonNestDisplays.place(orb, at, scale, (float) angle, (float) Math.sin(age * 0.2 + i) * 0.4f);

                visuals.dust(at, MAGENTA, 1.05f, 1, 0.0, 0.0, 0.0, 0.0);
                if (i % 2 == 0) {
                    visuals.particle("DRAGON_BREATH", at, 1, 0.05, 0.05, 0.05, 0.01, null);
                }
                if (age % 2 == 0) {
                    Location trail = at.clone().add(0, -0.15, 0);
                    visuals.dust(trail, PURPLE, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }

            if (age % 2 == 0 && ring.orbs.size() >= 2) {
                for (int i = 0; i < ring.orbs.size(); i++) {
                    double a0 = ring.angles.get(i);
                    double a1 = ring.angles.get((i + 1) % ring.angles.size());
                    for (int s = 1; s <= 2; s++) {
                        double t = s / 3.0;
                        double a = a0 + (a1 - a0) * t;
                        Location mid = origin.clone().add(
                                Math.cos(a) * ring.radius * ringMul,
                                height,
                                Math.sin(a) * ring.radius * ringMul);
                        visuals.dust(mid, DEEP, 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }
        }

        if (age % 3 != 0) {
            return;
        }

        double checkR = Math.min(hitRadius, rings.isEmpty() ? hitRadius : rings.get(rings.size() - 1).radius * ringMul + 1.2);
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
            double dist = horizontalDistance(origin, player.getLocation());
            if (dist > checkR) {
                continue;
            }
            if (hitCooldown.contains(player.getUniqueId())) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }

            hitCooldown.add(player.getUniqueId());
            UUID id = player.getUniqueId();
            session.runLater(HIT_COOLDOWN, () -> hitCooldown.remove(id));

            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            player.setFireTicks(Math.max(player.getFireTicks(), burnSeconds * 20));
            Vector push = player.getLocation().toVector().subtract(origin.toVector());
            push.setY(0.0);
            if (push.lengthSquared() < 0.01) {
                push = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            push.normalize().multiply(0.35).setY(0.28);
            player.setVelocity(player.getVelocity().add(push));

            Location hitAt = player.getLocation().clone().add(0, 1, 0);
            visuals.particle("FLAME", hitAt, ParticleScale.scale(12), 0.25, 0.35, 0.25, 0.02, null);
            visuals.dust(hitAt, HOT, 1.5f, 8, 0.3, 0.3, 0.3, 0.0);
            visuals.sound("ENTITY_BLAZE_HURT", hitAt, 0.9f, 0.7f);
            visuals.sound("ENTITY_ENDER_DRAGON_HURT", origin, 0.55f, 1.2f);

            if (level.get() < maxLevel) {
                level.incrementAndGet();
                visuals.sound("ENTITY_PLAYER_LEVELUP", origin, 0.8f, 0.85f);
                visuals.sound("ENTITY_ENDER_DRAGON_GROWL", origin, 0.7f, 1.15f);
                visuals.dust(origin.clone().add(0, 1.5, 0), MAGENTA, 1.8f, 20, 0.8, 0.6, 0.8, 0.0);
            }
        }
    }

    private static void drawNestAura(VisualEffectService visuals, Location origin, int tick, int level) {
        int points = ParticleScale.scale(10 + level * 3);
        double spin = tick * 0.09;
        for (int i = 0; i < points; i++) {
            double a = spin + (Math.PI * 2.0 * i) / points;
            double r = 0.9 + (i % 3) * 0.35;
            Location p = origin.clone().add(Math.cos(a) * r, 0.15 + Math.sin(tick * 0.15 + i) * 0.1, Math.sin(a) * r);
            visuals.dust(p, i % 2 == 0 ? PURPLE : DEEP, 1.2f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % 10 == 0) {
            visuals.sound("ENTITY_ENDER_DRAGON_FLAP", origin, 0.35f, 0.55f);
            visuals.particle("DRAGON_BREATH", origin.clone().add(0, 1.4, 0), ParticleScale.scale(6), 0.5, 0.5, 0.5, 0.02, null);
        }
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            ItemDisplay egg,
            List<BreathRing> rings,
            Player killer) {
        visuals.sound("ENTITY_ENDER_DRAGON_DEATH", origin, 0.55f, 1.4f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.9f, 0.85f);
        visuals.particle("DRAGON_BREATH", origin.clone().add(0, 1.2, 0), ParticleScale.scale(50), 1.2, 0.8, 1.2, 0.05, null);
        visuals.dust(origin.clone().add(0, 1.0, 0), MAGENTA, 1.8f, 30, 1.0, 0.8, 1.0, 0.0);
        DragonNestDisplays.remove(egg);
        for (BreathRing ring : rings) {
            for (ItemDisplay orb : ring.orbs) {
                if (orb != null && orb.isValid()) {
                    visuals.dust(orb.getLocation(), PURPLE, 1.2f, 4, 0.15, 0.15, 0.15, 0.0);
                }
                DragonNestDisplays.remove(orb);
            }
            ring.orbs.clear();
        }
        rings.clear();
        PerkActionBar.clear(killer);
    }

    private static double horizontalDistance(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final class BreathRing {
        private final int index;
        private double radius;
        private final double phase;
        private final List<ItemDisplay> orbs = new ArrayList<>();
        private final List<Double> angles = new ArrayList<>();

        private BreathRing(int index, double radius, double phase) {
            this.index = index;
            this.radius = radius;
            this.phase = phase;
        }
    }
}
