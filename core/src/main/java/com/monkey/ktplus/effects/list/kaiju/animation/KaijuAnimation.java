package com.monkey.ktplus.effects.list.kaiju.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.kaiju.animation.util.KaijuDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.Nullable;

public final class KaijuAnimation {
    private static final int DEFAULT_SHADOW_TICKS = 18;
    private static final double DEFAULT_STOMP_DAMAGE = 8.0;
    private static final double DEFAULT_SHOCKWAVE_RADIUS = 10.0;
    private static final int DEFAULT_DEBRIS_MAX = 20;
    private static final int DEFAULT_AFTERSHOCKS = 2;
    private static final int TOTAL_TICKS = 200;
    private static final int STOMP_FALL_TICKS = 8;
    private static final double START_HEIGHT = 14.0;
    private static final float HEAD_SCALE = 4.5f;
    private static final double REST_Y = HEAD_SCALE * 0.45;

    private static final Color DUST_DARK = Color.fromRGB(45, 40, 35);
    private static final Color DUST_MID = Color.fromRGB(95, 85, 70);
    private static final Color DUST_LIGHT = Color.fromRGB(150, 140, 120);
    private static final Color SHOCK = Color.fromRGB(200, 180, 140);

    private static final Material[] FALLBACK_DEBRIS = {
        Material.COBBLESTONE,
        Material.STONE,
        Material.DIRT,
        Material.GRAVEL
    };

    private KaijuAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("kaiju");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int shadowTicks = perks == null
                ? DEFAULT_SHADOW_TICKS
                : Math.max(6, perks.getInt("shadow-ticks", DEFAULT_SHADOW_TICKS));
        double stompDamage = perks == null
                ? DEFAULT_STOMP_DAMAGE
                : Math.max(2.0, perks.getDouble("stomp-damage", DEFAULT_STOMP_DAMAGE));
        double shockwaveRadius = perks == null
                ? DEFAULT_SHOCKWAVE_RADIUS
                : Math.max(4.0, perks.getDouble("shockwave-radius", DEFAULT_SHOCKWAVE_RADIUS));
        int debrisMax = perks == null
                ? DEFAULT_DEBRIS_MAX
                : Math.max(4, perks.getInt("debris-max", DEFAULT_DEBRIS_MAX));
        int aftershockCount = perks == null
                ? DEFAULT_AFTERSHOCKS
                : Math.max(0, perks.getInt("aftershock-count", DEFAULT_AFTERSHOCKS));

        EffectDamageConfig damageCfg = context.config().effectDamage("kaiju");
        double damageValue = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : stompDamage;
        double damageRadius = damageCfg.enabled() ? Math.max(3.0, damageCfg.radius()) : shockwaveRadius;

        Player killer = context.killer();
        Player victim = context.victim() instanceof Player player ? player : null;
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        ItemDisplay foot = KaijuDisplays.spawnVictimHead(
                origin.clone().add(0, START_HEIGHT, 0), victim, victimId);
        if (foot != null) {
            session.trackEntity(foot);
        }

        List<Debris> debris = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        AtomicInteger aftershocksLeft = new AtomicInteger(aftershockCount);
        Set<UUID> hitOnce = new HashSet<>();
        boolean[] stomped = {false};
        boolean[] finished = {false};
        double[] lastY = {START_HEIGHT};
        float[] lastScale = {HEAD_SCALE};
        int[] nextAftershock = {shadowTicks + STOMP_FALL_TICKS + 35};
        int descentTicks = shadowTicks + STOMP_FALL_TICKS;

        session.onCleanup(() -> {
            KaijuDisplays.remove(foot);
            for (Debris d : debris) {
                KaijuDisplays.remove(d.display);
            }
            debris.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_WARDEN_SONIC_BOOM", origin, 0.45f, 0.35f);
        visuals.sound("ENTITY_ENDER_DRAGON_GROWL", origin, 0.85f, 0.35f);
        session.resetDeadline(TOTAL_TICKS + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL_TICKS) {
                finish(visuals, origin, foot, debris, killer);
                finished[0] = true;
                return false;
            }

            if (current < shadowTicks) {
                drawShadow(visuals, origin, current, shadowTicks, shockwaveRadius);
                if (current % 6 == 0) {
                    visuals.sound("BLOCK_GRASS_STEP", origin, 0.7f, 0.4f);
                }
                if (current % 3 == 0 && killer != null && killer.isOnline()) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&8🦖 KAIJU &8| &7SHADOW &f%d&8/&f%d &8| &cSTOMP INCOMING",
                                    current,
                                    shadowTicks));
                }
                
                placeDescendingHead(foot, origin, current, descentTicks, lastY, lastScale);
                return true;
            }

            int fallAge = current - shadowTicks;
            if (!stomped[0]) {
                if (fallAge < STOMP_FALL_TICKS) {
                    placeDescendingHead(foot, origin, current, descentTicks, lastY, lastScale);
                    if (fallAge % 3 == 0) {
                        visuals.sound(
                                "ENTITY_PLAYER_ATTACK_CRIT",
                                origin.clone().add(0, lastY[0], 0),
                                0.5f,
                                0.5f);
                    }
                    return true;
                }

                stomped[0] = true;
                lastY[0] = REST_Y;
                lastScale[0] = HEAD_SCALE + 1.0f;
                KaijuDisplays.placeHead(foot, origin.clone().add(0, REST_Y, 0), lastScale[0], 0.0f);
                doStomp(
                        session,
                        visuals,
                        world,
                        origin,
                        killer,
                        victimId,
                        foot,
                        debris,
                        damageValue,
                        damageRadius,
                        shockwaveRadius,
                        debrisMax,
                        hitOnce);
                if (killer != null && killer.isOnline()) {
                    PerkActionBar.show(killer, "&8🦖 KAIJU &8| &cSTOMP &fIMPACT &8| &6SHOCKWAVE");
                }
                return true;
            }

            updateDebris(visuals, debris);
            drawResidualCrater(visuals, origin, current, shockwaveRadius);

            if (aftershocksLeft.get() > 0 && current >= nextAftershock[0]) {
                aftershocksLeft.decrementAndGet();
                nextAftershock[0] = current + 28 + (int) (Math.random() * 16);
                doAftershock(
                        session,
                        visuals,
                        world,
                        origin,
                        killer,
                        victimId,
                        shockwaveRadius * (0.55 + Math.random() * 0.25),
                        damageValue * 0.55,
                        debris,
                        Math.max(4, debrisMax / 2));
                if (killer != null && killer.isOnline()) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&8🦖 KAIJU &8| &eAFTERSHOCK &f%d &8| &7LEFT &f%d",
                                    aftershockCount - aftershocksLeft.get(),
                                    aftershocksLeft.get()));
                }
            }

            if (foot != null) {
                int settleAge = current - shadowTicks - STOMP_FALL_TICKS;
                float sink = Math.max(HEAD_SCALE * 0.95f, lastScale[0] - settleAge * 0.008f);
                lastScale[0] = sink;
                lastY[0] = REST_Y;
                KaijuDisplays.placeHead(foot, origin.clone().add(0, REST_Y, 0), sink, 0.0f);
            }

            if (current > shadowTicks + STOMP_FALL_TICKS + 20 + aftershockCount * 40
                    && debris.isEmpty()
                    && aftershocksLeft.get() <= 0) {
                finish(visuals, origin, foot, debris, killer);
                finished[0] = true;
                return false;
            }
            return true;
        });
    }

    private static void placeDescendingHead(
            @Nullable ItemDisplay foot,
            Location origin,
            int current,
            int descentTicks,
            double[] lastY,
            float[] lastScale) {
        if (foot == null) {
            return;
        }
        double t = Math.min(1.0, (current + 1) / (double) Math.max(1, descentTicks));
        
        double ease = t * t * t;
        double y = START_HEIGHT * (1.0 - ease) + REST_Y * ease;
        if (y > lastY[0]) {
            y = lastY[0];
        }
        lastY[0] = y;
        float scale = HEAD_SCALE + (float) ease * 0.9f;
        lastScale[0] = scale;
        
        float yaw = (float) (current * 0.015);
        KaijuDisplays.placeHead(foot, origin.clone().add(0, y, 0), scale, yaw);
    }

    private static void drawShadow(
            VisualEffectService visuals, Location origin, int current, int shadowTicks, double radius) {
        double progress = (current + 1) / (double) shadowTicks;
        double r = radius * (0.35 + progress * 0.75);
        int points = ParticleScale.scale(18 + (int) (progress * 16));
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points + current * 0.03;
            double rr = r * (0.55 + (i % 5) * 0.1);
            Location p = origin.clone().add(Math.cos(a) * rr, 0.08, Math.sin(a) * rr);
            Color c = i % 3 == 0 ? DUST_DARK : (i % 3 == 1 ? DUST_MID : DUST_LIGHT);
            visuals.dust(p, c, 1.5f + (float) progress * 0.6f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        int fill = ParticleScale.scale(10 + (int) (progress * 12));
        for (int i = 0; i < fill; i++) {
            double a = Math.random() * Math.PI * 2.0;
            double rr = Math.random() * r * 0.85;
            Location p = origin.clone().add(Math.cos(a) * rr, 0.05, Math.sin(a) * rr);
            visuals.dust(p, DUST_DARK, 1.2f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        visuals.particle("CLOUD", origin.clone().add(0, 0.3, 0), ParticleScale.scale(4), r * 0.3, 0.05, r * 0.3, 0.0, null);
    }

    private static void doStomp(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            ItemDisplay foot,
            List<Debris> debris,
            double damage,
            double damageRadius,
            double shockwaveRadius,
            int debrisMax,
            Set<UUID> hitOnce) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 1.5f, 0.45f);
        visuals.sound("ENTITY_WARDEN_SONIC_BOOM", origin, 0.9f, 0.55f);
        visuals.sound("ENTITY_IRON_GOLEM_ATTACK", origin, 1.2f, 0.45f);
        visuals.sound("BLOCK_ANVIL_LAND", origin, 0.8f, 0.4f);
        visuals.particle("EXPLOSION", origin.clone().add(0, 0.5, 0), ParticleScale.scale(6), 1.2, 0.3, 1.2, 0.0, null);
        visuals.dust(origin.clone().add(0, 0.4, 0), SHOCK, 2.0f, 40, 1.5, 0.4, 1.5, 0.0);
        visuals.particle("CLOUD", origin.clone().add(0, 0.6, 0), ParticleScale.scale(45), 2.0, 0.5, 2.0, 0.05, null);

        for (int ring = 0; ring < 4; ring++) {
            double r = shockwaveRadius * (0.25 + ring * 0.25);
            int pts = ParticleScale.scale(16 + ring * 4);
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI * 2.0 * i) / pts;
                Location p = origin.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                visuals.dust(p, ring % 2 == 0 ? DUST_MID : DUST_LIGHT, 1.6f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        spawnDebris(session, visuals, origin, debris, debrisMax, 0.9);
        flingPlayers(session, world, origin, killer, victimId, damage, damageRadius, hitOnce, 1.35);
        KaijuDisplays.placeHead(foot, origin.clone().add(0, REST_Y, 0), HEAD_SCALE + 1.0f, 0.0f);
    }

    private static void doAftershock(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            double damage,
            List<Debris> debris,
            int debrisCount) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.85f, 0.7f);
        visuals.sound("BLOCK_STONE_BREAK", origin, 1.0f, 0.55f);
        visuals.dust(origin.clone().add(0, 0.3, 0), DUST_MID, 1.7f, 22, radius * 0.25, 0.2, radius * 0.25, 0.0);
        int pts = ParticleScale.scale(20);
        for (int i = 0; i < pts; i++) {
            double a = (Math.PI * 2.0 * i) / pts;
            Location p = origin.clone().add(Math.cos(a) * radius, 0.12, Math.sin(a) * radius);
            visuals.dust(p, SHOCK, 1.4f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        spawnDebris(session, visuals, origin, debris, debrisCount, 0.65);
        Set<UUID> tmp = new HashSet<>();
        flingPlayers(session, world, origin, killer, victimId, damage, radius, tmp, 0.85);
    }

    private static void spawnDebris(
            EffectSession session,
            VisualEffectService visuals,
            Location origin,
            List<Debris> debris,
            int count,
            double power) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }
        List<Block> samples = KaijuDisplays.sampleRipBlocks(world, origin, 3.5, Math.max(count * 3, count));
        for (int i = 0; i < count; i++) {
            double a = Math.random() * Math.PI * 2.0;
            double dist = 0.8 + Math.random() * 2.2;
            Location spawnAt = origin.clone().add(Math.cos(a) * dist, 0.6, Math.sin(a) * dist);
            Material mat;
            if (i < samples.size()) {
                mat = samples.get(i).getType();
            } else if (!samples.isEmpty()) {
                mat = samples.get(i % samples.size()).getType();
            } else {
                mat = FALLBACK_DEBRIS[i % FALLBACK_DEBRIS.length];
            }
            ItemDisplay display = KaijuDisplays.spawnDebris(spawnAt, mat);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            Vector vel = new Vector(Math.cos(a), 0.55 + Math.random() * 0.55, Math.sin(a))
                    .multiply(power * (0.45 + Math.random() * 0.55));
            debris.add(new Debris(display, spawnAt, vel));
            visuals.dust(spawnAt, DUST_LIGHT, 1.0f, 3, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private static void updateDebris(VisualEffectService visuals, List<Debris> debris) {
        Iterator<Debris> it = debris.iterator();
        while (it.hasNext()) {
            Debris d = it.next();
            if (d.display == null || !d.display.isValid() || d.display.isDead()) {
                it.remove();
                continue;
            }
            d.age++;
            d.velocity.setY(d.velocity.getY() - 0.05);
            d.loc.add(d.velocity);
            float spin = d.age * 0.4f;
            KaijuDisplays.placeDebris(d.display, d.loc, 0.5f + (d.age % 5) * 0.02f, spin, spin * 0.5f, spin * 0.3f);
            if (d.age % 2 == 0) {
                visuals.dust(d.loc, DUST_MID, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (d.age > 35 || d.velocity.getY() < -0.1 && d.loc.getY() <= d.spawnY + 0.1) {
                visuals.dust(d.loc, DUST_DARK, 1.1f, 4, 0.15, 0.1, 0.15, 0.0);
                KaijuDisplays.remove(d.display);
                it.remove();
            }
        }
    }

    private static void drawResidualCrater(
            VisualEffectService visuals, Location origin, int tick, double radius) {
        if (tick % 2 != 0) {
            return;
        }
        int pts = ParticleScale.scale(10);
        double r = radius * 0.55;
        for (int i = 0; i < pts; i++) {
            double a = tick * 0.04 + (Math.PI * 2.0 * i) / pts;
            Location p = origin.clone().add(Math.cos(a) * r, 0.08, Math.sin(a) * r);
            visuals.dust(p, DUST_DARK, 1.1f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void flingPlayers(
            EffectSession session,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double damage,
            double radius,
            Set<UUID> hitOnce,
            double knockPower) {
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
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (hitOnce.contains(player.getUniqueId())) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            hitOnce.add(player.getUniqueId());
            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            Vector away = player.getLocation().toVector().subtract(origin.toVector());
            away.setY(0.0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(knockPower).setY(0.55 + knockPower * 0.2);
            player.setVelocity(away);
        }
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            ItemDisplay foot,
            List<Debris> debris,
            Player killer) {
        visuals.sound("BLOCK_STONE_BREAK", origin, 0.8f, 0.5f);
        visuals.particle("CLOUD", origin.clone().add(0, 1, 0), ParticleScale.scale(20), 1.2, 0.6, 1.2, 0.02, null);
        KaijuDisplays.remove(foot);
        for (Debris d : debris) {
            KaijuDisplays.remove(d.display);
        }
        debris.clear();
        PerkActionBar.clear(killer);
    }

    private static final class Debris {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final double spawnY;
        private int age;

        private Debris(ItemDisplay display, Location loc, Vector velocity) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.spawnY = loc.getY();
        }
    }
}
