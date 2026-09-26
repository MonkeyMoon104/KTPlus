package com.monkey.ktplus.api.event;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.PurchaseResult;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

/**
 * Fired around effect purchase attempts.
 *
 * <p><b>When:</b>
 *
 * <ul>
 *   <li><b>Pre</b> ({@link #isPre()} {@code true}): before funds are withdrawn / ownership granted.
 *       Cancellable — cancel to abort the purchase.
 *   <li><b>Post</b> ({@link #isPre()} {@code false}): after the attempt completes; {@link
 *       #getResult()} holds the outcome. Cancellation is ignored.
 * </ul>
 *
 * <p><b>Sync:</b> synchronous on the main thread.
 *
 * @since 4.0.3
 */
public final class EffectPurchaseEvent extends KtPlusEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Effect effect;
    private final boolean pre;
    private @Nullable PurchaseResult result;
    private boolean cancelled;

    /**
     * Creates a purchase event.
     *
     * @param player purchasing player
     * @param effect effect being purchased
     * @param pre {@code true} for the pre-purchase phase
     */
    public EffectPurchaseEvent(Player player, Effect effect, boolean pre) {
        this.player = Objects.requireNonNull(player, "player");
        this.effect = Objects.requireNonNull(effect, "effect");
        this.pre = pre;
    }

    /**
     * @return purchasing player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return effect involved
     */
    public Effect getEffect() {
        return effect;
    }

    /**
     * @return {@code true} if this is the cancellable pre-purchase phase
     */
    public boolean isPre() {
        return pre;
    }

    /**
     * Purchase outcome; typically set on the post event (may be {@code null} on pre).
     *
     * @return result, or {@code null} if not yet known
     */
    public @Nullable PurchaseResult getResult() {
        return result;
    }

    /**
     * Sets or overrides the purchase result (primarily for post events / internal use).
     *
     * @param result outcome, or {@code null} to clear
     */
    public void setResult(@Nullable PurchaseResult result) {
        this.result = result;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels the purchase only when {@link #isPre()} is {@code true}; no-op on post events.
     *
     * @param cancel whether to cancel
     */
    @Override
    public void setCancelled(boolean cancel) {
        if (!pre) {
            return;
        }
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
