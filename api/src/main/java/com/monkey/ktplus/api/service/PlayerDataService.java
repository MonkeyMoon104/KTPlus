package com.monkey.ktplus.api.service;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Persistent per-player selected-effect storage.
 *
 * <p>Clearing selection fires {@link com.monkey.ktplus.api.event.EffectClearEvent}. Prefer {@link
 * AccessService#select} for user-facing selection that includes purchase/permission gates.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface PlayerDataService {
    /**
     * @param player online player
     * @return selected effect id if any
     */
    Optional<String> selectedEffect(Player player);

    /**
     * @param playerId player UUID
     * @return selected effect id if any
     */
    Optional<String> selectedEffect(UUID playerId);

    /**
     * Sets the player's selected effect id without purchase checks.
     *
     * <p><b>Side effects:</b> persists selection.
     *
     * @param player online player
     * @param effectId effect id to select
     */
    void setSelectedEffect(Player player, String effectId);

    /**
     * Sets the player's selected effect id without purchase checks.
     *
     * @param playerId player UUID
     * @param effectId effect id to select
     */
    void setSelectedEffect(UUID playerId, String effectId);

    /**
     * Clears the player's selection.
     *
     * <p><b>Side effects:</b> persists clear and may fire {@link
     * com.monkey.ktplus.api.event.EffectClearEvent}.
     *
     * @param player online player
     */
    void clearSelectedEffect(Player player);

    /**
     * Clears the player's selection by UUID.
     *
     * @param playerId player UUID
     */
    void clearSelectedEffect(UUID playerId);
}
