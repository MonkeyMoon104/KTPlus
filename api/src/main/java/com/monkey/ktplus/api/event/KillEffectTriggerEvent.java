package com.monkey.ktplus.api.event;

import com.monkey.ktplus.api.model.Effect;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired <em>after</em> a gated kill-effect session has successfully started.
 *
 * <p><b>When:</b> during {@link com.monkey.ktplus.api.service.RuntimeService#play} once the session
 * is running. Not fired for {@link com.monkey.ktplus.api.service.RuntimeService#playForced}.
 *
 * <p><b>Sync:</b> synchronous on the main thread. <b>Cancellable:</b> no.
 *
 * @since 4.0.3
 * @see KillEffectPreTriggerEvent
 */
public final class KillEffectTriggerEvent extends KtPlusEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final Entity victim;
    private final Location location;
    private final Effect effect;

    /**
     * Creates a post-start trigger event.
     *
     * @param killer activating player
     * @param victim killed entity
     * @param location effect origin (cloned defensively)
     * @param effect effect that started
     */
    public KillEffectTriggerEvent(Player killer, Entity victim, Location location, Effect effect) {
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
     * @return effect that started
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
