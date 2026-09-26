package com.monkey.ktplus.api.service;

import java.time.Duration;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Per-player named cooldowns.
 *
 * <p>Gated playback uses {@link #EFFECT_KEY} for the global kill-effect cooldown. Custom keys are
 * free for integrations.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface CooldownService {
    /**
     * Key used by gated {@link RuntimeService#play} for the global effect cooldown.
     */
    String EFFECT_KEY = "effect";

    /**
     * @param player player to check
     * @param key cooldown key
     * @return {@code true} if no cooldown is active for the key
     */
    boolean ready(Player player, String key);

    /**
     * @param playerId player UUID
     * @param key cooldown key
     * @return {@code true} if no cooldown is active for the key
     */
    boolean ready(UUID playerId, String key);

    /**
     * Starts or refreshes a cooldown.
     *
     * <p><b>Side effects:</b> stores expiry for {@code player}/{@code key}.
     *
     * @param player player
     * @param key cooldown key
     * @param duration cooldown length
     */
    void set(Player player, String key, Duration duration);

    /**
     * Starts or refreshes a cooldown by UUID.
     *
     * @param playerId player UUID
     * @param key cooldown key
     * @param duration cooldown length
     */
    void set(UUID playerId, String key, Duration duration);

    /**
     * @param player player
     * @param key cooldown key
     * @return remaining milliseconds, or {@code 0} if ready
     */
    long remainingMillis(Player player, String key);

    /**
     * @param playerId player UUID
     * @param key cooldown key
     * @return remaining milliseconds, or {@code 0} if ready
     */
    long remainingMillis(UUID playerId, String key);

    /**
     * Clears all cooldowns for the player.
     *
     * @param player player
     */
    void clear(Player player);

    /**
     * Clears all cooldowns for the player id.
     *
     * @param playerId player UUID
     */
    void clear(UUID playerId);

    /**
     * Clears a single cooldown key for the player.
     *
     * @param player player
     * @param key cooldown key
     */
    void clearKey(Player player, String key);

    /**
     * Clears a single cooldown key for the player id.
     *
     * @param playerId player UUID
     * @param key cooldown key
     */
    void clearKey(UUID playerId, String key);
}
