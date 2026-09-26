package com.monkey.ktplus.api.spi;

/**
 * Functional SPI invoked when a registered kill effect starts playing.
 *
 * <p>Implementations should schedule visual/audio work via {@link EffectPlayContext#session()}
 * helpers rather than blocking. Called on the Bukkit main thread (or Folia-appropriate region
 * thread) when the runtime starts the session.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * EffectExecutor executor = context -> {
 *     Player killer = context.killer();
 *     Location at = context.location();
 *     context.session().runLater(20L, () -> {
 *         // delayed flourish
 *     });
 * };
 * }</pre>
 *
 * @since 4.0.3
 * @see EffectPlayContext
 * @see EffectRegistration
 */
@FunctionalInterface
public interface EffectExecutor {
    /**
     * Runs the effect logic for a newly started session.
     *
     * <p><b>Side effects:</b> may spawn entities, particles, sounds, or schedule further tasks via
     * {@code context.session()}.
     *
     * @param context non-null play context for this invocation
     */
    void execute(EffectPlayContext context);
}
