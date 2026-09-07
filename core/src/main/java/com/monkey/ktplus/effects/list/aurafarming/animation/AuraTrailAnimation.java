package com.monkey.ktplus.effects.list.aurafarming.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.list.aurafarming.animation.util.AuraBoostApplier;
import com.monkey.ktplus.effects.list.aurafarming.animation.util.AuraHomingTrail;
import com.monkey.ktplus.effects.list.aurafarming.animation.util.AuraParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class AuraTrailAnimation {
    private AuraTrailAnimation() {}

    public static void start(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location origin) {
        if (origin.getWorld() == null) {
            return;
        }
        Player killer = context.killer();
        ConfigurationSection section = context.config().effectSection("aurafarming");
        Set<UUID> lockedTargets = ConcurrentHashMap.newKeySet();
        Set<UUID> levitatingTargets = ConcurrentHashMap.newKeySet();
        session.onCleanup(() -> PerkActionBar.clear(killer));
        session.resetDeadline(420L);
        session.runTimer(0L, 5L, () -> {
            if (!session.active() || killer == null || !killer.isOnline()) {
                PerkActionBar.clear(killer);
                return false;
            }
            if (lockedTargets.isEmpty() && levitatingTargets.isEmpty()) {
                return true;
            }
            PerkActionBar.show(
                    killer,
                    String.format(
                            "&b✧ AURA &8| &cLOCKED &f%d &8| &fLEVITATING &b%d",
                            lockedTargets.size(),
                            levitatingTargets.size()));
            return true;
        });
        List<Vector> directions = new ArrayList<>();
        directions.add(new Vector(-0.4, 1, -0.4).normalize());
        directions.add(new Vector(-0.6, 1, 0).normalize());
        directions.add(new Vector(-0.4, 1, 0.4).normalize());
        directions.add(new Vector(0, 1, 0).normalize());
        directions.add(new Vector(0.4, 1, 0.4).normalize());
        directions.add(new Vector(0.6, 1, 0).normalize());
        directions.add(new Vector(0.4, 1, -0.4).normalize());
        List<Location> currentPositions = new ArrayList<>();
        List<Boolean> trailEnded = new ArrayList<>();
        for (int i = 0; i < directions.size(); i++) {
            currentPositions.add(origin.clone());
            trailEnded.add(false);
        }
        AtomicInteger ticks = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (killer == null || !killer.isOnline()) {
                return false;
            }
            int current = ticks.getAndIncrement();
            if (current > 200) {
                return false;
            }
            for (int i = 0; i < currentPositions.size(); i++) {
                if (trailEnded.get(i)) {
                    continue;
                }
                Location currentLoc = currentPositions.get(i);
                Location killerLocation = killer.isOnline() ? killer.getLocation() : null;
                if (currentLoc.getWorld() == null
                        || killerLocation == null
                        || killerLocation.getWorld() == null
                        || !killerLocation.getWorld().equals(currentLoc.getWorld())) {
                    trailEnded.set(i, true);
                    continue;
                }
                if (current <= 80) {
                    Vector dir = directions.get(i).clone().multiply(0.15);
                    currentPositions.set(i, currentLoc.add(dir));
                } else if (current <= 120) {
                    Vector toKiller = killerLocation.clone().add(0, 1.5, 0).subtract(currentLoc).toVector().normalize().multiply(0.12);
                    currentPositions.set(i, currentLoc.add(toKiller));
                } else if (current > 120 && current <= 140) {
                    Location killerLoc = killerLocation.clone().add(0, 0.5, 0);
                    int ticksLeft = 140 - current + 1;
                    Vector toKiller = killerLoc.toVector().subtract(currentLoc.toVector()).multiply(1.0 / ticksLeft);
                    currentPositions.set(i, currentLoc.add(toKiller));
                    if (current == 140) {
                        killerLoc.getWorld().strikeLightningEffect(killerLoc);
                        int finalI = i;
                        AtomicInteger explosionTick = new AtomicInteger();
                        session.runTimer(0L, 2L, () -> {
                            if (explosionTick.getAndIncrement() > 10) {
                                return false;
                            }
                            AuraParticles.spawnExplosionEffect(visuals, currentPositions.get(finalI), explosionTick.get());
                            return true;
                        });
                        if (i == 0) {
                            BuiltInDamageService.apply(session, killer, killerLoc, context.config().effectDamage("aurafarming"));
                            AuraBoostApplier.apply(context, killer);
                            for (Player player : killerLoc.getWorld().getPlayers()) {
                                if (player.equals(killer)) {
                                    continue;
                                }
                                if (player.getLocation().distance(killer.getLocation()) <= 100) {
                                    AuraHomingTrail.start(
                                            session,
                                            visuals,
                                            section,
                                            killer,
                                            player,
                                            lockedTargets,
                                            levitatingTargets);
                                }
                            }
                        }
                        trailEnded.set(i, true);
                    }
                }
                AuraParticles.spawnThickWhiteSparkle(visuals, currentPositions.get(i));
            }
            return true;
        });
    }
}
