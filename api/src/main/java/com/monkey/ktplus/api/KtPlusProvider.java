package com.monkey.ktplus.api;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Static holder for the single loaded {@link KtPlus} API instance.
 *
 * <p>KTPlus registers itself on enable and clears the holder on disable. Soft-depend plugins should
 * prefer {@link #getOptional()} or {@link #isAvailable()} over {@link #get()} when KTPlus may be
 * absent.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * KtPlusProvider.getOptional().ifPresent(api -> {
 *     api.gui().open(player);
 * });
 * }</pre>
 *
 * <p><b>Thread-safety:</b> registration/unregistration are performed by KTPlus on the main thread
 * during lifecycle events. After registration, reading via {@link #get()} / {@link #getOptional()}
 * is safe from any thread, but service methods still follow Bukkit threading rules.
 *
 * @since 4.0.3
 * @see KtPlus
 */
public final class KtPlusProvider {
    private static @Nullable KtPlus instance;

    private KtPlusProvider() {}

    /**
     * Returns the registered API, or fails fast if KTPlus is not loaded.
     *
     * @return the live {@link KtPlus} facade
     * @throws IllegalStateException if no API has been {@linkplain #register(KtPlus) registered}
     */
    public static KtPlus get() {
        KtPlus api = instance;
        if (api == null) {
            throw new IllegalStateException("KTPlus API is not loaded");
        }
        return api;
    }

    /**
     * Returns the registered API when present.
     *
     * @return optional containing the API, or empty if KTPlus is not loaded
     */
    public static Optional<KtPlus> getOptional() {
        return Optional.ofNullable(instance);
    }

    /**
     * Checks whether an API instance is currently registered.
     *
     * @return {@code true} if {@link #get()} would succeed
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    /**
     * Registers the API instance. Intended for KTPlus plugin bootstrap only.
     *
     * <p><b>Side effects:</b> stores {@code api} as the global singleton.
     *
     * @param api non-null facade implementation
     * @throws NullPointerException if {@code api} is null
     * @throws IllegalStateException if an API is already registered
     */
    public static void register(KtPlus api) {
        Objects.requireNonNull(api, "api");
        if (instance != null) {
            throw new IllegalStateException("KTPlus API is already registered");
        }
        instance = api;
    }

    /**
     * Clears the registered API when {@code api} is the current instance.
     *
     * <p>No-op if a different instance is registered (defensive against mismatched unload).
     *
     * @param api the instance previously passed to {@link #register(KtPlus)}
     * @throws NullPointerException if {@code api} is null
     */
    public static void unregister(KtPlus api) {
        Objects.requireNonNull(api, "api");
        if (instance == api) {
            instance = null;
        }
    }
}
