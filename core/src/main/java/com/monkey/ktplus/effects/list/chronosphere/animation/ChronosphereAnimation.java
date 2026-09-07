package com.monkey.ktplus.effects.list.chronosphere.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.chronosphere.animation.util.ChronosphereDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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

public final class ChronosphereAnimation {
    private static final int TOTAL_TICKS = 300;
    private static final double DEFAULT_FREEZE_RADIUS = 9.0;
    private static final int DEFAULT_FREEZE_TICKS = 50;
    private static final int DEFAULT_SLASH_COUNT = 8;
    private static final double DEFAULT_SHATTER = 8.0;

    private static final Color TIME_CORE = Color.fromRGB(180, 230, 255);
    private static final Color TIME_EDGE = Color.fromRGB(90, 140, 220);
    private static final Color SLASH = Color.fromRGB(255, 240, 120);

    private static final String[] EFFECT_SOUNDS = {
        "BLOCK_BEACON_AMBIENT",
        "BLOCK_NOTE_BLOCK_CHIME",
        "ENTITY_ENDERMAN_TELEPORT",
        "BLOCK_AMETHYST_BLOCK_CHIME",
        "BLOCK_GLASS_BREAK",
        "ENTITY_PLAYER_ATTACK_SWEEP",
        "BLOCK_ENCHANTMENT_TABLE_USE"
    };

    private ChronosphereAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.2, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("chronosphere");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double freezeRadius = perks == null
                ? DEFAULT_FREEZE_RADIUS
                : Math.max(4.0, perks.getDouble("freeze-radius", DEFAULT_FREEZE_RADIUS));
        int freezeTicks = perks == null
                ? DEFAULT_FREEZE_TICKS
                : Math.max(20, perks.getInt("freeze-ticks", DEFAULT_FREEZE_TICKS));
        int slashCount = perks == null
                ? DEFAULT_SLASH_COUNT
                : Math.max(4, Math.min(16, perks.getInt("slash-count", DEFAULT_SLASH_COUNT)));
        double shatterDamage = perks == null
                ? DEFAULT_SHATTER
                : Math.max(2.0, perks.getDouble("shatter-damage", DEFAULT_SHATTER));

        EffectDamageConfig damageCfg = context.config().effectDamage("chronosphere");
        double damageValue = damageCfg.enabled() ? Math.max(2.0, damageCfg.value()) : shatterDamage;
        double damageRadius = damageCfg.enabled() ? Math.max(3.0, damageCfg.radius()) : freezeRadius;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        ItemDisplay clock = ChronosphereDisplays.spawnClock(origin, 3.2f);
        if (clock != null) {
            session.trackEntity(clock);
        }

        List<Slash> slashes = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        boolean[] shattered = {false};
        Set<UUID> frozen = new HashSet<>();

        session.onCleanup(() -> {
            stopSounds(origin);
            ChronosphereDisplays.remove(clock);
            for (Slash s : slashes) {
                ChronosphereDisplays.remove(s.display);
            }
            slashes.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_BEACON_AMBIENT", origin, 0.95f, 1.4f);
        visuals.sound("BLOCK_ENCHANTMENT_TABLE_USE", origin, 1.0f, 0.7f);
        session.resetDeadline(TOTAL_TICKS + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL_TICKS) {
                cleanup(visuals, origin, clock, slashes, killer);
                finished[0] = true;
                return false;
            }

            float spin = current * 0.12f;
            if (!shattered[0]) {
                ChronosphereDisplays.place(
                        clock, origin, 3.2f + (float) Math.sin(current * 0.1) * 0.25f, spin, 0.2f, spin * 0.3f);
                drawSphere(visuals, origin, freezeRadius, current);
            }

            if (current < freezeTicks) {
                freezeNearby(session, world, origin, killer, victimId, freezeRadius, frozen);
                if (current % 6 == 0) {
                    visuals.sound("BLOCK_NOTE_BLOCK_CHIME", origin, 0.5f, 1.6f);
                }
                if (killer != null && killer.isOnline() && current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&b⌛ CHRONOSPHERE &8| &3FREEZE &f%d&8/&f%d &8| &7LOCKED &f%d",
                                    current,
                                    freezeTicks,
                                    frozen.size()));
                }
                return true;
            }

            if (!shattered[0]) {
                shattered[0] = true;
                ChronosphereDisplays.remove(clock);
                spawnSlashes(session, visuals, origin, slashCount, slashes);
                visuals.sound("BLOCK_GLASS_BREAK", origin, 1.4f, 0.6f);
                visuals.sound("ENTITY_ENDERMAN_TELEPORT", origin, 1.1f, 0.5f);
                visuals.dust(origin, TIME_CORE, 2.2f, 50, freezeRadius * 0.25, 0.8, freezeRadius * 0.25, 0.0);
                hitNear(session, world, origin, killer, victimId, damageValue, damageRadius);
            }

            updateSlashes(
                    session, visuals, world, origin, killer, victimId, slashes, damageValue, damageRadius);

            if (shattered[0] && slashes.isEmpty()) {
                cleanup(visuals, origin, clock, slashes, killer);
                finished[0] = true;
                return false;
            }

            if (killer != null && killer.isOnline() && current % 3 == 0 && shattered[0]) {
                PerkActionBar.show(
                        killer,
                        String.format("&b⌛ CHRONOSPHERE &8| &eSHARDS &f%d", slashes.size()));
            }
            return true;
        });
    }

    private static void drawSphere(VisualEffectService visuals, Location origin, double radius, int tick) {
        int rings = 5;
        int pts = ParticleScale.scale(12);
        for (int ring = 0; ring < rings; ring++) {
            double t = ring / (double) (rings - 1);
            double y = (t - 0.5) * radius * 1.5;
            double r = Math.sin(Math.PI * t) * radius;
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI * 2.0 * i) / pts + tick * 0.04;
                Location p = origin.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                visuals.dust(p, ring % 2 == 0 ? TIME_CORE : TIME_EDGE, 1.25f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        visuals.dust(origin, TIME_CORE, 1.8f, ParticleScale.scale(4), 0.2, 0.2, 0.2, 0.0);
    }

    private static void freezeNearby(
            EffectSession session,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            Set<UUID> frozen) {
        double radiusSq = radius * radius;
        frozen.clear();
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
            frozen.add(player.getUniqueId());
            player.setVelocity(new Vector(0, 0, 0));
            player.setFallDistance(0.0f);
        }
    }

    private static void spawnSlashes(
            EffectSession session,
            VisualEffectService visuals,
            Location origin,
            int count,
            List<Slash> slashes) {
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2.0 * i) / count;
            Location spawn = origin.clone().add(Math.cos(a) * 1.2, 0.4, Math.sin(a) * 1.2);
            ItemDisplay shard = ChronosphereDisplays.spawnShard(spawn);
            if (shard == null) {
                continue;
            }
            session.trackEntity(shard);
            Vector vel = new Vector(Math.cos(a), 0.15, Math.sin(a)).multiply(0.55);
            slashes.add(new Slash(shard, spawn, vel, a));
            visuals.dust(spawn, SLASH, 1.4f, 4, 0.1, 0.1, 0.1, 0.0);
        }
        visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", origin, 1.2f, 0.8f);
    }

    private static void updateSlashes(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<Slash> slashes,
            double damage,
            double radius) {
        Iterator<Slash> it = slashes.iterator();
        while (it.hasNext()) {
            Slash slash = it.next();
            if (slash.display == null || !slash.display.isValid() || slash.display.isDead()) {
                it.remove();
                continue;
            }
            slash.age++;
            slash.loc.add(slash.velocity);
            ChronosphereDisplays.place(
                    slash.display,
                    slash.loc,
                    0.7f,
                    (float) slash.angle + slash.age * 0.35f,
                    slash.age * 0.2f,
                    slash.age * 0.15f);
            if (slash.age % 2 == 0) {
                visuals.dust(slash.loc, SLASH, 1.2f, 2, 0.05, 0.05, 0.05, 0.0);
            }
            if (slash.age == 8) {
                hitNear(session, world, slash.loc, killer, victimId, damage, 2.4);
            }
            if (slash.age > 35 || slash.loc.distanceSquared(origin) > radius * radius * 1.4) {
                visuals.dust(slash.loc, TIME_EDGE, 1.5f, 6, 0.2, 0.2, 0.2, 0.0);
                ChronosphereDisplays.remove(slash.display);
                it.remove();
            }
        }
    }

    private static void hitNear(
            EffectSession session,
            World world,
            Location at,
            Player killer,
            UUID victimId,
            double damage,
            double radius) {
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
        }
    }

    private static void cleanup(
            VisualEffectService visuals,
            Location origin,
            ItemDisplay clock,
            List<Slash> slashes,
            Player killer) {
        visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", origin, 1.0f, 0.8f);
        ChronosphereDisplays.remove(clock);
        for (Slash s : slashes) {
            ChronosphereDisplays.remove(s.display);
        }
        slashes.clear();
        stopSounds(origin);
        PerkActionBar.clear(killer);
    }

    private static void stopSounds(Location center) {
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

    private static final class Slash {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final double angle;
        private int age;

        private Slash(ItemDisplay display, Location loc, Vector velocity, double angle) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.angle = angle;
        }
    }
}
