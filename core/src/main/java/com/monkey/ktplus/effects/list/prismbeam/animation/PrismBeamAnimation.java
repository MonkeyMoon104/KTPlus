package com.monkey.ktplus.effects.list.prismbeam.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.prismbeam.animation.util.PrismBeamDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
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
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PrismBeamAnimation {
    private static final int DEFAULT_CHARGE = 28;
    private static final int DEFAULT_BEAMS = 3;
    private static final double DEFAULT_LEAD = 0.35;
    private static final double DEFAULT_DAMAGE = 5.0;
    private static final int FIRE_TICKS = 55;
    private static final String[] SOUNDS = {
        "BLOCK_BEACON_ACTIVATE",
        "BLOCK_BEACON_AMBIENT",
        "BLOCK_AMETHYST_BLOCK_CHIME",
        "ENTITY_GUARDIAN_ATTACK",
        "BLOCK_RESPAWN_ANCHOR_CHARGE"
    };

    private static final Color[] BEAM_COLORS = {
        Color.fromRGB(255, 70, 90),
        Color.fromRGB(80, 220, 120),
        Color.fromRGB(70, 140, 255),
        Color.fromRGB(255, 210, 60),
        Color.fromRGB(200, 90, 255)
    };
    private static final Material[] LENS_MATS = {
        Material.REDSTONE,
        Material.EMERALD,
        Material.LAPIS_LAZULI,
        Material.GOLD_NUGGET,
        Material.AMETHYST_SHARD
    };

    private PrismBeamAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("prismbeam");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int chargeTicks = perks == null
                ? DEFAULT_CHARGE
                : Math.max(12, perks.getInt("charge-ticks", DEFAULT_CHARGE));
        int beamCount = perks == null
                ? DEFAULT_BEAMS
                : Math.max(1, perks.getInt("beam-count", DEFAULT_BEAMS));
        double trackLead = perks == null
                ? DEFAULT_LEAD
                : Math.max(0.05, perks.getDouble("track-lead", DEFAULT_LEAD));
        double damageBase = perks == null
                ? DEFAULT_DAMAGE
                : Math.max(0.5, perks.getDouble("damage", DEFAULT_DAMAGE));

        EffectDamageConfig damageCfg = context.config().effectDamage("prismbeam");
        final double damage = damageCfg.enabled()
                ? Math.max(damageBase, damageCfg.value())
                : damageBase;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        BlockDisplay prism = PrismBeamDisplays.spawnPrism(origin);
        if (prism != null) {
            session.trackEntity(prism);
        }

        List<ItemDisplay> lenses = new ArrayList<>(beamCount);
        for (int i = 0; i < beamCount; i++) {
            ItemDisplay lens = PrismBeamDisplays.spawnLens(origin, LENS_MATS[i % LENS_MATS.length]);
            if (lens != null) {
                session.trackEntity(lens);
                lenses.add(lens);
            }
        }

        List<Beam> beams = new ArrayList<>(beamCount);
        for (int i = 0; i < beamCount; i++) {
            beams.add(new Beam(i, BEAM_COLORS[i % BEAM_COLORS.length]));
        }

        AtomicInteger tick = new AtomicInteger();
        Set<UUID> hitCooldown = new HashSet<>();
        boolean[] finished = {false};
        int total = chargeTicks + FIRE_TICKS + 20;

        session.onCleanup(() -> {
            stopSounds(origin);
            PrismBeamDisplays.remove(prism);
            for (ItemDisplay lens : lenses) {
                PrismBeamDisplays.remove(lens);
            }
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(total + 25L);

        visuals.sound("BLOCK_BEACON_ACTIVATE", origin, 1.0f, 1.4f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", origin, 0.9f, 1.2f);

        double finalDamage = damage;
        double finalLead = trackLead;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                cleanup(prism, lenses);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                finish(visuals, origin, prism, lenses, killer);
                finished[0] = true;
                return false;
            }

            PrismBeamDisplays.placePrism(prism, origin, 0.85f + (float) Math.sin(current * 0.2) * 0.08f, current * 0.08f);

            if (current < chargeTicks) {
                double progress = (current + 1) / (double) chargeTicks;
                for (int i = 0; i < lenses.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / beamCount + current * 0.12;
                    Location at = origin.clone().add(Math.cos(a) * (0.6 + progress * 0.5), 0.2, Math.sin(a) * (0.6 + progress * 0.5));
                    PrismBeamDisplays.placeLens(lenses.get(i), at, 0.35f + (float) progress * 0.25f, current * 0.2f + i);
                }
                drawCharge(visuals, origin, progress, current);
                if (current % 6 == 0) {
                    visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 0.45f, 1.0f + (float) progress * 0.6f);
                }
                showBar(killer, "CHARGE", (int) (progress * 100), 0);
            } else {
                int local = current - chargeTicks;
                List<Player> targets = findTargets(world, origin, killer, victimId, 28.0, session, beamCount);
                int hits = 0;
                for (int i = 0; i < beams.size(); i++) {
                    Beam beam = beams.get(i);
                    Player target = targets.isEmpty() ? null : targets.get(i % targets.size());
                    Location aim = aimPoint(origin, target, killer, finalLead);
                    beam.dir = aim.toVector().subtract(origin.toVector());
                    if (beam.dir.lengthSquared() < 0.01) {
                        double a = (Math.PI * 2.0 * i) / beamCount + 0.4;
                        beam.dir = new Vector(Math.cos(a), 0.05, Math.sin(a));
                    }
                    beam.dir.normalize();
                    beam.length = Math.min(30.0, origin.distance(aim) + 1.5);

                    if (i < lenses.size()) {
                        Location lensAt = origin.clone().add(beam.dir.clone().multiply(0.9));
                        PrismBeamDisplays.placeLens(lenses.get(i), lensAt, 0.55f, current * 0.25f + i);
                    }

                    drawBeam(visuals, origin, beam, local);
                    if (local % 4 == 0 && hitAlongBeam(
                            session, visuals, world, origin, beam, killer, victimId, finalDamage, hitCooldown)) {
                        hits++;
                    }
                }
                if (local % 8 == 0) {
                    visuals.sound("ENTITY_GUARDIAN_ATTACK", origin, 0.55f, 1.5f);
                    visuals.sound("BLOCK_BEACON_AMBIENT", origin, 0.35f, 1.6f);
                }
                showBar(killer, "TRACK", Math.min(100, local * 2), hits);
                if (local >= FIRE_TICKS) {
                    finish(visuals, origin, prism, lenses, killer);
                    finished[0] = true;
                    return false;
                }
            }
            return true;
        });
    }

    private static void drawCharge(VisualEffectService visuals, Location origin, double progress, int tick) {
        int pts = ParticleScale.scale(8 + (int) (progress * 8));
        for (int i = 0; i < pts; i++) {
            double a = tick * 0.2 + (Math.PI * 2.0 * i) / pts;
            Location p = origin.clone().add(Math.cos(a) * (0.5 + progress), 0.3, Math.sin(a) * (0.5 + progress));
            visuals.dust(p, BEAM_COLORS[i % BEAM_COLORS.length], 1.1f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        visuals.particle("END_ROD", origin, ParticleScale.scale(2), 0.15, 0.2, 0.15, 0.0, null);
    }

    private static void drawBeam(VisualEffectService visuals, Location origin, Beam beam, int local) {
        int steps = ParticleScale.scale(18 + (int) (beam.length * 0.6));
        for (int s = 0; s < steps; s++) {
            double t = s / (double) Math.max(1, steps - 1);
            Location p = origin.clone().add(beam.dir.clone().multiply(beam.length * t));
            float size = 1.0f + (float) Math.sin(local * 0.3 + s * 0.2) * 0.2f;
            visuals.dust(p, beam.color, size, 1, 0.0, 0.0, 0.0, 0.0);
            if (s % 3 == 0) {
                visuals.dust(p, Color.fromRGB(255, 255, 255), 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        Location tip = origin.clone().add(beam.dir.clone().multiply(beam.length));
        visuals.particle("CRIT", tip, ParticleScale.scale(3), 0.1, 0.1, 0.1, 0.02, null);
    }

    private static boolean hitAlongBeam(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Beam beam,
            Player killer,
            UUID victimId,
            double damage,
            Set<UUID> hitCooldown) {
        boolean any = false;
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
            if (hitCooldown.contains(player.getUniqueId())) {
                continue;
            }
            Location eye = player.getLocation().clone().add(0, 1.0, 0);
            Vector toPlayer = eye.toVector().subtract(origin.toVector());
            double along = toPlayer.dot(beam.dir);
            if (along < 0.4 || along > beam.length + 0.5) {
                continue;
            }
            Location closest = origin.clone().add(beam.dir.clone().multiply(along));
            if (closest.distanceSquared(eye) > 1.6) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            hitCooldown.add(player.getUniqueId());
            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            visuals.dust(eye, beam.color, 1.5f, 8, 0.2, 0.2, 0.2, 0.0);
            visuals.sound("ENTITY_PLAYER_HURT", eye, 0.6f, 1.3f);
            any = true;
        }
        
        if (hitCooldown.size() > 12) {
            hitCooldown.clear();
        }
        return any;
    }

    private static Location aimPoint(Location origin, Player target, Player killer, double lead) {
        if (target != null) {
            Location aim = target.getLocation().clone().add(0, 1.0, 0);
            Vector vel = target.getVelocity();
            aim.add(vel.clone().multiply(lead * 20.0));
            return aim;
        }
        if (killer != null) {
            return killer.getEyeLocation().clone().add(killer.getLocation().getDirection().multiply(18));
        }
        return origin.clone().add(1, 0, 0);
    }

    private static List<Player> findTargets(
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            EffectSession session,
            int needed) {
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
        if (found.size() > needed) {
            return new ArrayList<>(found.subList(0, needed));
        }
        return found;
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            BlockDisplay prism,
            List<ItemDisplay> lenses,
            Player killer) {
        visuals.dust(origin, Color.fromRGB(230, 210, 255), 1.5f, ParticleScale.scale(14), 0.5, 0.5, 0.5, 0.0);
        visuals.sound("BLOCK_AMETHYST_BLOCK_BREAK", origin, 0.9f, 1.3f);
        stopSounds(origin);
        cleanup(prism, lenses);
        PerkActionBar.clear(killer);
    }

    private static void cleanup(BlockDisplay prism, List<ItemDisplay> lenses) {
        PrismBeamDisplays.remove(prism);
        for (ItemDisplay lens : lenses) {
            PrismBeamDisplays.remove(lens);
        }
        lenses.clear();
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static void showBar(Player killer, String phase, int pct, int hits) {
        if (killer == null || !killer.isOnline()) {
            return;
        }
        PerkActionBar.show(
                killer,
                String.format("&d◆ PRISMBEAM &8| &f%s &b%d%% &8| &cLOCK &f%d", phase, pct, hits));
    }

    private static final class Beam {
        private final int index;
        private final Color color;
        private Vector dir = new Vector(1, 0, 0);
        private double length = 16.0;

        private Beam(int index, Color color) {
            this.index = index;
            this.color = color;
        }
    }
}
