package com.monkey.ktplus.api.session;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Handle for a single in-flight kill-effect session.
 *
 * <p>Obtained from {@link com.monkey.ktplus.api.spi.EffectPlayContext#session()} inside an {@link
 * com.monkey.ktplus.api.spi.EffectExecutor}, or from {@link
 * com.monkey.ktplus.api.service.RuntimeService} inspection APIs. Use scheduling helpers so delayed
 * work is cancelled automatically when the session ends.
 *
 * <p><b>Thread-safety:</b> mutate / schedule from the Bukkit main thread unless otherwise noted by
 * the implementation.
 *
 * @since 4.0.3
 */
public interface EffectSessionHandle {
    /**
     * Unique id of this session instance.
     *
     * @return session UUID
     */
    UUID id();

    /**
     * Owner player UUID (the killer / activator).
     *
     * @return player UUID
     */
    UUID playerId();

    /**
     * Id of the effect being played.
     *
     * @return effect id
     */
    String effectId();

    /**
     * Whether this session counts as a heavy effect.
     *
     * @return {@code true} if heavy
     */
    boolean heavy();

    /**
     * Whether cosmetic mode was active when the session started.
     *
     * @return {@code true} if cosmetic constraints apply
     */
    boolean cosmeticMode();

    /**
     * Whether the session is still running (not cancelled / expired).
     *
     * @return {@code true} while active
     */
    boolean active();

    /**
     * Origin location captured at session start.
     *
     * @return non-null location
     */
    Location origin();

    /**
     * Registers a hook invoked when the session cleans up (cancel, complete, or unload).
     *
     * <p><b>Side effects:</b> {@code hook} may run immediately if the session is already inactive.
     *
     * @param hook cleanup callback
     */
    void onCleanup(Runnable hook);

    /**
     * Schedules {@code action} on the session's scheduler after {@code delayTicks}.
     *
     * <p>Cancelled automatically if the session ends first.
     *
     * @param delayTicks delay in server ticks ({@code >= 0})
     * @param action task to run
     */
    void runLater(long delayTicks, Runnable action);

    /**
     * Schedules a repeating task; stops when {@code action} returns {@code false} or the session
     * ends.
     *
     * @param delayTicks initial delay in ticks
     * @param periodTicks period between runs in ticks
     * @param action supplier returning {@code true} to continue, {@code false} to stop
     */
    void runTimer(long delayTicks, long periodTicks, BooleanSupplier action);

    /**
     * Spawns a tracked entity that is removed after {@code removeAfterTicks} or when the session
     * ends. Respects cosmetic mode and world-mutation guards.
     *
     * @param typeName Bukkit {@link org.bukkit.entity.EntityType} name
     * @param location spawn location
     * @param setup optional post-spawn setup
     * @param removeAfterTicks ticks until auto-remove ({@code >= 1})
     * @return spawned entity, or {@code null} if spawn was denied / failed
     * @since 4.0.5
     */
    @Nullable
    Entity spawnEntity(
            String typeName, Location location, @Nullable Consumer<Entity> setup, long removeAfterTicks);

    /**
     * Tracks an existing entity so it is removed when the session cleans up.
     *
     * @param entity entity to track
     * @since 4.0.5
     */
    void trackEntity(Entity entity);

    /**
     * Whether temporary block / world mutations are allowed at {@code location} for this session.
     *
     * @param actor optional actor (defaults to session owner when {@code null})
     * @param location target location
     * @return {@code true} if mutations are allowed
     * @since 4.0.5
     */
    boolean allowsWorldMutation(@Nullable Player actor, Location location);

    /**
     * Places a temporary block that restores after {@code restoreDelayTicks}, when allowed.
     *
     * @param actor optional actor for protection checks
     * @param block target block
     * @param material temporary material
     * @param restoreDelayTicks ticks until restore
     * @since 4.0.5
     */
    void placeTempBlock(@Nullable Player actor, Block block, Material material, long restoreDelayTicks);

    /**
     * Cancels this session and runs cleanup hooks.
     *
     * <p><b>Side effects:</b> stops scheduled tasks; subsequent {@link #active()} is {@code false}.
     */
    void cancel();
}
