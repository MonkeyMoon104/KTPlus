package com.monkey.ktplus.api.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Optional random world events that may accompany kill-effect playback.
 *
 * <p>Gated {@link RuntimeService#play} may call {@link #tryTrigger} after a successful start.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface RandomEventService {
    /**
     * @return {@code true} if random events are enabled in config
     */
    boolean enabled();

    /**
     * Attempts to trigger a random event at the location for the killer.
     *
     * <p><b>Side effects:</b> may spawn world effects subject to chance and config.
     *
     * @param killer player context
     * @param location event origin
     */
    void tryTrigger(Player killer, Location location);
}
