package com.monkey.ktplus.api;

import com.monkey.ktplus.api.service.AccessService;
import com.monkey.ktplus.api.service.AvailabilityService;
import com.monkey.ktplus.api.service.ConditionService;
import com.monkey.ktplus.api.service.ConfigService;
import com.monkey.ktplus.api.service.CooldownService;
import com.monkey.ktplus.api.service.EconomyService;
import com.monkey.ktplus.api.service.EffectService;
import com.monkey.ktplus.api.service.GuiService;
import com.monkey.ktplus.api.service.HookService;
import com.monkey.ktplus.api.service.LangService;
import com.monkey.ktplus.api.service.PermissionService;
import com.monkey.ktplus.api.service.PlayerDataService;
import com.monkey.ktplus.api.service.RandomEventService;
import com.monkey.ktplus.api.service.ResourcePackService;
import com.monkey.ktplus.api.service.ReviewService;
import com.monkey.ktplus.api.service.RuntimeService;
import com.monkey.ktplus.api.spi.EffectRegistrationService;
import org.bukkit.plugin.Plugin;

/**
 * Root facade for the KTPlus public API.
 *
 * <p>Obtain an instance via {@link KtPlusProvider} after the KTPlus plugin has enabled. All service
 * accessors return live, plugin-backed implementations — never cache them across plugin reloads.
 *
 * <p><b>Thread-safety:</b> unless a specific service documents otherwise, call API methods on the
 * Bukkit main thread (or the Folia entity/region thread for the involved players).
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * if (!KtPlusProvider.isAvailable()) {
 *     return;
 * }
 * KtPlus api = KtPlusProvider.get();
 * api.effects().find("lightning").ifPresent(effect ->
 *         api.runtime().playForced(killer, victim, location, effect));
 * }</pre>
 *
 * @since 4.0.3
 * @see KtPlusProvider
 */
public interface KtPlus {
    /**
     * Returns the running KTPlus plugin version string (e.g. {@code 4.0.3}).
     *
     * @return non-blank plugin version
     */
    String version();

    /**
     * Returns the Bukkit {@link Plugin} instance that owns this API.
     *
     * @return the KTPlus plugin
     */
    Plugin plugin();

    /**
     * Lookup and enumeration of registered kill effects (builtin and external).
     *
     * @return effect catalog service
     */
    EffectService effects();

    /**
     * SPI for third-party plugins to register or unregister custom kill effects.
     *
     * @return registration service
     */
    EffectRegistrationService registration();

    /**
     * Persistent per-player selection state (selected effect id).
     *
     * @return player-data service
     */
    PlayerDataService players();

    /**
     * Economy balances, ownership, and purchases.
     *
     * @return economy service
     */
    EconomyService economy();

    /**
     * Permission/economy gating for activating and selecting effects.
     *
     * @return access service
     */
    AccessService access();

    /**
     * Runtime enable/disable toggles for individual effects.
     *
     * @return availability service
     */
    AvailabilityService availability();

    /**
     * Localized messages and effect display text.
     *
     * @return language service
     */
    LangService lang();

    /**
     * Play, cancel, and inspect active effect sessions.
     *
     * @return runtime service
     */
    RuntimeService runtime();

    /**
     * Read-only snapshot of key {@code config.yml} values.
     *
     * @return config service
     */
    ConfigService config();

    /**
     * Effect permission checks and admin bypass.
     *
     * @return permission service
     */
    PermissionService permissions();

    /**
     * Open/close the effect selection GUI.
     *
     * @return GUI service
     */
    GuiService gui();

    /**
     * Per-player cooldown keys (including the global effect trigger cooldown).
     *
     * @return cooldown service
     */
    CooldownService cooldowns();

    /**
     * World / game-mode / trigger condition checks used by gated playback.
     *
     * @return condition service
     */
    ConditionService conditions();

    /**
     * Presence and settings of optional soft-depend hooks (Vault, LuckPerms, etc.).
     *
     * @return hook service
     */
    HookService hooks();

    /**
     * Optional random world events that may fire alongside kill effects.
     *
     * @return random-event service
     */
    RandomEventService randomEvents();

    /**
     * Resource-pack URL, hash, and custom sound name resolution.
     *
     * @return resource-pack service
     */
    ResourcePackService resourcePack();

    /**
     * Review / feedback integration status.
     *
     * @return review service
     */
    ReviewService reviews();
}
