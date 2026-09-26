package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.PurchaseResult;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Economy balances, effect ownership, and purchases.
 *
 * <p>When {@link #enabled()} is {@code false}, purchase/ownership semantics follow KTPlus config
 * (effects may be treated as free / permission-only).
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface EconomyService {
    /**
     * @return {@code true} if the economy subsystem is active
     */
    boolean enabled();

    /**
     * @return provider id currently in use
     */
    String providerId();

    /**
     * @param player online player
     * @return current balance ({@code >= 0})
     */
    long balance(Player player);

    /**
     * @param playerId player UUID
     * @return current balance ({@code >= 0})
     */
    long balance(UUID playerId);

    /**
     * Sets the absolute balance for the player.
     *
     * <p><b>Side effects:</b> persists balance via the active provider.
     *
     * @param playerId player UUID
     * @param balance new balance ({@code >= 0} recommended)
     */
    void setBalance(UUID playerId, long balance);

    /**
     * Adds (or subtracts if negative) to the player's balance.
     *
     * @param playerId player UUID
     * @param amount delta to apply
     */
    void addBalance(UUID playerId, long amount);

    /**
     * Whether the player already owns the effect (excluding free-by-price semantics).
     *
     * @param player player
     * @param effect effect
     * @return {@code true} if owned
     */
    boolean owns(Player player, Effect effect);

    /**
     * Whether the player owns the effect or it is free ({@link Effect#free()}).
     *
     * @param player player
     * @param effect effect
     * @return {@code true} if usable without purchase
     */
    boolean ownsOrFree(Player player, Effect effect);

    /**
     * Attempts to purchase the effect for the player.
     *
     * <p><b>Side effects:</b> may withdraw funds, grant ownership, fire purchase events.
     *
     * @param player purchaser
     * @param effect effect to buy
     * @return purchase outcome
     */
    PurchaseResult purchase(Player player, Effect effect);
}
