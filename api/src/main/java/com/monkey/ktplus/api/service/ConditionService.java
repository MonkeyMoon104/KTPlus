package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.Effect;
import org.bukkit.entity.Player;

/**
 * World, game-mode, and per-effect trigger conditions used by gated playback.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 * @see RuntimeService#play
 */
public interface ConditionService {
    /**
     * Whether the player's current world allows kill effects.
     *
     * @param player player to check
     * @return {@code true} if the world is allowed
     */
    boolean allowsWorld(Player player);

    /**
     * Whether the player's game mode allows kill effects.
     *
     * @param player player to check
     * @return {@code true} if the game mode is allowed
     */
    boolean allowsGameMode(Player player);

    /**
     * Whether additional per-effect trigger rules allow this play (e.g. region flags).
     *
     * @param player activating player
     * @param effect effect to evaluate
     * @return {@code true} if the trigger is allowed
     */
    boolean allowsTrigger(Player player, Effect effect);
}
