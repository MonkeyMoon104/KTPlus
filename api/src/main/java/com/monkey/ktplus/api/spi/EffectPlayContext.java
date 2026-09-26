package com.monkey.ktplus.api.spi;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.session.EffectSessionHandle;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Read-only context passed to {@link EffectExecutor#execute(EffectPlayContext)} for a playing
 * effect session.
 *
 * <p>All accessors are safe to call during {@code execute} and from session-scheduled tasks while
 * the session remains {@linkplain EffectSessionHandle#active() active}. Prefer {@link #session()}
 * for delayed work so cleanup stays tied to the effect lifetime.
 *
 * @since 4.0.3
 */
public interface EffectPlayContext {
    /**
     * Player who triggered the effect (the killer / activator).
     *
     * @return non-null killer
     */
    Player killer();

    /**
     * Entity that was killed, if any.
     *
     * <p>May be {@code null} for forced / synthetic plays that omit a victim.
     *
     * @return victim entity, or {@code null}
     */
    @Nullable Entity victim();

    /**
     * World location where the effect should originate (typically the victim's death location).
     *
     * @return non-null location snapshot for this play
     */
    Location location();

    /**
     * Effect descriptor being played.
     *
     * @return non-null effect
     */
    Effect effect();

    /**
     * Session handle for scheduling, cleanup hooks, and cancellation.
     *
     * @return non-null active session at the time {@code execute} is entered
     */
    EffectSessionHandle session();

    /**
     * Whether the server is running in cosmetic mode (reduced world interaction).
     *
     * <p>When {@code true}, executors should avoid damaging blocks, dropping items, or applying
     * gameplay-altering effects.
     *
     * @return {@code true} if cosmetic constraints apply
     */
    boolean cosmeticMode();
}
