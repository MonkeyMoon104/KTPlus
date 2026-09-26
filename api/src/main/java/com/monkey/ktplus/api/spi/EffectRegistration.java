package com.monkey.ktplus.api.spi;

import com.monkey.ktplus.api.builder.EffectRegistrationBuilders;
import com.monkey.ktplus.api.model.Effect;
import java.util.List;
import java.util.Objects;

/**
 * Bundle of effect metadata, executor, and optional lookup aliases for registration.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * EffectRegistration registration = EffectRegistration.builder()
 *         .effect(Effect.builder("shockwave")
 *                 .displayName("Shockwave")
 *                 .icon(Material.TNT)
 *                 .build())
 *         .executor(ctx -> {
 *             // spawn particles / play sounds using ctx
 *         })
 *         .alias("sw")
 *         .build();
 * KtPlusProvider.get().registration().register(registration);
 * }</pre>
 *
 * @param effect effect descriptor to expose in the catalog and GUI
 * @param executor callback invoked on the main thread when the effect plays
 * @param aliases additional id tokens that resolve to this effect (immutable copy)
 * @since 4.0.3
 * @see EffectRegistrationService
 */
public record EffectRegistration(Effect effect, EffectExecutor executor, List<String> aliases) {
    /**
     * Compact constructor: copies {@code aliases} defensively.
     *
     * @throws NullPointerException if any component is null
     */
    public EffectRegistration {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(aliases, "aliases");
        aliases = List.copyOf(aliases);
    }

    /**
     * Starts a staged builder for a new registration.
     *
     * @return effect stage
     */
    public static EffectRegistrationBuilders.EffectStage builder() {
        return EffectRegistrationBuilders.start();
    }

    /**
     * Returns aliases as a fresh array suitable for varargs registration APIs.
     *
     * @return alias array (possibly empty, never null)
     */
    public String[] aliasArray() {
        return aliases.toArray(String[]::new);
    }
}
