package com.monkey.ktplus.effects.list.cosmicfinale.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.cosmicfinale.animation.util.CosmicFinaleDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class CosmicFinaleAnimation {
    private static final int DEFAULT_STARS = 12;
    private static final int DEFAULT_PHASES = 3;
    private static final double DEFAULT_BEAM = 6.0;
    private static final int DEFAULT_DURATION = 280;
    private static final double CORE_START_Y = 18.0;
    private static final double CORE_DESCENT_SPEED = 0.55;

    private static final Color STAR = Color.fromRGB(230, 240, 255);
    private static final Color BEAM = Color.fromRGB(120, 180, 255);
    private static final Color NOVA = Color.fromRGB(255, 220, 160);
    private static final Color VOID = Color.fromRGB(40, 20, 80);

    private static final String[] EFFECT_SOUNDS = {
        "BLOCK_BEACON_AMBIENT",
        "BLOCK_BEACON_ACTIVATE",
        "BLOCK_END_PORTAL_SPAWN",
        "ENTITY_WITHER_SPAWN",
        "ENTITY_GENERIC_EXPLODE",
        "ENTITY_ENDER_DRAGON_DEATH",
        "BLOCK_RESPAWN_ANCHOR_CHARGE",
        "ENTITY_FIREWORK_ROCKET_BLAST"
    };

    private CosmicFinaleAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.2, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("cosmicfinale");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int starCount = perks == null
                ? DEFAULT_STARS
                : Math.max(1, perks.getInt("stars", DEFAULT_STARS));
        int phaseCount = perks == null
                ? DEFAULT_PHASES
                : Math.max(1, perks.getInt("phase-count", DEFAULT_PHASES));
        double beamDamage = perks == null
                ? DEFAULT_BEAM
                : Math.max(0.0, perks.getDouble("beam-damage", DEFAULT_BEAM));
        boolean finaleNova = perks == null || perks.getBoolean("finale-nova", true);
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(1, perks.getInt("duration-ticks", DEFAULT_DURATION));

        EffectDamageConfig damageCfg = context.config().effectDamage("cosmicfinale");
        double damageValue = damageCfg.enabled() ? Math.max(2.0, damageCfg.value()) : beamDamage;
        double damageRadius = damageCfg.enabled() ? Math.max(4.0, damageCfg.radius()) : 10.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        ItemDisplay core = CosmicFinaleDisplays.spawnCore(origin.clone().add(0, CORE_START_Y, 0), 2.4f);
        if (core != null) {
            session.trackEntity(core);
        }
        double[] coreY = {CORE_START_Y};
        boolean[] descending = {false};

        List<Star> constellation = new ArrayList<>();
        for (int i = 0; i < starCount; i++) {
            double a = (Math.PI * 2.0 * i) / starCount;
            double r = 6.0 + (i % 3) * 2.2;
            Location sky = origin.clone().add(Math.cos(a) * r, 12.0 + (i % 4) * 1.0, Math.sin(a) * r);
            ItemDisplay display = CosmicFinaleDisplays.spawnStar(sky, 1.1f + (i % 3) * 0.25f);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            constellation.add(new Star(display, sky, a, i));
        }

        List<FallingStar> falling = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        AtomicInteger phase = new AtomicInteger(0);
        boolean[] finished = {false};
        boolean[] novaDone = {false};
        int phaseLen = Math.max(1, (durationTicks - 55) / phaseCount);

        session.onCleanup(() -> {
            stopSounds(origin);
            CosmicFinaleDisplays.remove(core);
            for (Star s : constellation) {
                CosmicFinaleDisplays.remove(s.display);
            }
            for (FallingStar f : falling) {
                CosmicFinaleDisplays.remove(f.display);
            }
            constellation.clear();
            falling.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_END_PORTAL_SPAWN", origin, 1.0f, 0.7f);
        visuals.sound("BLOCK_BEACON_ACTIVATE", origin, 1.1f, 0.85f);
        session.resetDeadline(durationTicks + 60L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks) {
                finish(visuals, origin, core, constellation, falling, killer);
                finished[0] = true;
                return false;
            }

            if (!descending[0] && !novaDone[0] && current >= Math.max(40, durationTicks - 55)) {
                descending[0] = true;
                visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 1.2f, 0.55f);
            }

            if (descending[0] && !novaDone[0]) {
                coreY[0] = Math.max(1.2, coreY[0] - CORE_DESCENT_SPEED);
                CosmicFinaleDisplays.place(
                        core,
                        origin.clone().add(0, coreY[0], 0),
                        2.4f + (float) ((CORE_START_Y - coreY[0]) * 0.02),
                        current * 0.08f,
                        current * 0.05f);
                if (current % 2 == 0) {
                    visuals.dust(origin.clone().add(0, coreY[0], 0), BEAM, 1.5f, 4, 0.15, 0.15, 0.15, 0.0);
                }
                if (finaleNova && coreY[0] <= 1.25) {
                    novaDone[0] = true;
                    doNova(session, visuals, world, origin, killer, victimId, damageValue * 1.4, damageRadius);
                    CosmicFinaleDisplays.remove(core);
                }
            } else if (!novaDone[0]) {
                CosmicFinaleDisplays.place(
                        core,
                        origin.clone().add(0, coreY[0] + Math.sin(current * 0.05) * 0.4, 0),
                        2.4f,
                        current * 0.04f,
                        current * 0.02f);
            }

            drawConstellationLinks(visuals, constellation, current);
            pulseStars(visuals, constellation, current);

            int currentPhase = Math.min(phaseCount - 1, current / phaseLen);
            if (currentPhase != phase.get()) {
                phase.set(currentPhase);
                visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 1.1f, 0.6f + currentPhase * 0.15f);
                visuals.sound("ENTITY_WITHER_SPAWN", origin, 0.55f, 0.8f);
                launchPhaseStars(session, visuals, origin, constellation, falling, currentPhase, phaseCount);
            }

            if (current % 18 == 0 && current > 20 && !descending[0]) {
                fireBeam(session, visuals, world, origin, killer, victimId, constellation, damageValue, damageRadius * 0.45);
            }

            updateFalling(session, visuals, world, origin, killer, victimId, falling, damageValue, damageRadius);

            if (current % 12 == 0) {
                visuals.sound("BLOCK_BEACON_AMBIENT", origin.clone().add(0, coreY[0], 0), 0.5f, 1.3f);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&b✦ COSMIC FINALE &8| &3PHASE &f%d&8/&f%d &8| &eSTARS &f%d",
                                currentPhase + 1,
                                phaseCount,
                                constellation.size() + falling.size()));
            }
            return true;
        });
    }

    private static void pulseStars(VisualEffectService visuals, List<Star> stars, int tick) {
        for (Star star : stars) {
            if (star.display == null || !star.display.isValid()) {
                continue;
            }
            float pulse = 1.0f + (float) Math.sin(tick * 0.15 + star.index) * 0.25f;
            Location at = star.home.clone().add(0, Math.sin(tick * 0.08 + star.angle) * 0.35, 0);
            CosmicFinaleDisplays.place(star.display, at, star.baseScale * pulse, tick * 0.05f, tick * 0.03f);
            if (tick % 3 == 0) {
                visuals.dust(at, STAR, 1.3f, 2, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    private static void drawConstellationLinks(VisualEffectService visuals, List<Star> stars, int tick) {
        if (stars.size() < 2 || tick % 2 != 0) {
            return;
        }
        int pts = ParticleScale.scale(4);
        for (int i = 0; i < stars.size(); i++) {
            Star a = stars.get(i);
            Star b = stars.get((i + 1) % stars.size());
            for (int s = 1; s <= pts; s++) {
                double t = s / (double) (pts + 1);
                Location p = a.home.clone().add(
                        (b.home.getX() - a.home.getX()) * t,
                        (b.home.getY() - a.home.getY()) * t,
                        (b.home.getZ() - a.home.getZ()) * t);
                visuals.dust(p, BEAM, 1.0f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void launchPhaseStars(
            EffectSession session,
            VisualEffectService visuals,
            Location origin,
            List<Star> constellation,
            List<FallingStar> falling,
            int phase,
            int phaseCount) {
        int toLaunch = Math.max(2, constellation.size() / Math.max(1, phaseCount - phase));
        int launched = 0;
        Iterator<Star> it = constellation.iterator();
        while (it.hasNext() && launched < toLaunch) {
            Star star = it.next();
            if (star.display == null) {
                it.remove();
                continue;
            }
            Location start = star.home.clone();
            Vector vel = origin.clone().add(0, 1, 0).toVector().subtract(start.toVector());
            if (vel.lengthSquared() < 0.01) {
                vel = new Vector(0, -0.5, 0);
            }
            vel.normalize().multiply(1.15 + phase * 0.22);
            falling.add(new FallingStar(star.display, start, vel, star.baseScale));
            it.remove();
            launched++;
            visuals.sound("ENTITY_FIREWORK_ROCKET_BLAST", start, 0.7f, 0.9f);
            visuals.dust(start, STAR, 1.8f, 10, 0.3, 0.3, 0.3, 0.0);
        }
    }

    private static void updateFalling(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<FallingStar> falling,
            double damage,
            double radius) {
        Iterator<FallingStar> it = falling.iterator();
        while (it.hasNext()) {
            FallingStar star = it.next();
            if (star.display == null || !star.display.isValid()) {
                it.remove();
                continue;
            }
            star.age++;
            star.velocity.setY(star.velocity.getY() - 0.04);
            star.loc.add(star.velocity);
            CosmicFinaleDisplays.place(star.display, star.loc, star.scale, star.age * 0.2f, star.age * 0.1f);
            if (star.age % 2 == 0) {
                visuals.dust(star.loc, BEAM, 1.2f, 2, 0.05, 0.05, 0.05, 0.0);
            }
            if (star.loc.getY() <= origin.getY() + 1.2 || star.age > 40) {
                impact(session, visuals, world, star.loc, killer, victimId, damage, radius * 0.55);
                CosmicFinaleDisplays.remove(star.display);
                it.remove();
            }
        }
    }

    private static void fireBeam(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<Star> constellation,
            double damage,
            double radius) {
        if (constellation.isEmpty()) {
            return;
        }
        Star star = constellation.get((int) (Math.random() * constellation.size()));
        Location from = star.home.clone();
        Player target = nearest(world, origin, killer, victimId, 14.0);
        Location to = target != null ? target.getLocation().add(0, 1, 0) : origin.clone().add(0, 1, 0);
        int steps = ParticleScale.scale(12);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Location p = from.clone().add(
                    (to.getX() - from.getX()) * t,
                    (to.getY() - from.getY()) * t,
                    (to.getZ() - from.getZ()) * t);
            visuals.dust(p, BEAM, 1.4f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        visuals.sound("BLOCK_BEACON_ACTIVATE", to, 0.7f, 1.4f);
        impact(session, visuals, world, to, killer, victimId, damage, radius);
    }

    private static void doNova(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double damage,
            double radius) {
        visuals.sound("ENTITY_ENDER_DRAGON_DEATH", origin, 0.85f, 0.7f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 1.5f, 0.55f);
        visuals.sound("BLOCK_END_PORTAL_SPAWN", origin, 1.1f, 0.9f);
        visuals.dust(origin.clone().add(0, 2, 0), NOVA, 3.0f, 90, radius * 0.35, 1.5, radius * 0.35, 0.0);
        visuals.dust(origin.clone().add(0, 3, 0), STAR, 2.5f, 60, radius * 0.4, 1.2, radius * 0.4, 0.0);
        visuals.dust(origin.clone().add(0, 1, 0), VOID, 2.0f, 40, radius * 0.3, 0.8, radius * 0.3, 0.0);
        visuals.particle("EXPLOSION", origin.clone().add(0, 2, 0), ParticleScale.scale(12), 2.0, 1.2, 2.0, 0.0, null);
        impact(session, visuals, world, origin.clone().add(0, 1, 0), killer, victimId, damage, radius);
    }

    private static void impact(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location at,
            Player killer,
            UUID victimId,
            double damage,
            double radius) {
        visuals.dust(at, NOVA, 1.8f, 18, 0.4, 0.3, 0.4, 0.0);
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
            if (player.getLocation().distanceSquared(at) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            Vector away = player.getLocation().toVector().subtract(at.toVector());
            away.setY(0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(0.9).setY(0.45);
            player.setVelocity(away);
        }
    }

    private static Player nearest(World world, Location origin, Player killer, UUID victimId, double range) {
        double best = range * range;
        Player found = null;
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
            double d = player.getLocation().distanceSquared(origin);
            if (d < best) {
                best = d;
                found = player;
            }
        }
        return found;
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            ItemDisplay core,
            List<Star> constellation,
            List<FallingStar> falling,
            Player killer) {
        visuals.particle("CLOUD", origin.clone().add(0, 2, 0), ParticleScale.scale(30), 2.2, 1.5, 2.2, 0.02, null);
        CosmicFinaleDisplays.remove(core);
        for (Star s : constellation) {
            CosmicFinaleDisplays.remove(s.display);
        }
        for (FallingStar f : falling) {
            CosmicFinaleDisplays.remove(f.display);
        }
        constellation.clear();
        falling.clear();
        stopSounds(origin);
        PerkActionBar.clear(killer);
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 90.0 * 90.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : EFFECT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Star {
        private final ItemDisplay display;
        private final Location home;
        private final double angle;
        private final int index;
        private final float baseScale;

        private Star(ItemDisplay display, Location home, double angle, int index) {
            this.display = display;
            this.home = home.clone();
            this.angle = angle;
            this.index = index;
            this.baseScale = 1.1f + (index % 3) * 0.25f;
        }
    }

    private static final class FallingStar {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final float scale;
        private int age;

        private FallingStar(ItemDisplay display, Location loc, Vector velocity, float scale) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.scale = scale;
        }
    }
}
