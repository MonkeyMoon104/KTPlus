package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.EffectCategory;
import java.util.List;

/**
 * Read-only view of key KTPlus {@code config.yml} settings.
 *
 * <p>Values reflect the last loaded/reloaded snapshot; they do not mutate config files.
 *
 * <p><b>Thread-safety:</b> safe to read from the main thread; treat as an immutable snapshot for a
 * given reload generation.
 *
 * @since 4.0.3
 */
public interface ConfigService {
    /**
     * @return configured config schema / file version string
     */
    String configVersion();

    /**
     * @return default language code (e.g. {@code en})
     */
    String defaultLanguage();

    /**
     * @return {@code true} if cosmetic mode is enabled globally
     */
    boolean cosmeticMode();

    /**
     * @return {@code true} if kill effects may trigger on mob kills
     */
    boolean effectsOnMobs();

    /**
     * @return {@code true} if the economy subsystem is enabled
     */
    boolean economyEnabled();

    /**
     * @return economy provider id (e.g. {@code vault}, {@code internal})
     */
    String economyProvider();

    /**
     * @return starting balance granted to new players when using the internal economy
     */
    int startingBalance();

    /**
     * @return currency reward for killing a player
     */
    int killRewardPlayer();

    /**
     * @return currency reward for killing a mob
     */
    int killRewardMob();

    /**
     * @return gated effect cooldown duration in milliseconds
     */
    long effectCooldownMillis();

    /**
     * @param worldName world name
     * @return {@code true} if kill effects are disabled in that world
     */
    boolean isWorldDisabled(String worldName);

    /**
     * @return configured category display order
     */
    List<EffectCategory> effectCategories();
}
