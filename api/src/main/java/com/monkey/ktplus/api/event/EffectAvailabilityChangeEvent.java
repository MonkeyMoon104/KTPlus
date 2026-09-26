package com.monkey.ktplus.api.event;

import java.util.Objects;
import org.bukkit.event.HandlerList;

/**
 * Fired when an effect's runtime availability (enabled/disabled) changes.
 *
 * <p><b>When:</b> after {@link com.monkey.ktplus.api.service.AvailabilityService#enable} / {@link
 * com.monkey.ktplus.api.service.AvailabilityService#disable} (or equivalent) updates state.
 *
 * <p><b>Sync:</b> synchronous on the main thread. <b>Cancellable:</b> no.
 *
 * @since 4.0.3
 */
public final class EffectAvailabilityChangeEvent extends KtPlusEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String effectId;
    private final boolean enabled;

    /**
     * Creates an availability change event.
     *
     * @param effectId affected effect id
     * @param enabled new enabled state
     */
    public EffectAvailabilityChangeEvent(String effectId, boolean enabled) {
        this.effectId = Objects.requireNonNull(effectId, "effectId");
        this.enabled = enabled;
    }

    /**
     * @return effect id whose availability changed
     */
    public String getEffectId() {
        return effectId;
    }

    /**
     * @return {@code true} if the effect is now enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return {@code true} if the effect is now disabled
     */
    public boolean isDisabled() {
        return !enabled;
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
