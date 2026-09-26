package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.session.EffectSessionHandle;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Starts, cancels, and inspects live kill-effect sessions.
 *
 * <h2>Gated vs forced</h2>
 *
 * <ul>
 *   <li><b>Gated</b> ({@link #play}): applies world/game-mode/trigger conditions, availability,
 *       access ({@link AccessService#canActivate}), cooldown, fires {@link
 *       com.monkey.ktplus.api.event.KillEffectPreTriggerEvent} (cancellable), then starts the
 *       session and fires {@link com.monkey.ktplus.api.event.KillEffectTriggerEvent}. Also may
 *       trigger random events. Returns {@code false} if any gate fails or the session cannot start.
 *   <li><b>Forced</b> ({@link #playForced}): skips gates, cooldowns, and trigger events — starts
 *       the session immediately if the runtime accepts it. Use for admin commands, tests, and
 *       scripted sequences.
 * </ul>
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * KtPlus api = KtPlusProvider.get();
 * // Respect player rules & cooldowns:
 * boolean ok = api.runtime().play(killer, victim, location, "lightning");
 * // Admin / cinematic bypass:
 * api.runtime().playForced(killer, victim, location, effect);
 * }</pre>
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread (or Folia region thread for the killer).
 *
 * @since 4.0.3
 */
public interface RuntimeService {
    /**
     * Plays an effect with full gating (conditions, availability, access, cooldown, pre-trigger
     * event).
     *
     * @param killer activating player
     * @param victim killed entity
     * @param location effect origin
     * @param effect effect to play
     * @return {@code true} if a session started
     */
    boolean play(Player killer, Entity victim, Location location, Effect effect);

    /**
     * Plays an effect by id/alias with full gating.
     *
     * @param killer activating player
     * @param victim killed entity
     * @param location effect origin
     * @param effectId effect id or alias
     * @return {@code true} if a session started
     */
    boolean play(Player killer, Entity victim, Location location, String effectId);

    /**
     * Plays an effect, bypassing gates, cooldown, and kill-trigger events.
     *
     * @param killer activating player
     * @param victim killed entity
     * @param location effect origin
     * @param effect effect to play
     * @return {@code true} if a session started
     */
    boolean playForced(Player killer, Entity victim, Location location, Effect effect);

    /**
     * Plays an effect by id/alias, bypassing gates, cooldown, and kill-trigger events.
     *
     * @param killer activating player
     * @param victim killed entity
     * @param location effect origin
     * @param effectId effect id or alias
     * @return {@code true} if a session started
     */
    boolean playForced(Player killer, Entity victim, Location location, String effectId);

    /**
     * Cancels all active sessions for the player.
     *
     * <p><b>Side effects:</b> runs session cleanup hooks and stops scheduled tasks.
     *
     * @param player online player
     */
    void cancel(Player player);

    /**
     * Cancels all active sessions for the player id.
     *
     * @param playerId player UUID
     */
    void cancel(UUID playerId);

    /**
     * Cancels every active effect session on the server.
     *
     * <p><b>Side effects:</b> global cleanup; use sparingly (reload / shutdown).
     */
    void cancelAll();

    /**
     * Counts active sessions owned by the player.
     *
     * @param player online player
     * @return active session count ({@code >= 0})
     */
    int activeSessions(Player player);

    /**
     * Counts active sessions owned by the player id.
     *
     * @param playerId player UUID
     * @return active session count ({@code >= 0})
     */
    int activeSessions(UUID playerId);

    /**
     * Counts currently running heavy sessions across all players.
     *
     * @return heavy session count ({@code >= 0})
     */
    int heavySessions();

    /**
     * Lists active session handles for the player.
     *
     * @param player online player
     * @return collection of handles (possibly empty)
     */
    Collection<EffectSessionHandle> sessions(Player player);

    /**
     * Lists active session handles for the player id.
     *
     * @param playerId player UUID
     * @return collection of handles (possibly empty)
     */
    Collection<EffectSessionHandle> sessions(UUID playerId);

    /**
     * Looks up a session by its unique id.
     *
     * @param sessionId session UUID from {@link EffectSessionHandle#id()}
     * @return the handle if still tracked
     */
    Optional<EffectSessionHandle> session(UUID sessionId);
}
