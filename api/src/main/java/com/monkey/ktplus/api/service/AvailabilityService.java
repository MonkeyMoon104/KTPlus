package com.monkey.ktplus.api.service;

import java.util.Collection;
import java.util.Set;

/**
 * Runtime enable/disable toggles for individual effects.
 *
 * <p>Disabled effects fail gated {@link RuntimeService#play} checks and are typically hidden or
 * marked unavailable in the GUI. Changes fire {@link
 * com.monkey.ktplus.api.event.EffectAvailabilityChangeEvent}.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface AvailabilityService {
    /**
     * @param effectId effect id
     * @return {@code true} if the effect is enabled for gated play
     */
    boolean isEnabled(String effectId);

    /**
     * @param effectId effect id
     * @return {@code true} if the effect is disabled
     */
    boolean isDisabled(String effectId);

    /**
     * @return set of currently disabled effect ids
     */
    Set<String> disabledIds();

    /**
     * Disables the effect for gated playback.
     *
     * <p><b>Side effects:</b> persists toggle and may fire availability events.
     *
     * @param effectId effect id
     * @return {@code true} if state changed to disabled
     */
    boolean disable(String effectId);

    /**
     * Enables the effect for gated playback.
     *
     * <p><b>Side effects:</b> persists toggle and may fire availability events.
     *
     * @param effectId effect id
     * @return {@code true} if state changed to enabled
     */
    boolean enable(String effectId);

    /**
     * Returns the subset of {@code ids} that are currently enabled.
     *
     * @param ids candidate ids
     * @return enabled ids from the input
     */
    Collection<String> filterEnabled(Collection<String> ids);

    /**
     * Returns the subset of {@code knownIds} that are currently disabled.
     *
     * @param knownIds candidate ids
     * @return disabled ids from the input
     */
    Collection<String> filterDisabled(Collection<String> knownIds);
}
