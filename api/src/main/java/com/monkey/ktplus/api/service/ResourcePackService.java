package com.monkey.ktplus.api.service;

import org.jspecify.annotations.Nullable;

/**
 * Resource-pack configuration and custom sound name resolution.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface ResourcePackService {
    /**
     * @return {@code true} if a resource pack is configured for delivery
     */
    boolean enabled();

    /**
     * @return {@code true} if accepting the pack is required to play
     */
    boolean required();

    /**
     * @return pack download URL, or {@code null} if unset
     */
    @Nullable String url();

    /**
     * @return SHA-1 hash of the pack, or {@code null} if unset
     */
    @Nullable String sha1();

    /**
     * @return prompt text shown to the player, or {@code null} if unset
     */
    @Nullable String prompt();

    /**
     * Resolves a logical sound key to a Minecraft / resource-pack sound name.
     *
     * @param key logical sound key
     * @return sound name, or {@code null} if unknown
     */
    @Nullable String soundName(String key);
}
