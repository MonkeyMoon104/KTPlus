package com.monkey.ktplus.effects.list.puppetstrings.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PuppetStringsAnimation {
    private static final int DEFAULT_MAX_PUPPETS = 3;
    private static final double DEFAULT_LIFT_HEIGHT = 5.0;
    private static final int DEFAULT_CONTROL_TICKS = 60;
    private static final double DEFAULT_DROP_DAMAGE = 6.0;
    private static final double DEFAULT_STRING_BREAK_RADIUS = 10.0;
    private static final int ATTACH_TICKS = 18;
    private static final int DROP_TICKS = 22;
    private static final int BREAK_TICKS = 16;

    private static final Color STRING_GOLD = Color.fromRGB(255, 220, 120);
    private static final Color STRING_ROSE = Color.fromRGB(255, 120, 160);
    private static final Color STRING_GLOW = Color.fromRGB(200, 255, 255);
    private static final Color SLAM = Color.fromRGB(180, 60, 40);

    private PuppetStringsAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("puppetstrings");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int maxPuppets = perks == null
                ? DEFAULT_MAX_PUPPETS
                : Math.max(1, perks.getInt("max-puppets", DEFAULT_MAX_PUPPETS));
        double liftHeight = perks == null
                ? DEFAULT_LIFT_HEIGHT
                : Math.max(2.0, perks.getDouble("lift-height", DEFAULT_LIFT_HEIGHT));
        int controlTicks = perks == null
                ? DEFAULT_CONTROL_TICKS
                : Math.max(20, perks.getInt("control-ticks", DEFAULT_CONTROL_TICKS));
        double dropDamageBase = perks == null
                ? DEFAULT_DROP_DAMAGE
                : Math.max(0.0, perks.getDouble("drop-damage", DEFAULT_DROP_DAMAGE));
        double breakRadius = perks == null
                ? DEFAULT_STRING_BREAK_RADIUS
                : Math.max(4.0, perks.getDouble("string-break-radius", DEFAULT_STRING_BREAK_RADIUS));

        EffectDamageConfig damageCfg = context.config().effectDamage("puppetstrings");
        final double dropDamage = damageCfg.enabled()
                ? Math.max(dropDamageBase, damageCfg.value())
                : dropDamageBase;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        Location skyAnchor = origin.clone().add(0, liftHeight + 6.0, 0);
        List<Player> targets = findNearby(world, origin, killer, victimId, breakRadius, session, maxPuppets);
        List<Puppet> puppets = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            Player p = targets.get(i);
            double angle = (Math.PI * 2.0 * i) / Math.max(1, targets.size()) + 0.2;
            Location anchor = skyAnchor.clone().add(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);
            puppets.add(new Puppet(p.getUniqueId(), p.getLocation().clone(), anchor, i));
        }

        AtomicInteger tick = new AtomicInteger();
        int total = ATTACH_TICKS + controlTicks + DROP_TICKS + BREAK_TICKS;
        boolean[] slammed = {false};

        session.onCleanup(() -> PerkActionBar.clear(killer));
        session.resetDeadline(total + 20L);

        visuals.sound("ENTITY_ALLAY_AMBIENT_WITH_ITEM", skyAnchor, 0.8f, 1.3f);
        visuals.sound("BLOCK_NOTE_BLOCK_CHIME", origin, 0.7f, 1.5f);

        double finalDrop = dropDamage;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                PerkActionBar.clear(killer);
                return false;
            }

            if (current < ATTACH_TICKS) {
                double progress = (current + 1) / (double) ATTACH_TICKS;
                for (Puppet puppet : puppets) {
                    Player player = org.bukkit.Bukkit.getPlayer(puppet.id);
                    if (player == null || !player.isOnline() || player.isDead()) {
                        continue;
                    }
                    puppet.base = player.getLocation().clone();
                    drawString(visuals, puppet.anchor, player.getLocation().add(0, 1.6, 0), progress, current, puppet.index);
                    visuals.dust(player.getLocation().add(0, 1.6, 0), STRING_GLOW, 1.1f, 2, 0.05, 0.05, 0.05, 0.0);
                }
                if (current % 4 == 0) {
                    visuals.sound("ENTITY_FISHING_BOBBER_RETRIEVE", skyAnchor, 0.5f, 1.4f);
                }
                showBar(killer, "ATTACH", (int) (progress * 100), puppets.size());
            } else if (current < ATTACH_TICKS + controlTicks) {
                int local = current - ATTACH_TICKS;
                double liftProgress = Math.min(1.0, local / 18.0);
                for (Puppet puppet : puppets) {
                    Player player = org.bukkit.Bukkit.getPlayer(puppet.id);
                    if (player == null || !player.isOnline() || player.isDead()) {
                        continue;
                    }
                    if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                        continue;
                    }
                    double bob = Math.sin(local * 0.28 + puppet.index * 1.3) * 0.35;
                    double sway = Math.sin(local * 0.17 + puppet.index) * 0.25;
                    double targetY = puppet.base.getY() + liftHeight * liftProgress + bob;
                    Location want = puppet.base.clone().add(sway, 0, Math.cos(local * 0.14 + puppet.index) * 0.2);
                    want.setY(targetY);

                    Vector delta = want.toVector().subtract(player.getLocation().toVector());
                    if (delta.lengthSquared() > 0.0001) {
                        double step = Math.min(0.45, delta.length() * 0.35);
                        player.setVelocity(delta.normalize().multiply(step).setY(Math.max(-0.05, Math.min(0.55, delta.getY() * 0.25))));
                    }

                    drawString(visuals, puppet.anchor, player.getLocation().add(0, 1.7, 0), 1.0, current, puppet.index);
                    
                    if (local % 2 == 0) {
                        visuals.particle(
                                "END_ROD",
                                player.getLocation().add(0, 1.2, 0),
                                1,
                                0.25,
                                0.4,
                                0.25,
                                0.0,
                                null);
                        visuals.dust(
                                player.getLocation().add(0, 0.2, 0),
                                STRING_ROSE,
                                0.8f,
                                1,
                                0.15,
                                0.05,
                                0.15,
                                0.0);
                    }
                }
                if (local % 12 == 0) {
                    visuals.sound("BLOCK_NOTE_BLOCK_HARP", skyAnchor, 0.4f, 1.6f);
                }
                showBar(killer, "CONTROL", (int) ((local + 1) * 100.0 / controlTicks), puppets.size());
            } else if (current < ATTACH_TICKS + controlTicks + DROP_TICKS) {
                int local = current - ATTACH_TICKS - controlTicks;
                if (!slammed[0] && local >= 8) {
                    slammed[0] = true;
                    for (Puppet puppet : puppets) {
                        Player player = org.bukkit.Bukkit.getPlayer(puppet.id);
                        if (player == null || !player.isOnline() || player.isDead()) {
                            continue;
                        }
                        if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                            continue;
                        }
                        player.setVelocity(new Vector(0, -1.35, 0));
                        Location at = player.getLocation();
                        visuals.sound("ENTITY_GENERIC_EXPLODE", at, 0.7f, 1.2f);
                        visuals.sound("ENTITY_PLAYER_HURT", at, 0.9f, 0.8f);
                        visuals.dust(at, SLAM, 1.6f, 14, 0.4, 0.15, 0.4, 0.0);
                        visuals.particle("CLOUD", at, ParticleScale.scale(12), 0.5, 0.2, 0.5, 0.02, null);
                        if (finalDrop > 0.0) {
                            if (killer != null) {
                                player.damage(finalDrop, killer);
                            } else {
                                player.damage(finalDrop);
                            }
                        }
                    }
                } else {
                    for (Puppet puppet : puppets) {
                        Player player = org.bukkit.Bukkit.getPlayer(puppet.id);
                        if (player == null || !player.isOnline() || player.isDead()) {
                            continue;
                        }
                        
                        double fray = 1.0 - local / (double) DROP_TICKS;
                        if (Math.random() < 0.65) {
                            drawString(
                                    visuals,
                                    puppet.anchor,
                                    player.getLocation().add(0, 1.5, 0),
                                    fray,
                                    current,
                                    puppet.index);
                        }
                    }
                }
                showBar(killer, "DROP", Math.min(100, local * 5), puppets.size());
            } else {
                int local = current - ATTACH_TICKS - controlTicks - DROP_TICKS;
                double fade = 1.0 - local / (double) BREAK_TICKS;
                for (Puppet puppet : puppets) {
                    Player player = org.bukkit.Bukkit.getPlayer(puppet.id);
                    Location end = player != null && player.isOnline()
                            ? player.getLocation().add(0, 1.2, 0)
                            : puppet.base.clone().add(0, 1, 0);
                    
                    int shards = ParticleScale.scale(6);
                    for (int i = 0; i < shards; i++) {
                        double t = Math.random();
                        Location p = lerp(puppet.anchor, end, t);
                        p.add((Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.4);
                        visuals.dust(p, STRING_GOLD, (float) (1.0 * fade), 1, 0.0, 0.0, 0.0, 0.0);
                        visuals.particle("CRIT", p, 1, 0.0, 0.0, 0.0, 0.0, null);
                    }
                }
                if (local == 0) {
                    visuals.sound("ENTITY_ITEM_BREAK", origin, 1.0f, 0.9f);
                    visuals.sound("BLOCK_NOTE_BLOCK_BASS", origin, 0.7f, 0.6f);
                }
                showBar(killer, "BREAK", (int) (fade * 100), puppets.size());
            }
            return true;
        });
    }

    private static void showBar(Player killer, String phase, int pct, int count) {
        if (killer == null || !killer.isOnline()) {
            return;
        }
        PerkActionBar.show(
                killer,
                String.format("&d🎀 PUPPETSTRINGS &8| &f%s &a%d%% &8| &7PUPPETS &f%d", phase, pct, count));
    }

    private static void drawString(
            VisualEffectService visuals, Location from, Location to, double progress, int tick, int index) {
        Location end = lerp(from, to, Math.max(0.05, Math.min(1.0, progress)));
        int points = Math.max(6, ParticleScale.scale(10));
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Location p = lerp(from, end, t);
            
            double sway = Math.sin(tick * 0.2 + t * 4.0 + index) * 0.08 * (1.0 - Math.abs(t - 0.5) * 2);
            p.add(sway, 0, sway * 0.6);
            Color c = t < 0.3 ? STRING_GLOW : (t < 0.7 ? STRING_GOLD : STRING_ROSE);
            visuals.dust(p, c, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 3 == 0) {
                visuals.particle("END_ROD", p, 1, 0.0, 0.0, 0.0, 0.0, null);
            }
        }
    }

    private static Location lerp(Location a, Location b, double t) {
        return a.clone().add(
                (b.getX() - a.getX()) * t,
                (b.getY() - a.getY()) * t,
                (b.getZ() - a.getZ()) * t);
    }

    private static List<Player> findNearby(
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            EffectSession session,
            int max) {
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
        if (found.size() > max) {
            return new ArrayList<>(found.subList(0, max));
        }
        return found;
    }

    private static final class Puppet {
        private final UUID id;
        private Location base;
        private final Location anchor;
        private final int index;

        private Puppet(UUID id, Location base, Location anchor, int index) {
            this.id = id;
            this.base = base.clone();
            this.anchor = anchor.clone();
            this.index = index;
        }
    }
}
