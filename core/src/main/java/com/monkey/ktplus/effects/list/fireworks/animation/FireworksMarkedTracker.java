package com.monkey.ktplus.effects.list.fireworks.animation;

import com.monkey.ktplus.effects.list.fireworks.animation.util.FireworksGround;
import com.monkey.ktplus.effects.runtime.block.BlockChangeGuard;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockChange;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class FireworksMarkedTracker {
    private final TemporaryBlockService temporaryBlocks;
    private final BlockChangeGuard blockChangeGuard;
    private final Set<UUID> marked = new HashSet<>();
    private final Set<UUID> released = new HashSet<>();
    private final Map<UUID, TemporaryBlockChange> pinnedBlocks = new HashMap<>();
    private final Map<UUID, AtomicInteger> pendingShots = new HashMap<>();
    private final AtomicBoolean footTimerStarted = new AtomicBoolean();
    private final AtomicBoolean footTrackingActive = new AtomicBoolean(true);

    public FireworksMarkedTracker(TemporaryBlockService temporaryBlocks, BlockChangeGuard blockChangeGuard) {
        this.temporaryBlocks = Objects.requireNonNull(temporaryBlocks, "temporaryBlocks");
        this.blockChangeGuard = Objects.requireNonNull(blockChangeGuard, "blockChangeGuard");
    }

    public void mark(
            FireworksScheduler scheduler,
            Player killer,
            Player player,
            Material glowstone,
            boolean allowStructure) {
        Objects.requireNonNull(player, "player");
        registerMarked(player.getUniqueId());
        if (allowStructure) {
            updateFoot(killer, player, glowstone);
        }
    }

    void registerMarked(UUID playerId) {
        marked.add(playerId);
    }

    public int markedCount() {
        return activeMarkedIds().size();
    }

    public int huntingCount() {
        if (pendingShots.isEmpty()) {
            return markedCount();
        }
        int hunting = 0;
        for (UUID playerId : pendingShots.keySet()) {
            AtomicInteger remaining = pendingShots.get(playerId);
            if (remaining != null && remaining.get() > 0 && !released.contains(playerId)) {
                hunting++;
            }
        }
        return hunting;
    }

    public boolean isMarked(UUID playerId) {
        return marked.contains(playerId) && !released.contains(playerId);
    }

    public Set<UUID> activeMarkedIds() {
        Set<UUID> active = new HashSet<>();
        for (UUID id : marked) {
            if (!released.contains(id)) {
                active.add(id);
            }
        }
        return Collections.unmodifiableSet(active);
    }

    public boolean isPinned(Location location) {
        Objects.requireNonNull(location, "location");
        for (TemporaryBlockChange pinned : pinnedBlocks.values()) {
            Location pinnedLoc = pinned.block().getLocation();
            if (pinnedLoc.getWorld() == location.getWorld()
                    && pinnedLoc.getBlockX() == location.getBlockX()
                    && pinnedLoc.getBlockY() == location.getBlockY()
                    && pinnedLoc.getBlockZ() == location.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public void initializeFinale(int shotsPerPlayer) {
        pendingShots.clear();
        for (UUID playerId : activeMarkedIds()) {
            pendingShots.put(playerId, new AtomicInteger(Math.max(1, shotsPerPlayer)));
        }
    }

    public Collection<TemporaryBlockChange> pinnedBlockChanges() {
        return Collections.unmodifiableCollection(new ArrayList<>(pinnedBlocks.values()));
    }

    public void handoffFootTracking(
            FireworksScheduler detachedScheduler,
            Player killer,
            Material glowstone,
            boolean allowStructure,
            long periodTicks,
            String finaleWorldName) {
        Objects.requireNonNull(detachedScheduler, "detachedScheduler");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(glowstone, "glowstone");
        Objects.requireNonNull(finaleWorldName, "finaleWorldName");
        stopFootTracking();
        footTimerStarted.set(false);
        footTrackingActive.set(true);
        if (allowStructure) {
            for (UUID playerId : activeMarkedIds()) {
                Player player = onlinePlayer(playerId);
                if (player != null && player.getWorld().getName().equals(finaleWorldName)) {
                    updateFoot(killer, player, glowstone);
                }
            }
        }
        startFootTracking(detachedScheduler, killer, glowstone, allowStructure, periodTicks, finaleWorldName);
    }

    public void completeShot(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        AtomicInteger remaining = pendingShots.get(playerId);
        if (remaining == null) {
            return;
        }
        if (remaining.decrementAndGet() <= 0) {
            pendingShots.remove(playerId);
            release(playerId);
        }
    }

    public void skipRemainingShots(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        pendingShots.remove(playerId);
        release(playerId);
    }

    public void startFootTracking(
            FireworksScheduler scheduler,
            Player killer,
            Material glowstone,
            boolean allowStructure,
            long periodTicks) {
        startFootTracking(scheduler, killer, glowstone, allowStructure, periodTicks, null);
    }

    public void startFootTracking(
            FireworksScheduler scheduler,
            Player killer,
            Material glowstone,
            boolean allowStructure,
            long periodTicks,
            @Nullable String finaleWorldName) {
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(glowstone, "glowstone");
        if (!allowStructure || !footTimerStarted.compareAndSet(false, true)) {
            return;
        }
        scheduler.runTimer(0L, periodTicks, () -> {
            if (!killer.isOnline()) {
                stopFootTracking();
                return false;
            }
            for (UUID playerId : new ArrayList<>(marked)) {
                if (released.contains(playerId)) {
                    continue;
                }
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    skipRemainingShots(playerId);
                    continue;
                }
                if (finaleWorldName != null && !player.getWorld().getName().equals(finaleWorldName)) {
                    skipRemainingShots(playerId);
                    continue;
                }
                updateFoot(killer, player, glowstone);
            }
            return footTrackingActive.get();
        });
    }

    public void stopFootTracking() {
        footTrackingActive.set(false);
    }

    public void release(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (released.contains(playerId)) {
            return;
        }
        released.add(playerId);
        pendingShots.remove(playerId);
        restorePinned(playerId);
        marked.remove(playerId);
        if (!hasActiveMarkedPlayers()) {
            stopFootTracking();
        }
    }

    public void releaseAll() {
        pendingShots.clear();
        for (UUID playerId : new ArrayList<>(marked)) {
            release(playerId);
        }
    }

    public boolean hasActiveMarkedPlayers() {
        for (UUID id : marked) {
            if (!released.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private void updateFoot(Player killer, Player player, Material glowstone) {
        Block block = FireworksGround.solidBlockBelow(player);
        if (block == null) {
            return;
        }
        Location blockLoc = block.getLocation();
        TemporaryBlockChange existing = pinnedBlocks.get(player.getUniqueId());
        if (existing != null && existing.block().getLocation().equals(blockLoc)) {
            return;
        }
        if (!blockChangeGuard.allows(killer, blockLoc) || SensitiveBlocks.isSensitive(block)) {
            return;
        }
        if (existing != null) {
            temporaryBlocks.restore(existing);
            pinnedBlocks.remove(player.getUniqueId());
        }
        TemporaryBlockChange change = temporaryBlocks.change(block, glowstone);
        pinnedBlocks.put(player.getUniqueId(), change);
    }

    private void restorePinned(UUID playerId) {
        TemporaryBlockChange pinned = pinnedBlocks.remove(playerId);
        if (pinned != null) {
            temporaryBlocks.restore(pinned);
        }
    }

    public @Nullable Player onlinePlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return null;
        }
        return player;
    }

    public boolean allowsAction(Player killer, Location location) {
        return blockChangeGuard.allows(killer, location);
    }
}
