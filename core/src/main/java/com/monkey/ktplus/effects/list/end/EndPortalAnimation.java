package com.monkey.ktplus.effects.list.end;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class EndPortalAnimation {
    private static final int ARC_POINTS = 14;
    private static final long PORTAL_PARTICLE_PERIOD_TICKS = 4L;
    private static final long TRAIL_PERIOD_TICKS = 3L;

    private final EffectSession session;
    private final VisualEffectService visuals;
    private final Player killer;
    private final Location center;
    private final EndSettings settings;
    private final List<Enderman> spawned = new ArrayList<>();

    public EndPortalAnimation(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Location center,
            EndSettings settings) {
        this.session = Objects.requireNonNull(session, "session");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.killer = Objects.requireNonNull(killer, "killer");
        this.center = Objects.requireNonNull(center, "center").clone();
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public void start() {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        visuals.sound("BLOCK_END_PORTAL_SPAWN", center, 1.0f, 0.8f);
        runPortalAnimation(0L);
        startTrailTask();
        startStatusBar();
        scheduleSpawns();
        session.runLater(EndSettings.EFFECT_DURATION_TICKS, this::finish);
        session.onCleanup(() -> {
            stopLingeringSounds();
            PerkActionBar.clear(killer);
        });
    }

    private void startStatusBar() {
        session.runTimer(0L, 5L, () -> {
            if (!session.active() || !killer.isOnline()) {
                PerkActionBar.clear(killer);
                return false;
            }
            int alive = 0;
            int hunting = 0;
            for (Enderman enderman : spawned) {
                if (!enderman.isValid() || enderman.isDead()) {
                    continue;
                }
                alive++;
                LivingEntity target = enderman.getTarget();
                if (target instanceof Player player
                        && player.isOnline()
                        && !player.getUniqueId().equals(killer.getUniqueId())
                        && !player.isDead()) {
                    hunting++;
                }
            }
            PerkActionBar.show(
                    killer,
                    String.format("&5👁 END &8| &dENDERMEN &f%d &8| &cHUNTING &f%d", alive, hunting));
            return true;
        });
    }

    private void runPortalAnimation(long elapsed) {
        if (!session.active() || elapsed >= settings.portalDurationTicks()) {
            return;
        }
        spawnPortalVisuals(elapsed);
        if (elapsed == 0L) {
            visuals.sound("ENTITY_ENDERMAN_AMBIENT", center, 0.5f, 0.7f);
        } else if (elapsed % 24L == 0L) {
            visuals.sound("BLOCK_PORTAL_AMBIENT", center, 0.45f, 0.95f + (elapsed * 0.0015f));
        }
        session.runLater(PORTAL_PARTICLE_PERIOD_TICKS, () -> runPortalAnimation(elapsed + PORTAL_PARTICLE_PERIOD_TICKS));
    }

    private void spawnPortalVisuals(long elapsed) {
        double baseRadius = settings.portalRadius();
        double breathe = 1.0 + (Math.sin(elapsed * 0.14) * 0.1);
        double outerRadius = baseRadius * breathe;
        double innerRadius = baseRadius * 0.62 * breathe;
        double spinOuter = elapsed * 0.13;
        double spinInner = -elapsed * 0.19;
        double yBase = center.getY() + 0.15;
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        drawArc(world, spinOuter, outerRadius, yBase + 0.05, "PORTAL", 2);
        drawArc(world, spinInner + Math.PI, innerRadius, yBase + 0.55, "REVERSE_PORTAL", 1);

        double helixProgress = (elapsed % 28L) / 28.0;
        double helixAngle = spinOuter * 2.2;
        double helixRadius = outerRadius * 0.78;
        double helixY = yBase + (helixProgress * 1.35);
        Location helix = new Location(
                world,
                center.getX() + (Math.cos(helixAngle) * helixRadius),
                helixY,
                center.getZ() + (Math.sin(helixAngle) * helixRadius));
        visuals.particle("END_ROD", helix, 2, 0.02, 0.04, 0.02, 0.0, null);

        if (elapsed % 8L == 0L) {
            Location core = center.clone().add(0, 0.65, 0);
            visuals.particle("PORTAL", core, 4, 0.08, 0.18, 0.08, 0.04, null);
            visuals.particle("DRAGON_BREATH", core, 3, 0.1, 0.12, 0.1, 0.0, null);
        }
    }

    private void drawArc(
            World world, double startAngle, double radius, double y, String particle, int countPerPoint) {
        double arcSpan = Math.PI * 1.35;
        for (int point = 0; point < ARC_POINTS; point++) {
            double t = point / (double) (ARC_POINTS - 1);
            double angle = startAngle + (arcSpan * t);
            Location arcPoint = new Location(
                    world,
                    center.getX() + (Math.cos(angle) * radius),
                    y,
                    center.getZ() + (Math.sin(angle) * radius));
            visuals.particle(particle, arcPoint, countPerPoint, 0.02, 0.06, 0.02, 0.02, null);
        }
    }

    private void startTrailTask() {
        session.runTimer(TRAIL_PERIOD_TICKS, TRAIL_PERIOD_TICKS, () -> {
            if (!session.active()) {
                return false;
            }
            for (Enderman enderman : spawned) {
                if (!enderman.isValid() || enderman.isDead()) {
                    continue;
                }
                Location trail = enderman.getLocation().clone().add(0, 1.0, 0);
                visuals.particle("PORTAL", trail, 2, 0.05, 0.08, 0.05, 0.01, null);
                visuals.particle("DRAGON_BREATH", trail, 1, 0.04, 0.06, 0.04, 0.0, null);
            }
            return true;
        });
    }

    private void scheduleSpawns() {
        long interval = settings.spawnIntervalTicks();
        for (int index = 0; index < settings.endermanCount(); index++) {
            long delay = index * interval;
            if (delay >= settings.portalDurationTicks()) {
                delay = settings.portalDurationTicks() - 1L;
            }
            session.runLater(delay, this::spawnEnderman);
        }
    }

    private void spawnEnderman() {
        if (center.getWorld() == null || !session.active()) {
            return;
        }
        playSpawnSequence(randomRingLocation());
    }

    private void playSpawnSequence(Location spawnPoint) {
        Location burst = spawnPoint.clone().add(0, 1.0, 0);
        visuals.particle("REVERSE_PORTAL", burst, 8, 0.06, 0.2, 0.06, 0.04, null);
        visuals.sound("ENTITY_ENDERMAN_AMBIENT", spawnPoint, 0.4f, 1.4f);

        session.runLater(4L, () -> {
            if (!session.active()) {
                return;
            }
            visuals.particle("WITCH", burst, 10, 0.1, 0.25, 0.1, 0.01, null);
            visuals.particle("END_ROD", burst, 8, 0.08, 0.2, 0.08, 0.01, null);
            visuals.sound("ENTITY_ENDERMAN_TELEPORT", spawnPoint, 0.85f, 0.95f);

            session.runLater(3L, () -> {
                if (!session.active()) {
                    return;
                }
                visuals.particle("PORTAL", burst, 12, 0.12, 0.28, 0.12, 0.03, null);
                visuals.particle("FLASH", burst, 1, 0.0, 0.0, 0.0, 0.0, null);
                visuals.sound("ENTITY_ENDERMAN_TELEPORT", spawnPoint, 0.9f, 1.2f);
                materializeEnderman(spawnPoint);
            });
        });
    }

    private void materializeEnderman(Location spawnPoint) {
        World world = center.getWorld();
        if (world == null || !session.active()) {
            return;
        }
        if (!session.allowsWorldMutation(killer, spawnPoint)) {
            return;
        }
        Enderman enderman = (Enderman) world.spawnEntity(spawnPoint, EntityType.ENDERMAN);
        enderman.setPersistent(false);
        enderman.setRemoveWhenFarAway(false);
        enderman.customName(Component.text(killer.getName()));
        enderman.setCustomNameVisible(true);
        Player target = nearestTarget();
        if (target != null) {
            enderman.setTarget(target);
        }
        session.entityRegistry().registerEnderman(enderman.getUniqueId(), killer.getUniqueId());
        session.trackEntity(enderman);
        spawned.add(enderman);
    }

    private Location randomRingLocation() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radius = settings.portalRadius() * random.nextDouble(0.25, 0.9);
        double angle = random.nextDouble(0.0, Math.PI * 2.0);
        return new Location(
                center.getWorld(),
                center.getX() + (Math.cos(angle) * radius),
                center.getY(),
                center.getZ() + (Math.sin(angle) * radius),
                random.nextFloat(360.0f),
                0.0f);
    }

    private @Nullable Player nearestTarget() {
        double maxRangeSq = settings.targetRange() * settings.targetRange();
        Location origin = killer.getLocation();
        return killer.getWorld().getPlayers().stream()
                .filter(player -> !player.getUniqueId().equals(killer.getUniqueId()))
                .filter(player -> player.getLocation().distanceSquared(origin) <= maxRangeSq)
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(origin)))
                .orElse(null);
    }

    private void finish() {
        for (Enderman enderman : new ArrayList<>(spawned)) {
            if (enderman.isValid() && !enderman.isDead()) {
                Location fade = enderman.getLocation();
                visuals.particle("PORTAL", fade, 10, 0.15, 0.4, 0.15, 0.02, null);
                visuals.sound("ENTITY_ENDERMAN_TELEPORT", fade, 0.6f, 1.05f);
                session.entityRegistry().forget(enderman.getUniqueId());
                enderman.remove();
            }
        }
        spawned.clear();
        stopLingeringSounds();
        PerkActionBar.clear(killer);
        session.complete();
    }

    private void stopLingeringSounds() {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            EntityCompat.stopAllSounds(player);
            EntityCompat.stopSound(player, "BLOCK_PORTAL_AMBIENT");
            EntityCompat.stopSound(player, "ENTITY_ENDERMAN_AMBIENT");
            EntityCompat.stopSound(player, "BLOCK_END_PORTAL_SPAWN");
            EntityCompat.stopSound(player, "ENTITY_ENDERMAN_TELEPORT");
            EntityCompat.stopSound(player, "BLOCK_PORTAL_TRAVEL");
            EntityCompat.stopSound(player, "BLOCK_PORTAL_TRIGGER");
        }
    }
}
