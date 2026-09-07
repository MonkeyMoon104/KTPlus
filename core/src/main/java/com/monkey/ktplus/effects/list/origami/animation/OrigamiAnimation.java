package com.monkey.ktplus.effects.list.origami.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.origami.animation.util.OrigamiDisplays;
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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class OrigamiAnimation {
    private static final int DEFAULT_SHEETS = 16;
    private static final int DEFAULT_FOLD = 30;
    private static final double DEFAULT_DAMAGE = 3.0;
    private static final int DEFAULT_WAVES = 3;
    private static final int TOTAL_TICKS = 240;
    private static final String[] SOUNDS = {
        "ITEM_BOOK_PAGE_TURN",
        "ITEM_BOOK_PUT",
        "ENTITY_PLAYER_ATTACK_SWEEP",
        "ENTITY_PLAYER_ATTACK_CRIT",
        "BLOCK_WOOL_BREAK"
    };

    private static final Color PAPER = Color.fromRGB(245, 245, 240);
    private static final Color INK = Color.fromRGB(40, 40, 50);
    private static final Color BLADE = Color.fromRGB(200, 210, 220);

    private OrigamiAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("origami");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int sheetCount = perks == null
                ? DEFAULT_SHEETS
                : Math.max(8, Math.min(24, perks.getInt("sheet-count", DEFAULT_SHEETS)));
        int foldTicks = perks == null
                ? DEFAULT_FOLD
                : Math.max(12, perks.getInt("fold-ticks", DEFAULT_FOLD));
        double bladeDamageBase = perks == null
                ? DEFAULT_DAMAGE
                : Math.max(0.5, perks.getDouble("blade-damage", DEFAULT_DAMAGE));
        int waves = perks == null
                ? DEFAULT_WAVES
                : Math.max(1, Math.min(5, perks.getInt("waves", DEFAULT_WAVES)));

        EffectDamageConfig damageCfg = context.config().effectDamage("origami");
        final double bladeDamage = damageCfg.enabled()
                ? Math.max(bladeDamageBase, damageCfg.value())
                : bladeDamageBase;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<Sheet> sheets = new ArrayList<>(sheetCount);
        for (int i = 0; i < sheetCount; i++) {
            double a = (Math.PI * 2.0 * i) / sheetCount;
            Location spawn = origin.clone().add(Math.cos(a) * 1.4, (i % 4) * 0.15, Math.sin(a) * 1.4);
            ItemDisplay display = OrigamiDisplays.spawnSheet(spawn);
            if (display != null) {
                session.trackEntity(display);
                sheets.add(new Sheet(display, spawn, i, a));
            }
        }

        List<Blade> blades = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        AtomicInteger waveIndex = new AtomicInteger(0);
        Set<UUID> hitCooldown = new HashSet<>();
        boolean[] finished = {false};
        boolean[] folded = {false};

        session.onCleanup(() -> {
            stopSounds(origin);
            clearSheets(sheets);
            clearBlades(blades);
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(TOTAL_TICKS + 30L);

        visuals.sound("ITEM_BOOK_PAGE_TURN", origin, 1.0f, 1.1f);
        visuals.sound("ITEM_BOOK_PUT", origin, 0.7f, 0.9f);

        double finalDamage = bladeDamage;
        int finalFold = foldTicks;
        int finalWaves = waves;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                clearSheets(sheets);
                clearBlades(blades);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL_TICKS) {
                finish(visuals, origin, sheets, blades, killer);
                finished[0] = true;
                return false;
            }

            if (!folded[0]) {
                double progress = Math.min(1.0, (current + 1) / (double) finalFold);
                for (Sheet sheet : sheets) {
                    sheet.angle += 0.08;
                    double r = 1.4 * (1.0 - progress * 0.55);
                    Location at = origin.clone().add(
                            Math.cos(sheet.angle) * r,
                            Math.sin(current * 0.15 + sheet.index) * 0.35 + progress * 0.4,
                            Math.sin(sheet.angle) * r);
                    float fold = (float) (progress * Math.PI * 0.9);
                    OrigamiDisplays.place(
                            sheet.display,
                            at,
                            0.55f - (float) progress * 0.15f,
                            (float) sheet.angle,
                            fold,
                            fold * 0.5f);
                    sheet.loc = at;
                }
                if (current % 2 == 0) {
                    visuals.dust(origin, PAPER, 1.1f, ParticleScale.scale(4), 0.5, 0.3, 0.5, 0.0);
                }
                if (current % 5 == 0) {
                    visuals.sound("ITEM_BOOK_PAGE_TURN", origin, 0.4f, 1.0f + (float) progress * 0.5f);
                }
                showBar(killer, "FOLD", (int) (progress * 100), waveIndex.get());
                if (progress >= 1.0) {
                    folded[0] = true;
                    transformToBlades(session, visuals, origin, sheets, blades);
                    visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", origin, 1.0f, 1.4f);
                }
                return true;
            }

            int waveLocal = current - finalFold;
            int perWave = 35;
            if (waveIndex.get() < finalWaves && waveLocal % perWave == 0) {
                hitCooldown.clear();
                launchWave(session, visuals, world, origin, killer, victimId, blades, waveIndex.getAndIncrement());
            }

            updateBlades(session, visuals, world, origin, killer, victimId, blades, finalDamage, hitCooldown);

            showBar(killer, "STORM", Math.min(100, waveIndex.get() * 100 / finalWaves), blades.size());
            if (waveIndex.get() >= finalWaves && blades.isEmpty() && waveLocal > finalWaves * perWave) {
                finish(visuals, origin, sheets, blades, killer);
                finished[0] = true;
                return false;
            }
            return true;
        });
    }

    private static void transformToBlades(
            EffectSession session,
            VisualEffectService visuals,
            Location origin,
            List<Sheet> sheets,
            List<Blade> blades) {
        for (Sheet sheet : sheets) {
            visuals.dust(sheet.loc, PAPER, 1.2f, 4, 0.15, 0.15, 0.15, 0.0);
            OrigamiDisplays.remove(sheet.display);
            ItemDisplay blade = OrigamiDisplays.spawnBlade(sheet.loc);
            if (blade != null) {
                session.trackEntity(blade);
                blades.add(new Blade(blade, sheet.loc, sheet.angle, sheet.index));
            }
        }
        sheets.clear();
        visuals.dust(origin, BLADE, 1.5f, ParticleScale.scale(16), 0.6, 0.4, 0.6, 0.0);
    }

    private static void launchWave(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<Blade> blades,
            int wave) {
        visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", origin, 0.9f, 1.2f + wave * 0.1f);
        visuals.sound("BLOCK_WOOL_BREAK", origin, 0.7f, 1.4f);
        List<Player> targets = findTargets(world, origin, killer, victimId, 20.0, session);
        int i = 0;
        for (Blade blade : blades) {
            if (blade.seeking) {
                continue;
            }
            blade.seeking = true;
            blade.age = 0;
            if (!targets.isEmpty()) {
                blade.targetId = targets.get((i + wave) % targets.size()).getUniqueId();
            }
            double a = blade.phase + wave * 0.4;
            blade.velocity = new Vector(Math.cos(a), 0.12, Math.sin(a)).multiply(0.55 + wave * 0.08);
            i++;
        }
        
        if (blades.size() < 8) {
            for (int n = 0; n < 6; n++) {
                double a = (Math.PI * 2.0 * n) / 6.0 + wave;
                Location spawn = origin.clone().add(Math.cos(a) * 0.8, 0.4, Math.sin(a) * 0.8);
                ItemDisplay display = OrigamiDisplays.spawnBlade(spawn);
                if (display == null) {
                    continue;
                }
                session.trackEntity(display);
                Blade blade = new Blade(display, spawn, a, n + wave * 10);
                blade.seeking = true;
                if (!targets.isEmpty()) {
                    blade.targetId = targets.get(n % targets.size()).getUniqueId();
                }
                blade.velocity = new Vector(Math.cos(a), 0.1, Math.sin(a)).multiply(0.6);
                blades.add(blade);
            }
        }
    }

    private static void updateBlades(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<Blade> blades,
            double damage,
            Set<UUID> hitCooldown) {
        Iterator<Blade> it = blades.iterator();
        while (it.hasNext()) {
            Blade blade = it.next();
            if (blade.display == null || !blade.display.isValid() || blade.display.isDead()) {
                it.remove();
                continue;
            }
            blade.age++;
            if (!blade.seeking) {
                blade.phase += 0.15;
                Location at = origin.clone().add(
                        Math.cos(blade.phase) * 1.6,
                        0.5 + Math.sin(blade.age * 0.2) * 0.3,
                        Math.sin(blade.phase) * 1.6);
                OrigamiDisplays.place(blade.display, at, 0.45f, blade.age * 0.4f, blade.age * 0.2f, blade.age * 0.3f);
                blade.loc = at;
                continue;
            }

            Player target = blade.targetId == null ? null : org.bukkit.Bukkit.getPlayer(blade.targetId);
            if (target == null || !target.isOnline() || target.isDead()) {
                target = findNearest(world, blade.loc, killer, victimId, 16.0, session);
                if (target != null) {
                    blade.targetId = target.getUniqueId();
                }
            }
            if (target != null) {
                Vector to = target.getLocation().clone().add(0, 1, 0).toVector().subtract(blade.loc.toVector());
                if (to.lengthSquared() > 0.01) {
                    blade.velocity = blade.velocity
                            .multiply(0.82)
                            .add(to.normalize().multiply(0.28));
                }
            }
            blade.loc.add(blade.velocity);
            OrigamiDisplays.place(
                    blade.display,
                    blade.loc,
                    0.4f,
                    blade.age * 0.55f,
                    blade.age * 0.3f,
                    blade.age * 0.4f);
            visuals.dust(blade.loc, PAPER, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);

            if (target != null && blade.loc.distanceSquared(target.getLocation().add(0, 1, 0)) < 1.3) {
                if (!hitCooldown.contains(target.getUniqueId())
                        && (killer == null || session.allowsWorldMutation(killer, target.getLocation()))) {
                    hitCooldown.add(target.getUniqueId());
                    if (killer != null) {
                        target.damage(damage, killer);
                    } else {
                        target.damage(damage);
                    }
                    visuals.dust(blade.loc, INK, 1.3f, 6, 0.2, 0.2, 0.2, 0.0);
                    visuals.sound("ENTITY_PLAYER_ATTACK_CRIT", blade.loc, 0.7f, 1.5f);
                }
                OrigamiDisplays.remove(blade.display);
                it.remove();
                continue;
            }
            if (blade.age > 55) {
                OrigamiDisplays.remove(blade.display);
                it.remove();
            }
        }
        if (hitCooldown.size() > 20) {
            hitCooldown.clear();
        }
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            List<Sheet> sheets,
            List<Blade> blades,
            Player killer) {
        visuals.dust(origin, PAPER, 1.4f, ParticleScale.scale(18), 0.7, 0.5, 0.7, 0.0);
        visuals.sound("ITEM_BOOK_PAGE_TURN", origin, 0.8f, 0.8f);
        stopSounds(origin);
        clearSheets(sheets);
        clearBlades(blades);
        PerkActionBar.clear(killer);
    }

    private static void clearSheets(List<Sheet> sheets) {
        for (Sheet sheet : sheets) {
            OrigamiDisplays.remove(sheet.display);
        }
        sheets.clear();
    }

    private static void clearBlades(List<Blade> blades) {
        for (Blade blade : blades) {
            OrigamiDisplays.remove(blade.display);
        }
        blades.clear();
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

    private static void showBar(Player killer, String phase, int pct, int wave) {
        if (killer == null || !killer.isOnline()) {
            return;
        }
        PerkActionBar.show(
                killer,
                String.format("&f✂ ORIGAMI &8| &f%s &b%d%% &8| &7WAVE &f%d", phase, pct, wave));
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

    private static Player findNearest(
            World world, Location origin, Player killer, UUID victimId, double radius, EffectSession session) {
        List<Player> list = findTargets(world, origin, killer, victimId, radius, session);
        return list.isEmpty() ? null : list.get(0);
    }

    private static final class Sheet {
        private final ItemDisplay display;
        private Location loc;
        private final int index;
        private double angle;

        private Sheet(ItemDisplay display, Location loc, int index, double angle) {
            this.display = display;
            this.loc = loc.clone();
            this.index = index;
            this.angle = angle;
        }
    }

    private static final class Blade {
        private final ItemDisplay display;
        private Location loc;
        private double phase;
        private final int index;
        private Vector velocity = new Vector();
        private UUID targetId;
        private boolean seeking;
        private int age;

        private Blade(ItemDisplay display, Location loc, double phase, int index) {
            this.display = display;
            this.loc = loc.clone();
            this.phase = phase;
            this.index = index;
        }
    }
}
