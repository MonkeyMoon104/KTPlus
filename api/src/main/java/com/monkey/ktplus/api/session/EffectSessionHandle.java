package com.monkey.ktplus.api.session;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.bukkit.Location;

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
     * Cancels this session and runs cleanup hooks.
     *
     * <p><b>Side effects:</b> stops scheduled tasks; subsequent {@link #active()} is {@code false}.
     */
    void cancel();
}
