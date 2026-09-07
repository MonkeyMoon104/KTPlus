package com.monkey.ktplus.effects.list.arcade.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.arcade.animation.util.ArcadeDisplays;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ArcadeAnimation {
    private static final int DEFAULT_ENEMY_COUNT = 8;
    private static final int DEFAULT_WAVE_COUNT = 3;
    private static final int DEFAULT_HIT_SCORE = 4;
    private static final int DEFAULT_DURATION = 220;
    private static final double DEFAULT_SPEED = 0.28;
    private static final int TOTAL_TICKS = 260;
    private static final double HIT_DISTANCE = 1.15;

    private static final Material[] SPRITES = {
        Material.RED_CONCRETE_POWDER,
        Material.LIME_CONCRETE_POWDER,
        Material.CYAN_CONCRETE_POWDER,
        Material.YELLOW_CONCRETE_POWDER,
        Material.MAGENTA_CONCRETE_POWDER,
        Material.ORANGE_CONCRETE_POWDER,
        Material.PINK_CONCRETE_POWDER,
        Material.BLUE_CONCRETE_POWDER
    };

    private static final Color[] TRAILS = {
        Color.fromRGB(255, 60, 80),
        Color.fromRGB(80, 255, 100),
        Color.fromRGB(60, 220, 255),
        Color.fromRGB(255, 230, 60),
        Color.fromRGB(255, 80, 220),
        Color.fromRGB(255, 150, 40),
        Color.fromRGB(255, 140, 180),
        Color.fromRGB(80, 120, 255)
    };

    private ArcadeAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("arcade");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int enemyCount = perks == null
                ? DEFAULT_ENEMY_COUNT
                : Math.max(3, perks.getInt("enemy-count", DEFAULT_ENEMY_COUNT));
        int waveCount = perks == null
                ? DEFAULT_WAVE_COUNT
                : Math.max(1, perks.getInt("wave-count", DEFAULT_WAVE_COUNT));
        int hitScoreToUpgrade = perks == null
                ? DEFAULT_HIT_SCORE
                : Math.max(1, perks.getInt("hit-score-to-upgrade", DEFAULT_HIT_SCORE));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(80, perks.getInt("duration-ticks", DEFAULT_DURATION));
        double enemySpeed = perks == null
                ? DEFAULT_SPEED
                : Math.max(0.12, perks.getDouble("enemy-speed", DEFAULT_SPEED));

        EffectDamageConfig damageCfg = context.config().effectDamage("arcade");
        double damageValue = damageCfg.enabled() ? Math.max(0.5, damageCfg.value()) : 3.0;
        double damageRadius = damageCfg.enabled() ? Math.max(1.0, damageCfg.radius()) : 3.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        ItemDisplay cabinet = ArcadeDisplays.spawnCabinet(origin.clone().add(0, 0.7, 0));
        if (cabinet != null) {
            session.trackEntity(cabinet);
        }

        List<Invader> invaders = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        AtomicInteger wave = new AtomicInteger(1);
        AtomicInteger score = new AtomicInteger();
        AtomicInteger waveHits = new AtomicInteger();
        Set<UUID> hitCooldown = new HashSet<>();
        boolean[] finished = {false};
        int[] spawnCooldown = {0};

        session.onCleanup(() -> {
            ArcadeDisplays.remove(cabinet);
            for (Invader inv : invaders) {
                ArcadeDisplays.remove(inv.display);
            }
            invaders.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_NOTE_BLOCK_PLING", origin, 1.3f, 0.8f);
        visuals.sound("BLOCK_NOTE_BLOCK_CHIME", origin, 1.0f, 1.2f);
        spawnWave(session, visuals, origin, invaders, enemyCount, wave.get(), enemySpeed);
        session.resetDeadline(Math.max(TOTAL_TICKS, durationTicks) + 40L);

        int maxTicks = Math.max(TOTAL_TICKS, durationTicks);
        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= maxTicks) {
                finish(visuals, origin, cabinet, invaders, killer, score.get());
                finished[0] = true;
                return false;
            }

            float cabYaw = current * 0.08f;
            float bob = (float) (0.7 + Math.sin(current * 0.15) * 0.08);
            ArcadeDisplays.place(cabinet, origin.clone().add(0, bob, 0), 0.9f + wave.get() * 0.05f, cabYaw, 0.0f);
            if (current % 5 == 0) {
                visuals.dust(origin.clone().add(0, 1.4, 0), TRAILS[current % TRAILS.length], 1.1f, 3, 0.2, 0.2, 0.2, 0.0);
                visuals.particle("NOTE", origin.clone().add(0, 1.6, 0), 2, 0.25, 0.2, 0.25, 0.4, null);
            }
            if (current % 12 == 0) {
                visuals.sound("BLOCK_NOTE_BLOCK_HARP", origin, 0.35f, 0.9f + (wave.get() % 5) * 0.12f);
            }

            updateInvaders(
                    session,
                    visuals,
                    world,
                    origin,
                    killer,
                    victimId,
                    invaders,
                    current,
                    enemySpeed * (1.0 + (wave.get() - 1) * 0.12),
                    damageValue * (0.9 + wave.get() * 0.1),
                    damageRadius,
                    hitCooldown,
                    score,
                    waveHits);

            if (invaders.isEmpty()) {
                if (spawnCooldown[0]-- <= 0) {
                    if (wave.get() < waveCount) {
                        wave.incrementAndGet();
                        visuals.sound("ENTITY_PLAYER_LEVELUP", origin, 0.9f, 1.2f);
                        visuals.sound("BLOCK_NOTE_BLOCK_PLING", origin, 1.2f, 1.5f);
                        spawnWave(session, visuals, origin, invaders, enemyCount + wave.get(), wave.get(), enemySpeed);
                        waveHits.set(0);
                        spawnCooldown[0] = 10;
                    } else if (current > durationTicks - 40) {
                        finish(visuals, origin, cabinet, invaders, killer, score.get());
                        finished[0] = true;
                        return false;
                    } else {
                        spawnWave(session, visuals, origin, invaders, enemyCount, wave.get(), enemySpeed);
                        spawnCooldown[0] = 18;
                    }
                }
            }

            if (waveHits.get() >= hitScoreToUpgrade && wave.get() < waveCount) {
                waveHits.set(0);
                wave.incrementAndGet();
                visuals.sound("ENTITY_PLAYER_LEVELUP", origin, 1.0f, 1.35f);
                visuals.sound("BLOCK_BEACON_POWER_SELECT", origin, 0.7f, 1.4f);
                visuals.dust(origin.clone().add(0, 1.5, 0), Color.fromRGB(255, 255, 120), 1.6f, 18, 0.6, 0.5, 0.6, 0.0);
                
                spawnWave(session, visuals, origin, invaders, Math.max(3, enemyCount / 2), wave.get(), enemySpeed * 1.15);
            }

            if (current % 3 == 0 && killer != null && killer.isOnline()) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&e Arcade &8| &aWAVE &f%d&8/&f%d &8| &bSCORE &f%d &8| &dHITS &f%d&8/&f%d &8| &6INV &f%d",
                                wave.get(),
                                waveCount,
                                score.get(),
                                waveHits.get(),
                                hitScoreToUpgrade,
                                invaders.size()));
            }
            return true;
        });
    }

    private static void spawnWave(
            EffectSession session,
            VisualEffectService visuals,
            Location origin,
            List<Invader> invaders,
            int count,
            int wave,
            double speed) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }
        visuals.sound("BLOCK_NOTE_BLOCK_PLING", origin, 1.0f, 0.6f + wave * 0.15f);
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2.0 * i) / count + wave * 0.3;
            double r = 5.5 + (i % 3) * 0.8 + wave * 0.4;
            Location start = origin.clone().add(Math.cos(a) * r, 2.2 + (i % 4) * 0.35, Math.sin(a) * r);
            Material mat = SPRITES[i % SPRITES.length];
            ItemDisplay display = ArcadeDisplays.spawnEnemy(start, mat);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            boolean ghost = i % 3 == 0;
            invaders.add(new Invader(
                    display,
                    start,
                    i,
                    TRAILS[i % TRAILS.length],
                    ghost,
                    a,
                    speed * (0.85 + (i % 4) * 0.08),
                    8 + i * 2));
        }
    }

    private static void updateInvaders(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<Invader> invaders,
            int tick,
            double baseSpeed,
            double damage,
            double damageRadius,
            Set<UUID> hitCooldown,
            AtomicInteger score,
            AtomicInteger waveHits) {
        Iterator<Invader> it = invaders.iterator();
        while (it.hasNext()) {
            Invader inv = it.next();
            if (inv.display == null || !inv.display.isValid() || inv.display.isDead()) {
                it.remove();
                continue;
            }
            inv.age++;
            if (inv.age < inv.launchDelay) {
                
                double zig = Math.sin(tick * 0.25 + inv.index) * 0.35;
                Location idle = inv.loc.clone().add(zig, Math.sin(tick * 0.2 + inv.index) * 0.1, -zig * 0.5);
                ArcadeDisplays.place(inv.display, idle, inv.ghost ? 0.38f : 0.45f, (float) (tick * 0.2), 0.2f);
                visuals.dust(idle, inv.trail, 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
                continue;
            }

            Player target = findNearest(world, inv.loc, killer, victimId, 22.0, session);
            if (target == null) {
                
                Vector to = origin.toVector().subtract(inv.loc.toVector());
                if (to.lengthSquared() > 0.01) {
                    to.normalize().multiply(baseSpeed * 0.7);
                    inv.loc.add(to);
                }
                if (inv.loc.distanceSquared(origin) < 1.5) {
                    ArcadeDisplays.remove(inv.display);
                    it.remove();
                    continue;
                }
            } else {
                Location aim = target.getLocation().clone().add(0, 0.9, 0);
                Vector to = aim.toVector().subtract(inv.loc.toVector());
                double dist = to.length();
                if (dist < HIT_DISTANCE) {
                    if (!hitCooldown.contains(target.getUniqueId())
                            && (killer == null || session.allowsWorldMutation(killer, target.getLocation()))) {
                        hitCooldown.add(target.getUniqueId());
                        UUID id = target.getUniqueId();
                        session.runLater(10, () -> hitCooldown.remove(id));
                        if (killer != null) {
                            target.damage(damage, killer);
                        } else {
                            target.damage(damage);
                        }
                        score.addAndGet(100 + inv.index * 10);
                        waveHits.incrementAndGet();
                        visuals.sound("BLOCK_NOTE_BLOCK_PLING", aim, 1.1f, 1.6f);
                        visuals.sound("ENTITY_PLAYER_HURT", aim, 0.7f, 1.2f);
                        visuals.dust(aim, inv.trail, 1.5f, 10, 0.3, 0.3, 0.3, 0.0);
                        visuals.particle("CRIT", aim, ParticleScale.scale(8), 0.2, 0.2, 0.2, 0.1, null);
                    }
                    ArcadeDisplays.remove(inv.display);
                    it.remove();
                    continue;
                }
                if (to.lengthSquared() > 1.0e-6) {
                    to.normalize();
                    
                    Vector side = to.clone().crossProduct(new Vector(0, 1, 0));
                    if (side.lengthSquared() > 1.0e-6) {
                        side.normalize();
                        double zigAmp = inv.ghost ? 0.35 : 0.22;
                        to.add(side.multiply(Math.sin(inv.age * 0.28 + inv.phase) * zigAmp));
                    }
                    double diveBoost = inv.age > 40 ? 1.35 : 1.0;
                    inv.loc.add(to.multiply(Math.min(baseSpeed * inv.speedMul * diveBoost, dist * 0.35)));
                }
            }

            float pitch = inv.ghost ? (float) Math.sin(inv.age * 0.3) * 0.5f : -0.35f;
            ArcadeDisplays.place(
                    inv.display,
                    inv.loc,
                    inv.ghost ? 0.4f : 0.48f,
                    (float) (inv.phase + inv.age * 0.25),
                    pitch);
            visuals.dust(inv.loc, inv.trail, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
            if (inv.age % 2 == 0) {
                visuals.dust(inv.loc.clone().add(0, -0.15, 0), inv.trail, 0.55f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        if (tick % 20 == 0 && damageRadius > 0) {
            
        }
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            ItemDisplay cabinet,
            List<Invader> invaders,
            Player killer,
            int score) {
        visuals.sound("BLOCK_NOTE_BLOCK_CHIME", origin, 1.2f, 0.7f);
        visuals.sound("ENTITY_FIREWORK_ROCKET_TWINKLE", origin, 0.9f, 1.1f);
        visuals.particle("FIREWORK", origin.clone().add(0, 1.5, 0), ParticleScale.scale(20), 0.6, 0.6, 0.6, 0.05, null);
        for (Invader inv : invaders) {
            if (inv.display != null && inv.display.isValid()) {
                visuals.dust(inv.display.getLocation(), inv.trail, 1.2f, 5, 0.15, 0.15, 0.15, 0.0);
            }
            ArcadeDisplays.remove(inv.display);
        }
        invaders.clear();
        ArcadeDisplays.remove(cabinet);
        if (killer != null && killer.isOnline()) {
            PerkActionBar.show(killer, String.format("&e★ GAME OVER &8| &bFINAL SCORE &f%d", score));
            sessionClearLater(killer);
        } else {
            PerkActionBar.clear(killer);
        }
    }

    private static void sessionClearLater(Player killer) {
        
        PerkActionBar.clear(killer);
    }

    private static Player findNearest(
            World world, Location core, Player killer, UUID victimId, double range, EffectSession session) {
        double best = range * range;
        Player nearest = null;
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
            double d = player.getLocation().distanceSquared(core);
            if (d > best) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            best = d;
            nearest = player;
        }
        return nearest;
    }

    private static final class Invader {
        private final ItemDisplay display;
        private final Location loc;
        private final int index;
        private final Color trail;
        private final boolean ghost;
        private final double phase;
        private final double speedMul;
        private final int launchDelay;
        private int age;

        private Invader(
                ItemDisplay display,
                Location loc,
                int index,
                Color trail,
                boolean ghost,
                double phase,
                double speedMul,
                int launchDelay) {
            this.display = display;
            this.loc = loc.clone();
            this.index = index;
            this.trail = trail;
            this.ghost = ghost;
            this.phase = phase;
            this.speedMul = speedMul;
            this.launchDelay = launchDelay;
        }
    }
}
