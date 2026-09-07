package com.monkey.ktplus.effects.list.blackhole.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class BlackHoleAnimation {
    private static final int MAX_LEVEL = 3;
    private static final double DEFAULT_SUCK_RADIUS = 8.0;
    private static final double DEFAULT_SUCK_STRENGTH = 0.2;
    private static final int DEFAULT_COLLAPSE_TICKS = 80;
    private static final double DEFAULT_BURST_DAMAGE = 6.0;
    private static final int HIT_COOLDOWN = 16;

    private static final String[] EFFECT_SOUNDS = {
        "BLOCK_PORTAL_AMBIENT",
        "BLOCK_RESPAWN_ANCHOR_CHARGE",
        "ENTITY_ENDERMAN_TELEPORT",
        "ENTITY_PLAYER_HURT",
        "BLOCK_RESPAWN_ANCHOR_SET_SPAWN",
        "BLOCK_BEACON_POWER_SELECT",
        "BLOCK_PORTAL_TRIGGER",
        "ENTITY_GENERIC_EXPLODE",
        "BLOCK_RESPAWN_ANCHOR_DEPLETE"
    };

    private static final Color VOID_CORE = Color.fromRGB(8, 4, 18);
    private static final Color VOID_EDGE = Color.fromRGB(42, 18, 72);
    private static final Color VOID_GLOW = Color.fromRGB(120, 70, 180);
    private static final Color VOID_SPARK = Color.fromRGB(200, 160, 255);

    private BlackHoleAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location center = context.location().clone().add(0.5, 1.1, 0.5);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("blackhole");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double suckRadius = perks == null
                ? DEFAULT_SUCK_RADIUS
                : Math.max(2.0, perks.getDouble("suck-radius", DEFAULT_SUCK_RADIUS));
        double suckStrength = perks == null
                ? DEFAULT_SUCK_STRENGTH
                : Math.max(0.05, perks.getDouble("suck-strength", DEFAULT_SUCK_STRENGTH));
        int collapseTicks = perks == null
                ? DEFAULT_COLLAPSE_TICKS
                : Math.max(30, perks.getInt("collapse-ticks", DEFAULT_COLLAPSE_TICKS));
        double burstDamage = perks == null
                ? DEFAULT_BURST_DAMAGE
                : Math.max(0.0, perks.getDouble("burst-damage", DEFAULT_BURST_DAMAGE));
        boolean levelsEnabled = perks == null || perks.getBoolean("levels.enabled", true);
        ConfigurationSection levels = perks == null ? null : perks.getConfigurationSection("levels");

        EffectDamageConfig damageCfg = context.config().effectDamage("blackhole");
        double tickDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 0.0;
        double damageRadius = damageCfg.enabled() ? Math.max(1.0, damageCfg.radius()) : suckRadius;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        AtomicInteger level = new AtomicInteger(1);
        AtomicInteger suckLeft = new AtomicInteger(collapseTicks);
        AtomicInteger tick = new AtomicInteger();
        Set<UUID> hitCooldown = new HashSet<>();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            stopBlackHoleSounds(center);
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(collapseTicks + 80L);

        visuals.sound("BLOCK_PORTAL_AMBIENT", center, 0.85f, 0.55f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", center, 0.7f, 0.65f);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopBlackHoleSounds(center);
                PerkActionBar.clear(killer);
                return false;
            }

            int current = tick.getAndIncrement();
            int lvl = level.get();
            double radius = levelRadius(lvl, suckRadius, levels);
            double strength = suckStrength * (0.85 + lvl * 0.2);

            int left = suckLeft.decrementAndGet();
            if (left <= 0) {
                collapseBurst(session, visuals, world, center, killer, victimId, radius, burstDamage, lvl);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            drawVoidSphere(visuals, center, radius, current, lvl);
            pullAndHit(
                    session,
                    visuals,
                    world,
                    center,
                    killer,
                    victimId,
                    radius,
                    strength,
                    tickDamage,
                    damageRadius,
                    level,
                    suckLeft,
                    collapseTicks,
                    levelsEnabled,
                    levels,
                    hitCooldown,
                    current);

            if (current % 14 == 0) {
                visuals.sound("BLOCK_PORTAL_AMBIENT", center, 0.35f + lvl * 0.08f, 0.45f + lvl * 0.05f);
            }
            if (current % 22 == 0) {
                visuals.sound("ENTITY_ENDERMAN_TELEPORT", center, 0.25f, 0.4f);
            }

            if (killer != null && killer.isOnline() && current % 3 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&8⬤ BLACKHOLE &8| &dS%d &8| &bSUCK &f%.1fs &8| &5R &f%.1f",
                                lvl,
                                left / 20.0,
                                radius));
            }

            if (current > collapseTicks * (MAX_LEVEL + 1) + 60) {
                collapseBurst(session, visuals, world, center, killer, victimId, radius, burstDamage, lvl);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }
            return true;
        });
    }

    private static void stopBlackHoleSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : EFFECT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static double levelRadius(int level, double base, ConfigurationSection levels) {
        int lvl = Math.max(1, Math.min(MAX_LEVEL, level));
        if (levels != null) {
            ConfigurationSection s = levels.getConfigurationSection("s" + lvl);
            if (s != null) {
                return Math.max(2.0, s.getDouble("radius", base * (0.75 + lvl * 0.25)));
            }
        }
        return base * (0.75 + lvl * 0.25);
    }

    private static void drawVoidSphere(VisualEffectService visuals, Location center, double radius, int tick, int level) {
        double pulse = 0.82 + 0.18 * Math.sin(tick * 0.18);
        double r = radius * 0.55 * pulse;
        int rings = 4 + level;
        int points = Math.max(6, ParticleScale.scale(8 + level * 2));
        for (int ring = 0; ring < rings; ring++) {
            double t = ring / (double) Math.max(1, rings - 1);
            double y = (t - 0.5) * r * 1.4;
            double ringR = Math.sin(Math.PI * t) * r;
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2.0 * i) / points + tick * 0.12 + ring * 0.4;
                Location p = center.clone().add(Math.cos(a) * ringR, y, Math.sin(a) * ringR);
                Color c = t < 0.35 ? VOID_CORE : (t < 0.7 ? VOID_EDGE : VOID_GLOW);
                visuals.dust(p, c, (float) (0.9 + t * 0.7 + level * 0.08), 1, 0.0, 0.0, 0.0, 0.0);
                if (i % 3 == 0) {
                    visuals.particle("END_ROD", p, 1, 0.0, 0.0, 0.0, 0.0, null);
                }
            }
        }
        
        if (tick % 2 == 0) {
            int crumbs = ParticleScale.scale(4 + level);
            for (int i = 0; i < crumbs; i++) {
                double a = Math.random() * Math.PI * 2.0;
                double dist = r * (0.4 + Math.random() * 0.9);
                Location p = center.clone().add(Math.cos(a) * dist, (Math.random() - 0.5) * r, Math.sin(a) * dist);
                visuals.dust(p, VOID_SPARK, 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
                visuals.particle("SMOKE", p, 1, 0.02, 0.02, 0.02, 0.0, null);
            }
        }
        visuals.dust(center, VOID_CORE, 1.8f + level * 0.15f, 3, 0.08, 0.08, 0.08, 0.0);
    }

    private static void pullAndHit(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location center,
            Player killer,
            UUID victimId,
            double radius,
            double strength,
            double tickDamage,
            double damageRadius,
            AtomicInteger level,
            AtomicInteger suckLeft,
            int collapseTicks,
            boolean levelsEnabled,
            ConfigurationSection levels,
            Set<UUID> hitCooldown,
            int tick) {
        double radiusSq = radius * radius;
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
            double distSq = player.getLocation().distanceSquared(center);
            if (distSq > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }

            Vector pull = center.toVector().subtract(player.getLocation().toVector().add(new Vector(0, 0.9, 0)));
            double dist = Math.sqrt(distSq);
            if (pull.lengthSquared() > 1.0e-6) {
                double fade = 1.0 - Math.min(1.0, dist / Math.max(0.1, radius));
                pull.normalize().multiply(strength * (0.45 + fade * 0.9));
                pull.setY(pull.getY() * 0.55 + 0.02);
                player.setVelocity(player.getVelocity().multiply(0.55).add(pull));
            }

            if (tick % 2 == 0) {
                Location trail = player.getLocation().clone().add(0, 1.0, 0);
                visuals.dust(trail, VOID_GLOW, 0.9f, 2, 0.05, 0.05, 0.05, 0.0);
                visuals.particle("PORTAL", trail, 2, 0.1, 0.1, 0.1, 0.0, null);
            }

            if (dist <= Math.min(damageRadius, radius * 0.55) && !hitCooldown.contains(player.getUniqueId())) {
                hitCooldown.add(player.getUniqueId());
                UUID id = player.getUniqueId();
                session.runLater(HIT_COOLDOWN, () -> hitCooldown.remove(id));

                if (tickDamage > 0.0) {
                    if (killer != null) {
                        player.damage(tickDamage * (0.7 + level.get() * 0.2), killer);
                    } else {
                        player.damage(tickDamage * (0.7 + level.get() * 0.2));
                    }
                }
                visuals.sound("ENTITY_PLAYER_HURT", player.getLocation(), 0.7f, 0.75f);
                visuals.particle("END_ROD", player.getLocation().add(0, 1, 0), 6, 0.2, 0.3, 0.2, 0.02, null);

                if (levelsEnabled && level.get() < MAX_LEVEL) {
                    level.incrementAndGet();
                    suckLeft.set(collapseTicks);
                    session.resetDeadline(collapseTicks + 80L);
                    visuals.sound("BLOCK_RESPAWN_ANCHOR_SET_SPAWN", center, 1.1f, 0.7f + level.get() * 0.1f);
                    visuals.sound("BLOCK_BEACON_POWER_SELECT", center, 0.8f, 1.4f);
                    visuals.dust(center, VOID_SPARK, 1.6f, 18, 0.6, 0.5, 0.6, 0.0);
                } else if (levelsEnabled) {
                    suckLeft.set(collapseTicks);
                    session.resetDeadline(collapseTicks + 80L);
                    visuals.sound("BLOCK_PORTAL_TRIGGER", center, 0.6f, 0.8f);
                }
            }
        }
    }

    private static void collapseBurst(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location center,
            Player killer,
            UUID victimId,
            double radius,
            double burstDamage,
            int level) {
        stopBlackHoleSounds(center);
        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 1.15f, 0.55f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_DEPLETE", center, 1.0f, 0.7f);
        visuals.sound("ENTITY_ENDERMAN_TELEPORT", center, 0.9f, 0.35f);
        visuals.particle("EXPLOSION", center, 2, 0.15, 0.15, 0.15, 0.0, null);
        visuals.particle("FLASH", center, 1, 0, 0, 0, 0, Color.WHITE);

        AtomicInteger step = new AtomicInteger();
        double startR = radius * 0.7;
        session.runTimer(0L, 1L, () -> {
            int s = step.getAndIncrement();
            if (s > 18) {
                stopBlackHoleSounds(center);
                double burstR = radius * (0.85 + level * 0.1);
                int ring = ParticleScale.scale(28 + level * 6);
                for (int i = 0; i < ring; i++) {
                    double a = (Math.PI * 2.0 * i) / ring;
                    Location p = center.clone().add(Math.cos(a) * burstR, 0.4, Math.sin(a) * burstR);
                    visuals.dust(p, VOID_SPARK, 1.4f, 1, 0.0, 0.0, 0.0, 0.0);
                    visuals.particle("END_ROD", p, 1, 0.0, 0.0, 0.0, 0.05, null);
                }
                visuals.dust(center, VOID_GLOW, 2.0f, 24, 0.8, 0.6, 0.8, 0.0);

                if (burstDamage > 0.0) {
                    double rSq = burstR * burstR;
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
                        if (player.getLocation().distanceSquared(center) > rSq) {
                            continue;
                        }
                        if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                            continue;
                        }
                        if (killer != null) {
                            player.damage(burstDamage, killer);
                        } else {
                            player.damage(burstDamage);
                        }
                        Vector away = player.getLocation().toVector().subtract(center.toVector());
                        away.setY(0);
                        if (away.lengthSquared() < 0.01) {
                            away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
                        }
                        away.normalize().multiply(0.9 + level * 0.15).setY(0.45);
                        player.setVelocity(away);
                    }
                }
                return false;
            }
            double r = startR * (1.0 - s / 18.0);
            int pts = ParticleScale.scale(14);
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI * 2.0 * i) / pts + s * 0.35;
                Location p = center.clone().add(Math.cos(a) * r, Math.sin(s * 0.4 + i) * 0.25, Math.sin(a) * r);
                visuals.dust(p, VOID_EDGE, 1.1f, 1, 0.0, 0.0, 0.0, 0.0);
                visuals.particle("PORTAL", p, 1, 0.0, 0.0, 0.0, 0.0, null);
            }
            return true;
        });
    }
}
