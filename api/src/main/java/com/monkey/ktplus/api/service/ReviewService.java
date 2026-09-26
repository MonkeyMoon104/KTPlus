package com.monkey.ktplus.api.service;

/**
 * Status of the optional review / feedback integration.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface ReviewService {
    /**
     * @return {@code true} if review features are available in this build/environment
     */
    boolean available();
}
