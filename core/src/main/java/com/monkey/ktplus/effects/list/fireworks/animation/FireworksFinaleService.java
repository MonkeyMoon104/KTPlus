package com.monkey.ktplus.effects.list.fireworks.animation;

import com.monkey.ktplus.effects.runtime.EffectEntityRegistry;
import com.monkey.ktplus.effects.runtime.block.BlockChangeGuard;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.scheduler.ScheduledHandle;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class FireworksFinaleService {
    private final PlatformScheduler scheduler;
    private final EffectEntityRegistry entities;
    private final BlockChangeGuard blockChangeGuard;
    private final Map<UUID, FireworksActiveFinale> activeFinales = new ConcurrentHashMap<>();

    public FireworksFinaleService(
            PlatformScheduler scheduler,
            EffectEntityRegistry entities,
            BlockChangeGuard blockChangeGuard) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.entities = Objects.requireNonNull(entities, "entities");
        this.blockChangeGuard = Objects.requireNonNull(blockChangeGuard, "blockChangeGuard");
    }

    public EffectEntityRegistry entityRegistry() {
        return entities;
    }

    public BlockChangeGuard blockChangeGuard() {
        return blockChangeGuard;
    }

    public void launch(
            VisualEffectService visuals,
            Location anchor,
            Player killer,
            FireworksSettings settings,
            FireworksMarkedTracker tracker,
            boolean allowStructure) {
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(tracker, "tracker");
        if (anchor.getWorld() == null) {
            tracker.releaseAll();
            return;
        }
        FireworksActiveFinale finale = register(
                killer.getUniqueId(), anchor.getWorld().getName(), tracker);
        FireworksScheduler finaleScheduler = createScheduler(anchor, finale);
        finaleScheduler.runTimer(0L, 5L, () -> {
            if (finale.isClosed() || !killer.isOnline()) {
                PerkActionBar.clear(killer);
                return false;
            }
            PerkActionBar.show(
                    killer,
                    String.format(
                            "&d✦ FIREWORKS &8| &eMARKED &f%d &8| &cHUNTING &f%d",
                            tracker.markedCount(),
                            tracker.huntingCount()));
            return true;
        });
        FireworksFinaleLauncher.launchDetached(
                finaleScheduler,
                this,
                finale,
                visuals,
                killer,
                settings,
                allowStructure);
    }

    public void onPlayerQuit(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        abortForKiller(playerId);
        for (FireworksActiveFinale finale : snapshotFinale()) {
            if (finale.isClosed() || finale.killerId().equals(playerId)) {
                continue;
            }
            if (finale.tracker().isMarked(playerId)) {
                dropTarget(finale, playerId);
            }
        }
    }

    public void onPlayerChangedWorld(Player player, World fromWorld) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(fromWorld, "fromWorld");
        UUID playerId = player.getUniqueId();
        for (FireworksActiveFinale finale : snapshotFinale()) {
            if (finale.isClosed()) {
                continue;
            }
            if (finale.killerId().equals(playerId)) {
                abortFinale(finale);
                continue;
            }
            if (!finale.tracker().isMarked(playerId)) {
                continue;
            }
            if (fromWorld.getName().equals(finale.worldName())
                    && !player.getWorld().getName().equals(finale.worldName())) {
                dropTarget(finale, playerId);
            }
        }
    }

    public void abortForKiller(UUID killerId) {
        Objects.requireNonNull(killerId, "killerId");
        for (FireworksActiveFinale finale : snapshotFinale()) {
            if (finale.killerId().equals(killerId)) {
                abortFinale(finale);
            }
        }
    }

    public void abortForWorld(String worldName) {
        Objects.requireNonNull(worldName, "worldName");
        for (FireworksActiveFinale finale : snapshotFinale()) {
            if (worldName.equals(finale.worldName())) {
                abortFinale(finale);
            }
        }
    }

    public void shutdown() {
        for (FireworksActiveFinale finale : snapshotFinale()) {
            abortFinale(finale);
        }
        activeFinales.clear();
    }

    void tryClose(FireworksActiveFinale finale) {
        Objects.requireNonNull(finale, "finale");
        if (finale.isClosed() || !finale.canClose()) {
            return;
        }
        Player killer = Bukkit.getPlayer(finale.killerId());
        PerkActionBar.clear(killer);
        finale.close();
        activeFinales.remove(finale.id());
    }

    void onLaunchFinished(FireworksActiveFinale finale) {
        finale.onLaunchFinished();
        tryClose(finale);
    }

    void onHomingFinished(FireworksActiveFinale finale) {
        finale.onHomingFinished();
        tryClose(finale);
    }

    private FireworksActiveFinale register(UUID killerId, String worldName, FireworksMarkedTracker tracker) {
        FireworksActiveFinale finale = new FireworksActiveFinale(killerId, worldName, tracker, entities);
        activeFinales.put(finale.id(), finale);
        return finale;
    }

    private void dropTarget(FireworksActiveFinale finale, UUID targetId) {
        finale.tracker().skipRemainingShots(targetId);
        tryClose(finale);
    }

    private void abortFinale(FireworksActiveFinale finale) {
        if (finale.isClosed()) {
            return;
        }
        Player killer = Bukkit.getPlayer(finale.killerId());
        PerkActionBar.clear(killer);
        finale.close();
        activeFinales.remove(finale.id());
    }

    private ArrayList<FireworksActiveFinale> snapshotFinale() {
        return new ArrayList<>(activeFinales.values());
    }

    private FireworksScheduler createScheduler(Location anchor, FireworksActiveFinale finale) {
        Location region = anchor.clone();
        return new FireworksScheduler() {
            @Override
            public void runLater(long delayTicks, Runnable action) {
                if (finale.isClosed()) {
                    return;
                }
                AtomicReference<ScheduledHandle> reference = new AtomicReference<>();
                ScheduledHandle handle = scheduler.runLater(region, () -> {
                    if (finale.isClosed()) {
                        return;
                    }
                    try {
                        action.run();
                    } finally {
                        ScheduledHandle scheduled = reference.get();
                        if (scheduled != null) {
                            finale.untrackHandle(scheduled);
                        }
                    }
                }, delayTicks);
                reference.set(handle);
                finale.trackHandle(handle);
            }

            @Override
            public void runTimer(long delayTicks, long periodTicks, BooleanSupplier action) {
                if (finale.isClosed()) {
                    return;
                }
                AtomicReference<ScheduledHandle> reference = new AtomicReference<>();
                ScheduledHandle handle = scheduler.runTimer(
                        region,
                        () -> {
                            if (finale.isClosed()) {
                                ScheduledHandle scheduled = reference.get();
                                if (scheduled != null) {
                                    scheduled.cancel();
                                    finale.untrackHandle(scheduled);
                                }
                                return;
                            }
                            if (!action.getAsBoolean()) {
                                ScheduledHandle scheduled = reference.get();
                                if (scheduled != null) {
                                    scheduled.cancel();
                                    finale.untrackHandle(scheduled);
                                }
                            }
                        },
                        delayTicks,
                        periodTicks);
                reference.set(handle);
                finale.trackHandle(handle);
            }
        };
    }
}
