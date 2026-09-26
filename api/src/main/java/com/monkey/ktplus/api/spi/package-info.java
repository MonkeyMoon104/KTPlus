/**
 * Service-provider interfaces for registering and executing custom kill effects.
 *
 * <p>Implement {@link com.monkey.ktplus.api.spi.EffectExecutor}, wrap it in {@link
 * com.monkey.ktplus.api.spi.EffectRegistration}, and register via {@link
 * com.monkey.ktplus.api.spi.EffectRegistrationService}.
 *
 * @since 4.0.3
 */
@org.jspecify.annotations.NullMarked
package com.monkey.ktplus.api.spi;
