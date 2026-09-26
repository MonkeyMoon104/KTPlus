package com.monkey.ktplus.api.service;

/**
 * Presence and settings of optional soft-depend integrations.
 *
 * <p>Values reflect plugin detection and config flags at the last load/reload.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface HookService {
    /**
     * @return {@code true} if Vault is present
     */
    boolean vaultPresent();

    /**
     * @return {@code true} if LuckPerms is present
     */
    boolean luckPermsPresent();

    /**
     * @return {@code true} if WorldGuard is present
     */
    boolean worldGuardPresent();

    /**
     * @return {@code true} if PlaceholderAPI is present
     */
    boolean placeholderApiPresent();

    /**
     * @return {@code true} if LuckPerms permission grants on purchase are enabled in config
     */
    boolean luckPermsGrantOnPurchase();

    /**
     * WorldGuard block-change protection mode string from config.
     *
     * @return mode token (implementation-defined, e.g. {@code deny}, {@code allow})
     */
    String worldGuardBlockChangesMode();
}
