package com.monkey.ktplus.effects.list.fireworks.animation;

import com.monkey.ktplus.effects.support.entity.FireworkDetonator;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

public final class FireworksFinaleLauncher {
    private static final Color[] COLORS = {
        Color.RED, Color.ORANGE, Color.YELLOW,
        Color.GREEN, Color.BLUE, Color.AQUA,
        Color.FUCHSIA, Color.WHITE, Color.PURPLE
    };
    private static final double HOMING_SPEED = 1.05;
    private static final double IMPACT_DISTANCE = 1.35;
    private static final int ROCKET_POWER = 1;
    private static final int MAX_CONTINUATIONS = 2;

    private FireworksFinaleLauncher() {}

    public static void launchDetached(
            FireworksScheduler scheduler,
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            VisualEffectService visuals,
            Player killer,
            FireworksSettings settings,
            boolean allowStructure) {
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(finaleService, "finaleService");
        Objects.requireNonNull(finale, "finale");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(settings, "settings");

        FireworksMarkedTracker tracker = finale.tracker();
        Set<UUID> targets = tracker.activeMarkedIds();
        if (targets.isEmpty()) {
            tracker.stopFootTracking();
            finaleService.tryClose(finale);
            return;
        }

        if (!killer.isOnline()) {
            finaleService.abortForKiller(finale.killerId());
            return;
        }

        Material glowstone = MaterialResolver.resolve(visuals.material("GLOWSTONE"), "GLOWSTONE");
        tracker.initializeFinale(settings.fireworksPerPlayer());
        tracker.handoffFootTracking(
                scheduler, killer, glowstone, allowStructure, settings.footUpdateTicks(), finale.worldName());

        int launchCount = targets.size() * settings.fireworksPerPlayer();
        finale.scheduleLaunches(launchCount);

        Random random = new Random();
        long delay = 0L;
        for (UUID targetId : new ArrayList<>(targets)) {
            for (int fireworkIndex = 0; fireworkIndex < settings.fireworksPerPlayer(); fireworkIndex++) {
                final long launchDelay = delay;
                scheduler.runLater(launchDelay, () -> {
                    try {
                        launchSingle(
                                scheduler,
                                finaleService,
                                finale,
                                visuals,
                                killer,
                                targetId,
                                settings,
                                random);
                    } finally {
                        finaleService.onLaunchFinished(finale);
                    }
                });
                delay += settings.finaleIntervalTicks();
            }
        }
    }

    private static void launchSingle(
            FireworksScheduler scheduler,
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            VisualEffectService visuals,
            Player killer,
            UUID targetId,
            FireworksSettings settings,
            Random random) {
        if (finale.isClosed()) {
            return;
        }

        FireworksMarkedTracker tracker = finale.tracker();
        ShotState shot = new ShotState();
        if (!killer.isOnline()) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return;
        }

        Player target = tracker.onlinePlayer(targetId);
        if (target == null || target.isDead()) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return;
        }

        World world = killer.getWorld();
        if (world == null
                || !world.getName().equals(finale.worldName())
                || !world.equals(target.getWorld())) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return;
        }

        Location spawn = killer.getEyeLocation();
        Location targetLoc = aimPoint(target);
        if (!tracker.allowsAction(killer, spawn) || !tracker.allowsAction(killer, targetLoc)) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return;
        }

        Firework firework = createRocket(finaleService, world, spawn, killer, finale, random, true);
        visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", spawn, 1.5f, 1.3f);
        steerToward(firework, spawn, targetLoc);

        Location[] lastLocation = {spawn.clone()};
        finale.onHomingStarted();
        scheduleHomingFlight(
                scheduler,
                finaleService,
                finale,
                visuals,
                killer,
                targetId,
                settings,
                random,
                shot,
                firework,
                lastLocation);
    }

    private static void scheduleHomingFlight(
            FireworksScheduler scheduler,
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            VisualEffectService visuals,
            Player killer,
            UUID targetId,
            FireworksSettings settings,
            Random random,
            ShotState shot,
            Firework firework,
            Location[] lastLocation) {
        FireworksMarkedTracker tracker = finale.tracker();
        scheduler.runTimer(1L, 1L, () -> {
            if (finale.isClosed() || shot.done.get()) {
                safeRemove(finaleService, firework, finale);
                return false;
            }

            if (!firework.isValid()) {
                boolean keepGoing = handleInvalidRocket(
                        scheduler,
                        finaleService,
                        finale,
                        visuals,
                        killer,
                        targetId,
                        settings,
                        random,
                        shot,
                        lastLocation);
                if (!keepGoing) {
                    finaleService.onHomingFinished(finale);
                }
                return keepGoing;
            }

            EntityCompat.tryExtendFireworkFlight(firework);

            if (!killer.isOnline()) {
                finaleService.abortForKiller(finale.killerId());
                safeRemove(finaleService, firework, finale);
                finaleService.onHomingFinished(finale);
                return false;
            }

            Player liveTarget = tracker.onlinePlayer(targetId);
            if (liveTarget == null || liveTarget.isDead()) {
                resolveWithDetonation(finaleService, finale, tracker, targetId, shot, firework);
                finaleService.onHomingFinished(finale);
                return false;
            }

            World fireworkWorld = firework.getWorld();
            if (fireworkWorld == null
                    || !fireworkWorld.getName().equals(finale.worldName())
                    || !liveTarget.getWorld().equals(fireworkWorld)
                    || !killer.getWorld().equals(fireworkWorld)) {
                resolveMiss(finaleService, finale, tracker, targetId, shot);
                safeRemove(finaleService, firework, finale);
                finaleService.onHomingFinished(finale);
                return false;
            }

            Location liveTargetLoc = aimPoint(liveTarget);
            Location fireworkLoc = firework.getLocation();
            lastLocation[0] = fireworkLoc.clone();
            Vector delta = liveTargetLoc.toVector().subtract(fireworkLoc.toVector());
            double distance = delta.length();
            if (distance <= IMPACT_DISTANCE) {
                resolveImpact(finaleService, finale, visuals, killer, targetId, settings, shot, firework);
                finaleService.onHomingFinished(finale);
                return false;
            }

            delta.normalize().multiply(Math.min(HOMING_SPEED, Math.max(0.65, distance * 0.42)));
            firework.setVelocity(delta);
            return true;
        });
    }

    private static boolean handleInvalidRocket(
            FireworksScheduler scheduler,
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            VisualEffectService visuals,
            Player killer,
            UUID targetId,
            FireworksSettings settings,
            Random random,
            ShotState shot,
            Location[] lastLocation) {
        FireworksMarkedTracker tracker = finale.tracker();
        if (shot.done.get() || finale.isClosed()) {
            return false;
        }

        if (!killer.isOnline()) {
            finaleService.abortForKiller(finale.killerId());
            return false;
        }

        Player liveTarget = tracker.onlinePlayer(targetId);
        if (liveTarget == null || liveTarget.isDead()) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return false;
        }

        World targetWorld = liveTarget.getWorld();
        if (targetWorld == null || !targetWorld.getName().equals(finale.worldName())) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return false;
        }

        if (distanceToAim(lastLocation[0], liveTarget) <= IMPACT_DISTANCE * 2.0) {
            if (shot.done.compareAndSet(false, true)) {
                finishShot(killer, targetId, settings, tracker);
                finaleService.tryClose(finale);
            }
            return false;
        }

        if (shot.continuations.incrementAndGet() > MAX_CONTINUATIONS) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return false;
        }

        spawnContinuationRocket(
                scheduler,
                finaleService,
                finale,
                visuals,
                killer,
                targetId,
                settings,
                random,
                shot,
                lastLocation[0],
                liveTarget);
        return false;
    }

    private static void spawnContinuationRocket(
            FireworksScheduler scheduler,
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            VisualEffectService visuals,
            Player killer,
            UUID targetId,
            FireworksSettings settings,
            Random random,
            ShotState shot,
            Location spawn,
            Player target) {
        if (shot.done.get() || finale.isClosed()) {
            return;
        }

        FireworksMarkedTracker tracker = finale.tracker();
        World world = spawn.getWorld();
        if (world == null
                || !world.getName().equals(finale.worldName())
                || !world.equals(target.getWorld())) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return;
        }

        Location targetLoc = aimPoint(target);
        if (!tracker.allowsAction(killer, spawn) || !tracker.allowsAction(killer, targetLoc)) {
            resolveMiss(finaleService, finale, tracker, targetId, shot);
            return;
        }

        Firework firework = createRocket(finaleService, world, spawn, killer, finale, random, false);
        visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", spawn, 1.2f, 1.15f);
        steerToward(firework, spawn, targetLoc);

        Location[] lastLocation = {spawn.clone()};
        finale.onHomingStarted();
        scheduleHomingFlight(
                scheduler,
                finaleService,
                finale,
                visuals,
                killer,
                targetId,
                settings,
                random,
                shot,
                firework,
                lastLocation);
    }

    private static Firework createRocket(
            FireworksFinaleService finaleService,
            World world,
            Location spawn,
            Player killer,
            FireworksActiveFinale finale,
            Random random,
            boolean primary) {
        Firework firework = EntityCompat.spawnSilentFirework(world, spawn);
        finale.trackFirework(firework.getUniqueId());
        finaleService.entityRegistry().registerFirework(firework.getUniqueId(), killer.getUniqueId());

        FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.addEffect(FireworkEffect.builder()
                .flicker(primary)
                .trail(true)
                .withColor(randomColor(random), randomColor(random), Color.WHITE)
                .withFade(Color.FUCHSIA, Color.AQUA)
                .with(FireworkEffect.Type.BALL)
                .build());
        meta.setPower(ROCKET_POWER);
        firework.setFireworkMeta(meta);
        EntityCompat.trySetSilent(firework, true);
        EntityCompat.tryExtendFireworkFlight(firework);
        return firework;
    }

    private static void resolveImpact(
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            VisualEffectService visuals,
            Player killer,
            UUID targetId,
            FireworksSettings settings,
            ShotState shot,
            Firework firework) {
        FireworksMarkedTracker tracker = finale.tracker();
        if (!shot.done.compareAndSet(false, true)) {
            safeRemove(finaleService, firework, finale);
            return;
        }
        Location blastAt = firework.getLocation().clone();
        playImpactFx(visuals, blastAt);
        finale.untrackFirework(firework.getUniqueId());
        finaleService.entityRegistry().forget(firework.getUniqueId());
        if (firework.isValid()) {
            firework.remove();
        }
        finishShot(killer, targetId, settings, tracker);
        finaleService.tryClose(finale);
    }

    private static void playImpactFx(VisualEffectService visuals, Location blastAt) {
        visuals.sound("ENTITY_FIREWORK_ROCKET_BLAST", blastAt, 1.6f, 1.0f);
        visuals.sound("ENTITY_FIREWORK_ROCKET_TWINKLE", blastAt, 1.0f, 1.1f);
        visuals.particle("FIREWORKS_SPARK", blastAt, 40, 0.35, 0.35, 0.35, 0.12, null);
        visuals.particle("FLASH", blastAt, 1, 0.0, 0.0, 0.0, 0.0, null);
        visuals.particle("END_ROD", blastAt, 18, 0.45, 0.45, 0.45, 0.08, null);
    }

    private static void resolveWithDetonation(
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            FireworksMarkedTracker tracker,
            UUID targetId,
            ShotState shot,
            Firework firework) {
        if (!shot.done.compareAndSet(false, true)) {
            safeRemove(finaleService, firework, finale);
            return;
        }
        if (firework.isValid()) {
            FireworkDetonator.detonate(firework);
            if (firework.isValid()) {
                firework.remove();
            }
        }
        finale.untrackFirework(firework.getUniqueId());
        finaleService.entityRegistry().forget(firework.getUniqueId());
        tracker.completeShot(targetId);
        finaleService.tryClose(finale);
    }

    private static void resolveMiss(
            FireworksFinaleService finaleService,
            FireworksActiveFinale finale,
            FireworksMarkedTracker tracker,
            UUID targetId,
            ShotState shot) {
        if (shot.done.compareAndSet(false, true)) {
            tracker.completeShot(targetId);
            finaleService.tryClose(finale);
        }
    }

    private static void safeRemove(
            FireworksFinaleService finaleService, Firework firework, FireworksActiveFinale finale) {
        if (firework == null) {
            return;
        }
        finale.untrackFirework(firework.getUniqueId());
        finaleService.entityRegistry().forget(firework.getUniqueId());
        if (firework.isValid()) {
            firework.remove();
        }
    }

    private static void finishShot(
            Player killer,
            UUID targetId,
            FireworksSettings settings,
            FireworksMarkedTracker tracker) {
        applyDamage(killer, targetId, settings, tracker);
        tracker.completeShot(targetId);
    }

    private static void applyDamage(
            Player killer, UUID targetId, FireworksSettings settings, FireworksMarkedTracker tracker) {
        if (!settings.damageEnabled() || settings.damagePerFirework() <= 0.0) {
            return;
        }
        Player target = tracker.onlinePlayer(targetId);
        if (target == null || target.isDead()) {
            return;
        }
        if (!tracker.allowsAction(killer, target.getLocation())) {
            return;
        }
        target.damage(settings.damagePerFirework(), killer);
    }

    private static void steerToward(Firework firework, Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 0.01) {
            direction = new Vector(0, 0.4, 0);
        } else {
            direction.normalize().multiply(HOMING_SPEED);
        }
        firework.setVelocity(direction);
    }

    private static Location aimPoint(Player target) {
        return target.getLocation().clone().add(0, 1.0, 0);
    }

    private static double distanceToAim(Location from, Player target) {
        return from.distance(aimPoint(target));
    }

    private static Color randomColor(Random random) {
        return COLORS[random.nextInt(COLORS.length)];
    }

    private static final class ShotState {
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final AtomicInteger continuations = new AtomicInteger(0);
    }
}
