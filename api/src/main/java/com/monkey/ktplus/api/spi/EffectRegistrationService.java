package com.monkey.ktplus.api.spi;

import com.monkey.ktplus.api.model.Effect;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for registering and querying externally contributed kill effects.
 *
 * <p>External effects appear alongside builtins in {@link
 * com.monkey.ktplus.api.service.EffectService} and can be selected / played through the normal
 * runtime. Unregister on your plugin disable to avoid leaking executors.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 * @see EffectRegistration
 * @see EffectExecutor
 */
public interface EffectRegistrationService {
    /**
     * Registers an effect with its executor and optional aliases.
     *
     * <p><b>Side effects:</b> adds the effect to the live catalog; may replace a prior external
     * registration for the same id depending on implementation policy.
     *
     * @param effect effect descriptor
     * @param executor play callback
     * @param aliases optional alternate lookup ids
     */
    void register(Effect effect, EffectExecutor executor, String... aliases);

    /**
     * Registers a pre-built {@link EffectRegistration}.
     *
     * @param registration non-null registration bundle
     * @throws NullPointerException if {@code registration} is null
     */
    default void register(EffectRegistration registration) {
        Objects.requireNonNull(registration, "registration");
        register(registration.effect(), registration.executor(), registration.aliasArray());
    }

    /**
     * Unregisters an externally registered effect by id.
     *
     * @param effectId effect id (case handling matches catalog lookup)
     * @return {@code true} if an external registration was removed
     */
    boolean unregister(String effectId);

    /**
     * Whether any effect (builtin or external) is registered under the id/alias.
     *
     * @param effectId id or alias
     * @return {@code true} if registered
     */
    boolean isRegistered(String effectId);

    /**
     * Whether the id refers to an <em>external</em> (SPI-registered) effect rather than a builtin.
     *
     * @param effectId effect id
     * @return {@code true} if external
     */
    boolean isExternal(String effectId);

    /**
     * Ids of all currently registered external effects.
     *
     * @return unmodifiable or defensive collection of external ids
     */
    Collection<String> externalIds();

    /**
     * Looks up an external effect descriptor by id.
     *
     * @param effectId effect id
     * @return the effect if externally registered
     */
    Optional<Effect> findExternal(String effectId);
}
