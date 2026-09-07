package com.monkey.ktplus.effects.list.crownofash.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.crownofash.animation.util.CrownOfAshDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class CrownOfAshAnimation {
    private static final int DEFAULT_EMBERS = 14;
    private static final int DEFAULT_BURN = 4;
    private static final int TOTAL_TICKS = 240;
    private static final int ORBIT_TICKS = 40;
    private static final int MAX_LEVEL = 3;
    private static final String[] SOUNDS = {
        "BLOCK_FIRE_AMBIENT",
        "ENTITY_BLAZE_AMBIENT",
        "ENTITY_BLAZE_SHOOT",
        "BLOCK_FIRE_EXTINGUISH",
        "ITEM_FIRECHARGE_USE",
        "ENTITY_PLAYER_LEVELUP"
    };

    private static final Color ASH = Color.fromRGB(90, 85, 80);
    private static final Color EMBER = Color.fromRGB(255, 120, 40);
    private static final Color CORE = Color.fromRGB(255, 200, 80);
    private static final Color SMOKE = Color.fromRGB(50, 45, 40);

    private CrownOfAshAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.6, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("crownofash");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int emberCount = perks == null
                ? DEFAULT_EMBERS
                : Math.max(8, Math.min(24, perks.getInt("ember-count", DEFAULT_EMBERS)));
        boolean orbitThenSeek = perks == null || perks.getBoolean("orbit-then-seek", true);
        int burnSeconds = perks == null
                ? DEFAULT_BURN
                : Math.max(1, perks.getInt("burn-seconds", DEFAULT_BURN));
        boolean levelOnHit = perks == null || perks.getBoolean("level-on-hit", true);

        EffectDamageConfig damageCfg = context.config().effectDamage("crownofash");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 4.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<BlockDisplay> crown = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            double a = (Math.PI * 2.0 * i) / 8.0;
            Location spawn = origin.clone().add(Math.cos(a) * 0.55, 0.15, Math.sin(a) * 0.55);
            BlockDisplay point = CrownOfAshDisplays.spawnCrownPoint(spawn);
            if (point != null) {
                session.trackEntity(point);
                crown.add(point);
            }
        }

        List<Ember> embers = new ArrayList<>(emberCount);
        for (int i = 0; i < emberCount; i++) {
            double a = (Math.PI * 2.0 * i) / emberCount;
            Location spawn = origin.clone().add(Math.cos(a) * 1.1, 0.2, Math.sin(a) * 1.1);
            ItemDisplay display = CrownOfAshDisplays.spawnEmber(spawn);
            if (display != null) {
                session.trackEntity(display);
                embers.add(new Ember(display, spawn, i, a));
            }
        }

        AtomicInteger tick = new AtomicInteger();
        AtomicInteger level = new AtomicInteger(1);
        Set<UUID> hitOnce = new HashSet<>();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            stopSounds(origin);
            clearCrown(crown);
            clearEmbers(embers);
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(TOTAL_TICKS + 30L);

        visuals.sound("ITEM_FIRECHARGE_USE", origin, 0.9f, 0.7f);
        visuals.sound("ENTITY_BLAZE_AMBIENT", origin, 0.7f, 0.85f);

        double finalDamage = damage;
        int finalBurn = burnSeconds;
        boolean finalLevel = levelOnHit;
        boolean finalOrbit = orbitThenSeek;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                clearCrown(crown);
                clearEmbers(embers);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL_TICKS || embers.isEmpty() && current > ORBIT_TICKS + 40) {
                finish(visuals, origin, crown, embers, killer);
                finished[0] = true;
                return false;
            }

            int lvl = level.get();
            placeCrown(crown, origin, current, lvl);
            drawAshAura(visuals, origin, current, lvl);

            boolean seeking = !finalOrbit || current >= ORBIT_TICKS;
            Iterator<Ember> it = embers.iterator();
            while (it.hasNext()) {
                Ember ember = it.next();
                if (ember.display == null || !ember.display.isValid() || ember.display.isDead()) {
                    it.remove();
                    continue;
                }
                ember.age++;
                if (!seeking || ember.age < ORBIT_TICKS - ember.index % 5) {
                    ember.phase += 0.16 + lvl * 0.02;
                    double r = 1.0 + lvl * 0.15 + Math.sin(ember.phase) * 0.15;
                    ember.loc.setX(origin.getX() + Math.cos(ember.phase) * r);
                    ember.loc.setY(origin.getY() + 0.2 + Math.sin(ember.phase * 1.3) * 0.35);
                    ember.loc.setZ(origin.getZ() + Math.sin(ember.phase) * r);
                    CrownOfAshDisplays.placeEmber(ember.display, ember.loc, 0.3f + lvl * 0.05f, ember.age * 0.3f);
                    visuals.dust(ember.loc, EMBER, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
                    continue;
                }

                Player target = resolveTarget(world, ember, origin, killer, victimId, 18.0 + lvl * 2, session);
                if (target == null) {
                    ember.loc.add(Math.cos(ember.phase) * 0.08, 0.03, Math.sin(ember.phase) * 0.08);
                    CrownOfAshDisplays.placeEmber(ember.display, ember.loc, 0.28f, ember.age * 0.25f);
                    if (ember.age > ORBIT_TICKS + 50) {
                        CrownOfAshDisplays.remove(ember.display);
                        it.remove();
                    }
                    continue;
                }

                Location aim = target.getLocation().clone().add(0, 1.0, 0);
                if (ember.loc.distanceSquared(aim) < 1.2) {
                    boolean leveled = impact(
                            session,
                            visuals,
                            killer,
                            target,
                            ember,
                            finalDamage * (0.85 + lvl * 0.2),
                            finalBurn,
                            hitOnce,
                            level,
                            finalLevel);
                    CrownOfAshDisplays.remove(ember.display);
                    it.remove();
                    if (leveled) {
                        visuals.sound("ENTITY_PLAYER_LEVELUP", origin, 0.7f, 1.2f + level.get() * 0.1f);
                        visuals.dust(origin, CORE, 1.6f, ParticleScale.scale(12), 0.5, 0.4, 0.5, 0.0);
                    }
                    continue;
                }

                Vector to = aim.toVector().subtract(ember.loc.toVector());
                if (to.lengthSquared() > 0.01) {
                    ember.loc.add(to.normalize().multiply(0.35 + lvl * 0.05));
                }
                CrownOfAshDisplays.placeEmber(ember.display, ember.loc, 0.35f + lvl * 0.04f, ember.age * 0.4f);
                visuals.dust(ember.loc, EMBER, 0.9f, 1, 0.0, 0.0, 0.0, 0.0);
                visuals.particle("FLAME", ember.loc, 1, 0.0, 0.0, 0.0, 0.0, null);
            }

            if (current % 10 == 0) {
                visuals.sound("BLOCK_FIRE_AMBIENT", origin, 0.35f, 0.9f);
            }
            if (current % 18 == 0) {
                visuals.sound("ENTITY_BLAZE_AMBIENT", origin, 0.3f, 1.1f);
            }

            if (killer != null && killer.isOnline() && current % 3 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&6👑 CROWNOFASH &8| &cS%d &8| &eEMBERS &f%d &8| &7%s",
                                level.get(),
                                embers.size(),
                                seeking ? "SEEK" : "ORBIT"));
            }
            return true;
        });
    }

    private static void placeCrown(List<BlockDisplay> crown, Location origin, int tick, int level) {
        double radius = 0.5 + level * 0.08;
        for (int i = 0; i < crown.size(); i++) {
            double a = (Math.PI * 2.0 * i) / crown.size() + tick * 0.04;
            Location at = origin.clone().add(Math.cos(a) * radius, 0.1 + Math.sin(tick * 0.1 + i) * 0.08, Math.sin(a) * radius);
            CrownOfAshDisplays.placeCrown(crown.get(i), at, 0.2f + level * 0.03f, (float) a);
        }
    }

    private static void drawAshAura(VisualEffectService visuals, Location origin, int tick, int level) {
        visuals.dust(origin, ASH, 1.2f, ParticleScale.scale(4 + level), 0.4, 0.2, 0.4, 0.0);
        visuals.dust(origin.clone().add(0, 0.3, 0), CORE, 1.0f, ParticleScale.scale(2), 0.2, 0.15, 0.2, 0.0);
        if (tick % 2 == 0) {
            visuals.particle("SMOKE", origin.clone().add(0, 0.5, 0), ParticleScale.scale(3), 0.3, 0.2, 0.3, 0.01, null);
            visuals.particle("LAVA", origin, ParticleScale.scale(1), 0.2, 0.1, 0.2, 0.0, null);
        }
    }

    private static boolean impact(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Player target,
            Ember ember,
            double damage,
            int burnSeconds,
            Set<UUID> hitOnce,
            AtomicInteger level,
            boolean levelOnHit) {
        Location at = target.getLocation().clone().add(0, 1, 0);
        visuals.dust(at, EMBER, 1.5f, 10, 0.25, 0.25, 0.25, 0.0);
        visuals.dust(at, SMOKE, 1.2f, 6, 0.2, 0.2, 0.2, 0.0);
        visuals.sound("ENTITY_BLAZE_SHOOT", at, 0.7f, 1.3f);
        visuals.particle("FLAME", at, ParticleScale.scale(8), 0.2, 0.2, 0.2, 0.02, null);

        boolean leveled = false;
        if (killer != null && !session.allowsWorldMutation(killer, target.getLocation())) {
            return false;
        }
        if (!hitOnce.contains(target.getUniqueId())) {
            hitOnce.add(target.getUniqueId());
            if (killer != null) {
                target.damage(damage, killer);
            } else {
                target.damage(damage);
            }
            target.setFireTicks(Math.max(target.getFireTicks(), burnSeconds * 20));
            if (levelOnHit && level.get() < MAX_LEVEL) {
                level.incrementAndGet();
                leveled = true;
            }
        }
        return leveled;
    }

    private static Player resolveTarget(
            World world,
            Ember ember,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            EffectSession session) {
        if (ember.targetId != null) {
            Player locked = org.bukkit.Bukkit.getPlayer(ember.targetId);
            if (locked != null && locked.isOnline() && !locked.isDead() && locked.getWorld().equals(world)) {
                return locked;
            }
        }
        List<Player> found = findTargets(world, origin, killer, victimId, radius, session);
        if (found.isEmpty()) {
            return null;
        }
        Player pick = found.get(ember.index % found.size());
        ember.targetId = pick.getUniqueId();
        return pick;
    }

    private static List<Player> findTargets(
            World world, Location origin, Player killer, UUID victimId, double radius, EffectSession session) {
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
        return found;
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            List<BlockDisplay> crown,
            List<Ember> embers,
            Player killer) {
        visuals.dust(origin, ASH, 1.5f, ParticleScale.scale(16), 0.6, 0.4, 0.6, 0.0);
        visuals.sound("BLOCK_FIRE_EXTINGUISH", origin, 0.8f, 0.7f);
        stopSounds(origin);
        clearCrown(crown);
        clearEmbers(embers);
        PerkActionBar.clear(killer);
    }

    private static void clearCrown(List<BlockDisplay> crown) {
        for (BlockDisplay d : crown) {
            CrownOfAshDisplays.remove(d);
        }
        crown.clear();
    }

    private static void clearEmbers(List<Ember> embers) {
        for (Ember e : embers) {
            CrownOfAshDisplays.remove(e.display);
        }
        embers.clear();
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

    private static final class Ember {
        private final ItemDisplay display;
        private final Location loc;
        private final int index;
        private double phase;
        private UUID targetId;
        private int age;

        private Ember(ItemDisplay display, Location loc, int index, double phase) {
            this.display = display;
            this.loc = loc.clone();
            this.index = index;
            this.phase = phase;
        }
    }
}
