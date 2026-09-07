package com.monkey.ktplus.effects.list.chainsaw.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.chainsaw.animation.util.ChainsawDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.HashSet;
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

public final class ChainsawAnimation {
    private static final int MAX_TICKS = 140;
    private static final double DEFAULT_LENGTH = 4.0;
    private static final double DEFAULT_SPIN = 0.45;
    private static final double DEFAULT_MOVE = 0.28;
    private static final double DEFAULT_TICK_DAMAGE = 1.5;
    private static final double HIT_RADIUS = 1.35;

    private static final Color STEEL = Color.fromRGB(180, 185, 195);
    private static final Color SPARK = Color.fromRGB(255, 210, 90);
    private static final Color OIL = Color.fromRGB(40, 35, 30);

    private static final String[] AMBIENT_SOUNDS = {
        "ENTITY_PLAYER_ATTACK_SWEEP",
        "ITEM_TRIDENT_THROW",
        "ENTITY_IRON_GOLEM_ATTACK",
        "BLOCK_ANVIL_HIT"
    };

    private ChainsawAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("chainsaw");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double length = perks == null ? DEFAULT_LENGTH : Math.max(2.0, perks.getDouble("length", DEFAULT_LENGTH));
        double spinSpeed = perks == null ? DEFAULT_SPIN : Math.max(0.15, perks.getDouble("spin-speed", DEFAULT_SPIN));
        double moveSpeed = perks == null ? DEFAULT_MOVE : Math.max(0.1, perks.getDouble("move-speed", DEFAULT_MOVE));
        double tickDamage = perks == null
                ? DEFAULT_TICK_DAMAGE
                : Math.max(0.0, perks.getDouble("tick-damage", DEFAULT_TICK_DAMAGE));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("chainsaw");
        double splash = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 0.0;
        double damage = Math.max(tickDamage, splash);

        final Vector forward;
        if (killer != null && killer.isOnline() && killer.getWorld().equals(world)) {
            Vector dir = killer.getLocation().getDirection().clone();
            dir.setY(0.0);
            if (dir.lengthSquared() < 1.0e-4) {
                forward = new Vector(1, 0, 0);
            } else {
                forward = dir.normalize();
            }
        } else {
            forward = new Vector(1, 0, 0);
        }
        final Vector side = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        ItemDisplay blade = ChainsawDisplays.spawnBlade(origin);
        if (blade == null) {
            return;
        }
        session.trackEntity(blade);

        Location tip = origin.clone();
        Set<UUID> hitCooldown = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        double[] traveled = {0.0};
        int[] zigSign = {1};

        session.onCleanup(() -> {
            stopAmbient(tip);
            ChainsawDisplays.remove(blade);
        });

        visuals.sound("ENTITY_IRON_GOLEM_ATTACK", origin, 0.9f, 1.35f);
        visuals.sound("ITEM_TRIDENT_THROW", origin, 0.7f, 0.55f);
        session.resetDeadline(MAX_TICKS + 20L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopAmbient(tip);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= MAX_TICKS || traveled[0] >= length) {
                finish(visuals, tip, blade);
                finished[0] = true;
                return false;
            }

            double zig = Math.sin(current * 0.28) * 0.55 * zigSign[0];
            if (current % 18 == 0) {
                zigSign[0] *= -1;
            }
            Vector step = forward.clone().multiply(moveSpeed).add(side.clone().multiply(zig * 0.12));
            step.setY(Math.sin(current * 0.22) * 0.04);
            tip.add(step);
            traveled[0] += moveSpeed;

            float spin = (float) (current * spinSpeed);
            ChainsawDisplays.place(blade, tip, spin, 1.2f, spin * 0.35f, 1.05f + (float) Math.sin(current * 0.2) * 0.08f);

            drawTrail(visuals, tip, forward, side, current);
            if (current % 3 == 0) {
                visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", tip, 0.35f, 1.4f + (float) (Math.random() * 0.25));
            }
            if (current % 7 == 0) {
                visuals.sound("BLOCK_ANVIL_HIT", tip, 0.22f, 1.7f);
            }

            damageNearby(session, visuals, world, killer, victimId, tip, damage, hitCooldown, current);
            return true;
        });
    }

    private static void drawTrail(
            VisualEffectService visuals, Location tip, Vector forward, Vector side, int tick) {
        for (int i = 0; i < 5; i++) {
            double along = i * 0.18;
            Location p = tip.clone()
                    .subtract(forward.clone().multiply(along))
                    .add(side.clone().multiply(Math.sin(tick * 0.4 + i) * 0.25));
            visuals.dust(p, i % 2 == 0 ? STEEL : SPARK, 0.85f + i * 0.08f, 1, 0, 0, 0, 0);
        }
        if (tick % 2 == 0) {
            visuals.particle("CLOUD", tip, ParticleScale.scale(2), 0.08, 0.05, 0.08, 0.01, null);
            visuals.dust(tip, OIL, 0.7f, 2, 0.1, 0.05, 0.1, 0.0);
        }
    }

    private static void damageNearby(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location tip,
            double damage,
            Set<UUID> hitCooldown,
            int tick) {
        if (killer == null || damage <= 0.0) {
            return;
        }
        double radiusSq = HIT_RADIUS * HIT_RADIUS;
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
            if (hitCooldown.contains(player.getUniqueId())) {
                continue;
            }
            if (player.getLocation().clone().add(0, 1, 0).distanceSquared(tip) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            hitCooldown.add(player.getUniqueId());
            UUID id = player.getUniqueId();
            session.runLater(8L, () -> hitCooldown.remove(id));
            player.damage(damage, killer);
            Vector knock = player.getLocation().toVector().subtract(tip.toVector());
            knock.setY(0.0);
            if (knock.lengthSquared() < 1.0e-4) {
                knock = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            knock.normalize().multiply(0.35).setY(0.22);
            player.setVelocity(player.getVelocity().add(knock));
            visuals.dust(player.getLocation().add(0, 1, 0), SPARK, 1.4f, 8, 0.2, 0.25, 0.2, 0.0);
            visuals.sound("ENTITY_PLAYER_HURT", player.getLocation(), 0.7f, 1.1f);
            if (tick % 2 == 0) {
                visuals.particle("CLOUD", tip, 4, 0.15, 0.1, 0.15, 0.02, null);
            }
        }
    }

    private static void finish(VisualEffectService visuals, Location tip, ItemDisplay blade) {
        stopAmbient(tip);
        visuals.dust(tip, STEEL, 1.5f, 14, 0.35, 0.25, 0.35, 0.0);
        visuals.particle("CLOUD", tip, ParticleScale.scale(12), 0.35, 0.2, 0.35, 0.02, null);
        visuals.sound("ENTITY_ITEM_BREAK", tip, 0.85f, 0.7f);
        ChainsawDisplays.remove(blade);
    }

    private static void stopAmbient(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }
}
