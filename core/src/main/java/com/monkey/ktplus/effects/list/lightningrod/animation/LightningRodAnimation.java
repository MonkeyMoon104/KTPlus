package com.monkey.ktplus.effects.list.lightningrod.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.lightningrod.animation.util.LightningRodDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class LightningRodAnimation {
    private static final int DEFAULT_MAX_JUMPS = 5;
    private static final double DEFAULT_JUMP_RANGE = 10.0;
    private static final double DEFAULT_DAMAGE_FALLOFF = 0.75;
    private static final int DEFAULT_CHARGE_TICKS = 25;
    private static final double DEFAULT_ROD_HEIGHT = 2.4;
    private static final int DEFAULT_ATTACK_INTERVAL = 18;
    private static final int DEFAULT_HIT_COOLDOWN = 16;
    private static final int DEFAULT_MAX_LEVEL = 10;
    private static final double DEFAULT_RANGE_PER_LEVEL = 0.55;
    private static final double DEFAULT_DAMAGE_PER_LEVEL = 0.35;
    private static final int JUMP_INTERVAL = 8;
    private static final int POST_CHAIN_TICKS = 35;

    private static final Color COPPER = Color.fromRGB(210, 120, 55);
    private static final Color SPARK = Color.fromRGB(180, 220, 255);
    private static final Color CORE = Color.fromRGB(90, 170, 255);
    private static final Color WHITE = Color.fromRGB(245, 250, 255);

    private LightningRodAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("lightningrod");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int maxJumps = perks == null ? DEFAULT_MAX_JUMPS : Math.max(1, perks.getInt("max-jumps", DEFAULT_MAX_JUMPS));
        double jumpRange = perks == null
                ? DEFAULT_JUMP_RANGE
                : Math.max(2.0, perks.getDouble("jump-range", DEFAULT_JUMP_RANGE));
        double falloff = perks == null
                ? DEFAULT_DAMAGE_FALLOFF
                : Math.max(0.1, Math.min(1.0, perks.getDouble("damage-falloff", DEFAULT_DAMAGE_FALLOFF)));
        int chargeTicks = perks == null
                ? DEFAULT_CHARGE_TICKS
                : Math.max(8, perks.getInt("charge-ticks", DEFAULT_CHARGE_TICKS));
        double rodHeight = perks == null
                ? DEFAULT_ROD_HEIGHT
                : Math.max(1.2, perks.getDouble("rod-height", DEFAULT_ROD_HEIGHT));
        int attackInterval = perks == null
                ? DEFAULT_ATTACK_INTERVAL
                : Math.max(10, perks.getInt("attack-interval-ticks", DEFAULT_ATTACK_INTERVAL));
        int hitCooldown = perks == null
                ? DEFAULT_HIT_COOLDOWN
                : Math.max(6, perks.getInt("hit-cooldown-ticks", DEFAULT_HIT_COOLDOWN));
        int maxLevel = perks == null
                ? DEFAULT_MAX_LEVEL
                : Math.max(1, Math.min(10, perks.getInt("max-level", DEFAULT_MAX_LEVEL)));
        double rangePerLevel = perks == null
                ? DEFAULT_RANGE_PER_LEVEL
                : Math.max(0.0, perks.getDouble("range-per-level", DEFAULT_RANGE_PER_LEVEL));
        double damagePerLevel = perks == null
                ? DEFAULT_DAMAGE_PER_LEVEL
                : Math.max(0.0, perks.getDouble("damage-per-level", DEFAULT_DAMAGE_PER_LEVEL));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("lightningrod");
        double baseDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 0.0;

        ItemDisplay rod = LightningRodDisplays.spawnRod(origin.clone().add(0.0, 0.4, 0.0));
        if (rod == null) {
            return;
        }
        session.trackEntity(rod);

        List<ItemDisplay> sparks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2.0 * i) / 6.0;
            Location sparkAt = origin.clone().add(Math.cos(angle) * 0.55, 0.6 + i * 0.08, Math.sin(angle) * 0.55);
            ItemDisplay spark = LightningRodDisplays.spawnCopperSpark(sparkAt);
            if (spark != null) {
                session.trackEntity(spark);
                sparks.add(spark);
            }
        }

        List<BlockDisplay> spiral = new ArrayList<>(maxLevel);
        AtomicInteger level = new AtomicInteger(0);

        session.onCleanup(() -> {
            LightningRodDisplays.remove(rod);
            LightningRodDisplays.removeAll(sparks);
            LightningRodDisplays.removeAllBlocks(spiral);
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_COPPER_HIT", origin, 1.1f, 0.7f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 0.85f, 1.35f);

        int activeTicks = Math.max(maxJumps * JUMP_INTERVAL + POST_CHAIN_TICKS, attackInterval * 8);
        int total = chargeTicks + activeTicks;
        session.resetDeadline(total + 20L);

        AtomicInteger tick = new AtomicInteger();
        AtomicInteger jumpsDone = new AtomicInteger();
        Set<UUID> chainHit = new HashSet<>();
        Map<UUID, Integer> hitUntil = new HashMap<>();
        Location[] lastArc = {origin.clone().add(0.0, rodHeight * 0.55, 0.0)};
        boolean[] chaining = {false};
        int[] nextChainAt = {chargeTicks};
        int[] nextJumpAt = {chargeTicks};

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                LightningRodDisplays.remove(rod);
                LightningRodDisplays.removeAll(sparks);
                LightningRodDisplays.removeAllBlocks(spiral);
                finishBurst(visuals, origin);
                PerkActionBar.clear(killer);
                return false;
            }

            int chargeLevel = level.get();
            float pulse = 0.85f + (float) (Math.sin(current * 0.28) * 0.18) + chargeLevel * 0.02f;
            float yaw = current * 0.09f;
            double lift = Math.sin(current * 0.22) * 0.12;
            Location rodAt = origin.clone().add(0.0, 0.35 + lift + (current < chargeTicks
                    ? (current / (double) chargeTicks) * (rodHeight - 0.8)
                    : rodHeight - 0.8), 0.0);
            LightningRodDisplays.placeRod(rod, rodAt, pulse, yaw);
            updateSpiral(session, origin, rodAt, spiral, chargeLevel, current, rodHeight);

            for (int i = 0; i < sparks.size(); i++) {
                double angle = (Math.PI * 2.0 * i) / sparks.size() + current * 0.14 + chargeLevel * 0.08;
                double radius = 0.45 + Math.sin(current * 0.18 + i) * 0.18 + chargeLevel * 0.03;
                Location sparkAt = origin.clone().add(
                        Math.cos(angle) * radius,
                        0.4 + (i % 3) * 0.35 + Math.sin(current * 0.25 + i) * 0.12,
                        Math.sin(angle) * radius);
                LightningRodDisplays.placeSpark(
                        sparks.get(i), sparkAt, 0.16f + (i % 2) * 0.04f, (float) angle, current * 0.2f, current * 0.15f);
            }

            if (current < chargeTicks) {
                chargeAura(visuals, origin, rodAt, current, chargeTicks);
                if (current % 5 == 0) {
                    visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", rodAt, 0.45f, 1.2f + current * 0.02f);
                    visuals.sound("BLOCK_COPPER_HIT", rodAt, 0.35f, 0.8f + current * 0.015f);
                }
                if (killer != null && killer.isOnline() && current % 4 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&6⚡ LIGHTNING ROD &8| &eCHARGE &f%d&8/&f%d &8| &7LVL &f%d&8/&f%d",
                                    current,
                                    chargeTicks,
                                    chargeLevel,
                                    maxLevel));
                }
                return true;
            }

            Location rodTip = rodAt.clone().add(0.0, 0.6, 0.0);
            double scaledRange = jumpRange + chargeLevel * rangePerLevel;
            double scaledDamage = baseDamage + chargeLevel * damagePerLevel;

            if (current >= nextChainAt[0]) {
                if (hasAnyTarget(world, rodTip, killer, victimId, scaledRange, hitUntil, current, session)) {
                    world.strikeLightningEffect(rodTip);
                    visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", origin, 0.55f, 1.65f);
                    visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", origin, 0.9f, 1.25f);
                    visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", rodAt, 0.7f, 1.1f + chargeLevel * 0.05f);
                    visuals.sound("BLOCK_COPPER_PLACE", rodAt, 0.65f, 0.75f + chargeLevel * 0.04f);
                    chaining[0] = true;
                    chainHit.clear();
                    jumpsDone.set(0);
                    lastArc[0] = rodTip.clone();
                    nextJumpAt[0] = current;
                }
                nextChainAt[0] = current + attackInterval;
            }

            if (chaining[0] && jumpsDone.get() < maxJumps && current >= nextJumpAt[0]) {
                Player target = findNext(
                        world, lastArc[0], killer, victimId, scaledRange, chainHit, hitUntil, current, session);
                if (target != null) {
                    Location hitAt = target.getLocation().clone().add(0.0, 1.0, 0.0);
                    drawArc(visuals, lastArc[0], hitAt, jumpsDone.get());
                    double dmg = scaledDamage * Math.pow(falloff, jumpsDone.get());
                    if (killer != null && dmg > 0.0 && session.allowsWorldMutation(killer, target.getLocation())) {
                        target.damage(dmg, killer);
                        if (level.get() < maxLevel) {
                            int next = level.incrementAndGet();
                            growSpiralSegment(session, origin, spiral, next - 1);
                            visuals.sound("BLOCK_COPPER_HIT", rodAt, 0.9f, 0.7f + next * 0.06f);
                            visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", rodAt, 0.55f, 1.2f + next * 0.05f);
                        }
                    }
                    UUID id = target.getUniqueId();
                    chainHit.add(id);
                    hitUntil.put(id, current + hitCooldown);
                    lastArc[0] = hitAt;
                    jumpsDone.incrementAndGet();
                    nextJumpAt[0] = current + JUMP_INTERVAL;
                    visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", hitAt, 0.7f, 1.4f + jumpsDone.get() * 0.05f);
                    visuals.particle("ELECTRIC_SPARK", hitAt, 18, 0.25, 0.35, 0.25, 0.08, null);
                } else {
                    chaining[0] = false;
                }
            }

            if (jumpsDone.get() >= maxJumps) {
                chaining[0] = false;
            }

            if (current % 3 == 0) {
                visuals.dust(rodAt, SPARK, 1.1f, 2, 0.08, 0.2, 0.08, 0.0);
                visuals.dust(origin.clone().add(0, 0.2, 0), COPPER, 0.9f, 2, 0.2, 0.05, 0.2, 0.0);
            }
            if (killer != null && killer.isOnline() && current % 4 == 0) {
                String phase = current < chargeTicks ? "&eCHARGE" : "&bSTRIKE";
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&6⚡ LIGHTNING ROD &8| %s &8| &eLVL &f%d&8/&f%d &8| &7RANGE &f%.1f &8| &cDMG &f%.1f",
                                phase,
                                chargeLevel,
                                maxLevel,
                                scaledRange,
                                scaledDamage));
            }
            return true;
        });
    }

    private static void growSpiralSegment(
            EffectSession session, Location origin, List<BlockDisplay> spiral, int index) {
        if (index < 0 || index >= spiral.size()) {
            BlockDisplay segment = LightningRodDisplays.spawnCopperSegment(origin.clone().add(0.0, 0.2, 0.0), index);
            if (segment != null) {
                session.trackEntity(segment);
                spiral.add(segment);
            }
        }
    }

    private static void updateSpiral(
            EffectSession session,
            Location origin,
            Location rodAt,
            List<BlockDisplay> spiral,
            int chargeLevel,
            int tick,
            double rodHeight) {
        while (spiral.size() < chargeLevel) {
            growSpiralSegment(session, origin, spiral, spiral.size());
        }
        double spin = tick * (0.12 + chargeLevel * 0.018);
        for (int i = 0; i < spiral.size(); i++) {
            double t = (i + 1) / (double) Math.max(1, Math.max(chargeLevel, 1));
            double angle = spin + i * 0.85;
            double radius = 0.35 + i * 0.045;
            double y = 0.15 + t * Math.max(0.8, rodHeight - 0.5);
            Location at = origin.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            float scale = 0.18f + i * 0.012f;
            LightningRodDisplays.placeCopper(spiral.get(i), at, scale, (float) angle, (float) (Math.sin(tick * 0.2 + i) * 0.25));
        }
    }

    private static void chargeAura(
            VisualEffectService visuals, Location origin, Location rodAt, int current, int chargeTicks) {
        double progress = (current + 1) / (double) chargeTicks;
        int rings = 8;
        for (int i = 0; i < rings; i++) {
            double angle = (Math.PI * 2.0 * i) / rings + current * 0.2;
            double radius = 0.35 + progress * 1.1;
            Location p = origin.clone().add(Math.cos(angle) * radius, 0.15 + progress * 0.8, Math.sin(angle) * radius);
            visuals.dust(p, progress > 0.55 ? SPARK : COPPER, 0.85f + (float) progress * 0.5f, 1, 0, 0, 0, 0);
        }
        visuals.particle("ELECTRIC_SPARK", rodAt, 4, 0.12, 0.25, 0.12, 0.02, null);
        if (current % 2 == 0) {
            visuals.dust(rodAt, CORE, 1.35f, 1, 0.02, 0.05, 0.02, 0.0);
        }
    }

    private static void drawArc(VisualEffectService visuals, Location from, Location to, int jumpIndex) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < 0.01) {
            return;
        }
        Vector dir = delta.clone().normalize();
        Vector side = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (side.lengthSquared() < 1.0e-6) {
            side = new Vector(1, 0, 0);
        } else {
            side.normalize();
        }
        int steps = Math.max(8, (int) (length * 4.5));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Location point = from.clone().add(dir.clone().multiply(length * t));
            double jag = Math.sin(t * Math.PI * 3.0 + jumpIndex) * 0.22 * Math.sin(t * Math.PI);
            point.add(side.clone().multiply(jag));
            point.add(0.0, Math.sin(t * Math.PI) * 0.35, 0.0);
            visuals.dust(point, i % 2 == 0 ? WHITE : SPARK, 1.0f + (float) (1.0 - t) * 0.6f, 1, 0, 0, 0, 0);
            if (i % 2 == 0) {
                visuals.particle("ELECTRIC_SPARK", point, 1, 0.0, 0.0, 0.0, 0.0, null);
            }
        }
        visuals.dust(to, CORE, 1.6f, 6, 0.15, 0.2, 0.15, 0.0);
        visuals.sound("ENTITY_FIREWORK_ROCKET_TWINKLE", to, 0.55f, 1.7f);
    }

    private static void finishBurst(VisualEffectService visuals, Location origin) {
        visuals.particle("ELECTRIC_SPARK", origin.clone().add(0, 1.2, 0), 40, 0.6, 0.8, 0.6, 0.12, null);
        visuals.dust(origin.clone().add(0, 1.0, 0), COPPER, 1.4f, 12, 0.4, 0.5, 0.4, 0.0);
        visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", origin, 0.4f, 1.8f);
    }

    private static boolean hasAnyTarget(
            World world,
            Location from,
            Player killer,
            UUID victimId,
            double range,
            Map<UUID, Integer> hitUntil,
            int current,
            EffectSession session) {
        return findNext(world, from, killer, victimId, range, Set.of(), hitUntil, current, session) != null;
    }

    private static Player findNext(
            World world,
            Location from,
            Player killer,
            UUID victimId,
            double range,
            Set<UUID> chainHit,
            Map<UUID, Integer> hitUntil,
            int current,
            EffectSession session) {
        double rangeSq = range * range;
        List<Player> candidates = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (chainHit.contains(player.getUniqueId())) {
                continue;
            }
            Integer until = hitUntil.get(player.getUniqueId());
            if (until != null && current < until) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(from) > rangeSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            candidates.add(player);
        }
        candidates.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(from)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }
}
