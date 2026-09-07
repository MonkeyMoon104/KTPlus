package com.monkey.ktplus.effects.list.fireworks.animation;

import com.monkey.ktplus.effects.runtime.EffectEntityRegistry;
import com.monkey.ktplus.scheduler.ScheduledHandle;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.Nullable;

final class FireworksActiveFinale {
    private final UUID id = UUID.randomUUID();
    private final UUID killerId;
    private final String worldName;
    private final FireworksMarkedTracker tracker;
    private final EffectEntityRegistry entities;
    private final Set<ScheduledHandle> handles = ConcurrentHashMap.newKeySet();
    private final Set<UUID> fireworkIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger pendingLaunches = new AtomicInteger();
    private final AtomicInteger activeHoming = new AtomicInteger();

    FireworksActiveFinale(
            UUID killerId,
            String worldName,
            FireworksMarkedTracker tracker,
            EffectEntityRegistry entities) {
        this.killerId = Objects.requireNonNull(killerId, "killerId");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.entities = Objects.requireNonNull(entities, "entities");
    }

    UUID id() {
        return id;
    }

    UUID killerId() {
        return killerId;
    }

    String worldName() {
        return worldName;
    }

    FireworksMarkedTracker tracker() {
        return tracker;
    }

    void scheduleLaunches(int count) {
        pendingLaunches.addAndGet(Math.max(0, count));
    }

    void onLaunchFinished() {
        pendingLaunches.decrementAndGet();
    }

    void onHomingStarted() {
        activeHoming.incrementAndGet();
    }

    void onHomingFinished() {
        activeHoming.decrementAndGet();
    }

    boolean isClosed() {
        return closed.get();
    }

    boolean canClose() {
        return pendingLaunches.get() <= 0
                && activeHoming.get() <= 0
                && !tracker.hasActiveMarkedPlayers();
    }

    void trackHandle(@Nullable ScheduledHandle handle) {
        if (handle != null && !closed.get()) {
            handles.add(handle);
        }
    }

    void untrackHandle(@Nullable ScheduledHandle handle) {
        if (handle != null) {
            handles.remove(handle);
        }
    }

    void trackFirework(UUID fireworkId) {
        if (!closed.get()) {
            fireworkIds.add(fireworkId);
        }
    }

    void untrackFirework(UUID fireworkId) {
        fireworkIds.remove(fireworkId);
    }

    void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        tracker.stopFootTracking();
        for (ScheduledHandle handle : handles) {
            handle.cancel();
        }
        handles.clear();
        for (UUID fireworkId : fireworkIds) {
            entities.forget(fireworkId);
            removeFireworkEntity(fireworkId);
        }
        fireworkIds.clear();
        tracker.releaseAll();
    }

    private void removeFireworkEntity(UUID fireworkId) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity.getUniqueId().equals(fireworkId)) {
                entity.remove();
                return;
            }
        }
    }
}
