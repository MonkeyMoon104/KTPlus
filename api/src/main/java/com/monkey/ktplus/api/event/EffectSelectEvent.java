package com.monkey.ktplus.api.event;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.SelectionResult;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

/**
 * Fired after a player selection attempt completes (success or denial).
 *
 * <p><b>When:</b> after {@link com.monkey.ktplus.api.service.AccessService#select} finishes and a
 * {@link SelectionResult} is determined.
 *
 * <p><b>Sync:</b> synchronous on the main thread. <b>Cancellable:</b> no (observe {@link
 * #getResult()} for outcome).
 *
 * @since 4.0.3
 */
public final class EffectSelectEvent extends KtPlusEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final @Nullable Player player;
    private final Effect effect;
    private final SelectionResult result;

    /**
     * Creates a selection outcome event.
     *
     * @param playerId player UUID (always present)
     * @param player online player if available; may be {@code null}
     * @param effect effect involved in the selection
     * @param result outcome of the attempt
     */
    public EffectSelectEvent(
            UUID playerId, @Nullable Player player, Effect effect, SelectionResult result) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.player = player;
        this.effect = Objects.requireNonNull(effect, "effect");
        this.result = Objects.requireNonNull(result, "result");
    }

    /**
     * @return player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return online player, or {@code null} if offline / unavailable
     */
    public @Nullable Player getPlayer() {
        return player;
    }

    /**
     * @return effect that was selected or denied
     */
    public Effect getEffect() {
        return effect;
    }

    /**
     * @return selection outcome
     */
    public SelectionResult getResult() {
        return result;
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
