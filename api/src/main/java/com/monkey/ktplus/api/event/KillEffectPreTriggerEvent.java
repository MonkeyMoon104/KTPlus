package com.monkey.ktplus.api.event;

import com.monkey.ktplus.api.model.Effect;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Fired <em>before</em> a gated kill-effect session starts.
 *
 * <p><b>When:</b> during {@link com.monkey.ktplus.api.service.RuntimeService#play} after access /
 * cooldown gates pass, before the session is created. Not fired for {@link
 * com.monkey.ktplus.api.service.RuntimeService#playForced}.
 *
 * <p><b>Sync:</b> synchronous on the main thread. <b>Cancellable:</b> yes — cancel to abort playback
 * without starting a session or applying cooldown.
 *
 * @since 4.0.3
 * @see KillEffectTriggerEvent
 */
public final class KillEffectPreTriggerEvent extends KtPlusEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final Entity victim;
    private final Location location;
    private final Effect effect;
    private boolean cancelled;

    /**
     * Creates a pre-trigger event.
     *
     * @param killer activating player
     * @param victim killed entity
     * @param location effect origin (cloned defensively)
     * @param effect effect about to play
     */
    public KillEffectPreTriggerEvent(Player killer, Entity victim, Location location, Effect effect) {
        this.killer = Objects.requireNonNull(killer, "killer");
        this.victim = Objects.requireNonNull(victim, "victim");
        this.location = Objects.requireNonNull(location, "location").clone();
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    /**
     * @return the killer / activator
     */
    public Player getKiller() {
        return killer;
    }

    /**
     * @return the victim entity
     */
    public Entity getVictim() {
        return victim;
    }

    /**
     * @return a clone of the effect origin location
     */
    public Location getLocation() {
        return location.clone();
    }

    /**
     * @return effect that would play
     */
    public Effect getEffect() {
        return effect;
    }

    /**
     * @return {@code true} if the victim is a {@link Player}
     */
    public boolean isPlayerKill() {
        return victim instanceof Player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels gated playback when {@code cancel} is {@code true}.
     *
     * @param cancel whether to cancel
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * @return Bukkit handler list for this event type
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
