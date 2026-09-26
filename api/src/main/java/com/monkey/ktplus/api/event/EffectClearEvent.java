package com.monkey.ktplus.api.event;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

/**
 * Fired when a player's selected kill effect is cleared.
 *
 * <p><b>When:</b> after {@link
 * com.monkey.ktplus.api.service.PlayerDataService#clearSelectedEffect} (or equivalent internal
 * clear) persists.
 *
 * <p><b>Sync:</b> synchronous on the main thread. <b>Cancellable:</b> no.
 *
 * @since 4.0.3
 */
public final class EffectClearEvent extends KtPlusEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final @Nullable Player player;
    private final @Nullable String previousEffectId;

    /**
     * Creates a clear event.
     *
     * @param playerId player UUID
     * @param player online player if available; may be {@code null}
     * @param previousEffectId previously selected effect id, or {@code null} if none
     */
    public EffectClearEvent(UUID playerId, @Nullable Player player, @Nullable String previousEffectId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.player = player;
        this.previousEffectId = previousEffectId;
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
     * @return effect id that was cleared, or {@code null} if the player had no selection
     */
    public @Nullable String getPreviousEffectId() {
        return previousEffectId;
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
