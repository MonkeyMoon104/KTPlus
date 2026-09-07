package com.monkey.ktplus.effects.runtime;

import com.monkey.ktplus.effects.list.fireworks.animation.FireworksMarkedTracker;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksSettings;
import com.monkey.ktplus.effects.runtime.block.BlockChangeGuard;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockChange;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.scheduler.ScheduledHandle;
import com.monkey.ktplus.task.CancellationReason;
import com.monkey.ktplus.task.TaskRegistry;
import com.monkey.ktplus.task.TaskScope;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class EffectSession {
    private final UUID id;
    private final UUID playerId;
    private final String effectId;
    private final boolean heavy;
    private final Location origin;
    private final PlatformScheduler scheduler;
    private final TaskRegistry taskRegistry;
    private final TemporaryBlockService temporaryBlocks;
    private final BlockChangeGuard blockChangeGuard;
    private final EffectRuntime runtime;
    private final TaskScope scope;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final List<Entity> entities = new ArrayList<>();
    private final List<TemporaryBlockChange> blockChanges = new ArrayList<>();
    private final List<Runnable> cleanupHooks = new ArrayList<>();
    private volatile @Nullable ScheduledHandle deadlineHandle;

    public EffectSession(
            UUID id,
            UUID playerId,
            String effectId,
            boolean heavy,
            Location origin,
            PlatformScheduler scheduler,
            TaskRegistry taskRegistry,
            TemporaryBlockService temporaryBlocks,
            BlockChangeGuard blockChangeGuard,
            EffectRuntime runtime) {
        this.id = Objects.requireNonNull(id, "id");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.effectId = Objects.requireNonNull(effectId, "effectId");
        this.heavy = heavy;
        this.origin = Objects.requireNonNull(origin, "origin").clone();
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.taskRegistry = Objects.requireNonNull(taskRegistry, "taskRegistry");
        this.temporaryBlocks = Objects.requireNonNull(temporaryBlocks, "temporaryBlocks");
        this.blockChangeGuard = Objects.requireNonNull(blockChangeGuard, "blockChangeGuard");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.scope = new TaskScope(id, playerId, origin.getWorld().getName());
    }

    public UUID id() {
        return id;
    }

    public UUID playerId() {
        return playerId;
    }

    public String effectId() {
        return effectId;
    }

    public boolean heavy() {
        return heavy;
    }

    public Location origin() {
        return origin.clone();
    }

    public boolean active() {
        return active.get();
    }

    public void runLater(long delayTicks, Runnable action) {
        if (!active()) {
            return;
        }
        AtomicReference<ScheduledHandle> reference = new AtomicReference<>();
        ScheduledHandle handle = scheduler.runLater(origin, () -> {
            try {
                if (active()) {
                    action.run();
                }
            } catch (RuntimeException ex) {
                cancel(CancellationReason.ERROR);
                throw ex;
            } finally {
                ScheduledHandle scheduled = reference.get();
                if (scheduled != null) {
                    taskRegistry.forget(id, scheduled);
                }
            }
        }, delayTicks);
        reference.set(track(handle));
    }

    public void resetDeadline(long delayTicks) {
        if (!active()) {
            return;
        }
        ScheduledHandle previous = deadlineHandle;
        if (previous != null) {
            previous.cancel();
            taskRegistry.forget(id, previous);
        }
        AtomicReference<ScheduledHandle> reference = new AtomicReference<>();
        ScheduledHandle handle = scheduler.runLater(origin, () -> {
            try {
                if (active()) {
                    complete();
                }
            } catch (RuntimeException ex) {
                cancel(CancellationReason.ERROR);
                throw ex;
            } finally {
                ScheduledHandle scheduled = reference.get();
                if (scheduled != null) {
                    taskRegistry.forget(id, scheduled);
                }
                if (deadlineHandle == scheduled) {
                    deadlineHandle = null;
                }
            }
        }, Math.max(1L, delayTicks));
        reference.set(track(handle));
        deadlineHandle = handle;
    }

    public void runTimer(long delayTicks, long periodTicks, BooleanSupplier action) {
        if (!active()) {
            return;
        }
        AtomicReference<ScheduledHandle> reference = new AtomicReference<>();
        ScheduledHandle handle = scheduler.runTimer(origin, () -> {
            ScheduledHandle scheduled = reference.get();
            try {
                if (!active() || !action.getAsBoolean()) {
                    if (scheduled != null) {
                        scheduled.cancel();
                        taskRegistry.forget(id, scheduled);
                    }
                }
            } catch (RuntimeException ex) {
                cancel(CancellationReason.ERROR);
                throw ex;
            }
        }, delayTicks, periodTicks);
        reference.set(track(handle));
    }

    public @Nullable Entity spawnEntity(
            String typeName, Location location, @Nullable Consumer<Entity> setup, long removeAfterTicks) {
        EntityType type = resolveEntityType(typeName);
        if (type == null || location.getWorld() == null) {
            return null;
        }
        if (!blockChangeGuard.allows(resolveActor(), location)) {
            return null;
        }
        Entity entity = location.getWorld().spawnEntity(location, type);
        if (entity instanceof LivingEntity living) {
            EntityCompat.trySetAi(living, false);
        }
        EntityCompat.trySetSilent(entity, true);
        EntityCompat.trySetInvulnerable(entity, true);
        if (setup != null) {
            setup.accept(entity);
        }
        trackEntity(entity);
        runLater(removeAfterTicks, entity::remove);
        return entity;
    }

    public boolean allowsWorldMutation(@Nullable Player actor, Location location) {
        return blockChangeGuard.allows(actor != null ? actor : resolveActor(), location);
    }

    public TemporaryBlockService temporaryBlocks() {
        return temporaryBlocks;
    }

    public void handoffFireworksFinale(
            VisualEffectService visuals,
            Player killer,
            FireworksSettings settings,
            FireworksMarkedTracker tracker,
            boolean allowStructure) {
        runtime.handoffFireworksFinale(this, visuals, killer, settings, tracker, allowStructure);
    }

    public EffectEntityRegistry entityRegistry() {
        return runtime.entityRegistry();
    }

    public void trackEntity(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (!active()) {
            return;
        }
        entities.add(entity);
    }

    public void releaseEntity(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entities.remove(entity)) {
            runtime.entityRegistry().forget(entity.getUniqueId());
        }
    }

    public void onCleanup(Runnable hook) {
        Objects.requireNonNull(hook, "hook");
        if (active()) {
            cleanupHooks.add(hook);
        }
    }

    public void temporaryBlock(Block block, Material material, long restoreDelayTicks) {
        temporaryBlock(null, block, material, restoreDelayTicks);
    }

    public void temporaryBlock(
            @Nullable Player actor, Block block, Material material, long restoreDelayTicks) {
        temporaryBlock(actor, block, material, restoreDelayTicks, false);
    }

    public void temporaryBlockWithPhysics(
            @Nullable Player actor, Block block, Material material, long restoreDelayTicks) {
        temporaryBlock(actor, block, material, restoreDelayTicks, true);
    }

    public void temporaryBlockForStructure(
            @Nullable Player actor,
            Block block,
            Material material,
            long restoreDelayTicks,
            boolean applyPhysics) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(material, "material");
        TemporaryBlockChange change = temporaryBlocks.change(block, material, applyPhysics);
        blockChanges.add(change);
        runLater(restoreDelayTicks, () -> {
            if (blockChanges.remove(change)) {
                temporaryBlocks.restore(change);
            }
        });
    }

    public void temporaryBlock(
            @Nullable Player actor,
            Block block,
            Material material,
            long restoreDelayTicks,
            boolean applyPhysics) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(material, "material");
        if (!blockChangeGuard.allows(actor != null ? actor : resolveActor(), block.getLocation())) {
            return;
        }
        TemporaryBlockChange change = temporaryBlocks.change(block, material, applyPhysics);
        blockChanges.add(change);
        runLater(restoreDelayTicks, () -> {
            if (blockChanges.remove(change)) {
                temporaryBlocks.restore(change);
            }
        });
    }

    public void restoreTemporaryBlock(Block block) {
        Objects.requireNonNull(block, "block");
        for (TemporaryBlockChange change : new ArrayList<>(blockChanges)) {
            if (change.block().getLocation().equals(block.getLocation())) {
                if (blockChanges.remove(change)) {
                    temporaryBlocks.restore(change);
                }
                return;
            }
        }
    }

    public void detachBlockChanges(Collection<TemporaryBlockChange> changes) {
        Objects.requireNonNull(changes, "changes");
        for (TemporaryBlockChange change : changes) {
            blockChanges.remove(change);
        }
    }

    private @Nullable Player resolveActor() {
        return Bukkit.getPlayer(playerId);
    }

    public void complete() {
        runtime.finish(this, CancellationReason.COMPLETED);
    }

    public void cancel(CancellationReason reason) {
        runtime.finish(this, reason);
    }

    boolean close() {
        return active.compareAndSet(true, false);
    }

    void cleanup() {
        ScheduledHandle deadline = deadlineHandle;
        if (deadline != null) {
            deadline.cancel();
            taskRegistry.forget(id, deadline);
            deadlineHandle = null;
        }
        for (Runnable hook : new ArrayList<>(cleanupHooks)) {
            try {
                hook.run();
            } catch (RuntimeException ignored) {
            }
        }
        cleanupHooks.clear();
        for (Entity entity : new ArrayList<>(entities)) {
            runtime.entityRegistry().forget(entity.getUniqueId());
            if (!entity.isDead()) {
                entity.remove();
            }
        }
        entities.clear();
        for (TemporaryBlockChange change : new ArrayList<>(blockChanges)) {
            temporaryBlocks.restore(change);
        }
        blockChanges.clear();
    }

    private ScheduledHandle track(ScheduledHandle handle) {
        return taskRegistry.track(scope, handle);
    }

    private @Nullable EntityType resolveEntityType(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return null;
        }
        try {
            return EntityType.valueOf(typeName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
