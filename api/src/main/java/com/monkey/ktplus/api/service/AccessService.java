package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.PurchaseResult;
import com.monkey.ktplus.api.model.SelectionResult;
import org.bukkit.entity.Player;

/**
 * High-level gating for activating and selecting effects (permissions + economy).
 *
 * <p>Use {@link #canActivate} before playing; use {@link #select} for GUI / command selection flows
 * that may purchase then select.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface AccessService {
    /**
     * Whether the player may activate (play) the effect right now.
     *
     * <p>Typically requires permission and ownership (or free effect / admin bypass).
     *
     * @param player player to check
     * @param effect target effect
     * @return {@code true} if activation is allowed
     */
    boolean canActivate(Player player, Effect effect);

    /**
     * Attempts to unlock the effect for selection (purchase if needed) without selecting it.
     *
     * <p><b>Side effects:</b> may withdraw funds and grant ownership; may fire purchase events.
     *
     * @param player purchasing player
     * @param effect target effect
     * @return purchase outcome
     */
    PurchaseResult unlockForSelection(Player player, Effect effect);

    /**
     * Selects the effect for the player, purchasing if required by economy settings.
     *
     * <p><b>Side effects:</b> may purchase, persist selection, and fire {@link
     * com.monkey.ktplus.api.event.EffectSelectEvent} / purchase events.
     *
     * @param player selecting player
     * @param effect target effect
     * @return selection outcome
     */
    SelectionResult select(Player player, Effect effect);
}
