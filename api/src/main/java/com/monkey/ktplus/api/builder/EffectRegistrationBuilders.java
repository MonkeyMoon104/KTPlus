package com.monkey.ktplus.api.builder;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.spi.EffectExecutor;
import com.monkey.ktplus.api.spi.EffectRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Staged fluent builders for {@link EffectRegistration}.
 *
 * <p>Prefer {@link EffectRegistration#builder()} at call sites. An effect descriptor and an {@link
 * EffectExecutor} are required; aliases are optional.
 *
 * @since 4.0.3
 */
public final class EffectRegistrationBuilders {
    private EffectRegistrationBuilders() {}

    /**
     * Begins a new registration builder.
     *
     * @return effect stage
     */
    public static EffectStage start() {
        return new Stages();
    }

    /**
     * First stage: attach the effect descriptor.
     *
     * @since 4.0.3
     */
    public interface EffectStage {
        /**
         * Sets the effect metadata to register.
         *
         * @param effect non-null effect
         * @return executor stage
         * @throws NullPointerException if {@code effect} is null
         */
        ExecutorStage effect(Effect effect);
    }

    /**
     * Second stage: attach the runtime executor.
     *
     * @since 4.0.3
     */
    public interface ExecutorStage {
        /**
         * Sets the callback invoked when the effect plays.
         *
         * @param executor non-null executor
         * @return build stage
         * @throws NullPointerException if {@code executor} is null
         */
        BuildStage executor(EffectExecutor executor);
    }

    /**
     * Final stage: optional aliases and {@link #build()}.
     *
     * @since 4.0.3
     */
    public interface BuildStage {
        /**
         * Adds a single lookup alias (blank aliases are ignored).
         *
         * @param alias alias token
         * @return this stage
         * @throws NullPointerException if {@code alias} is null
         */
        BuildStage alias(String alias);

        /**
         * Adds multiple aliases.
         *
         * @param aliases alias tokens
         * @return this stage
         * @throws NullPointerException if {@code aliases} is null
         */
        BuildStage aliases(String... aliases);

        /**
         * Adds multiple aliases from an iterable.
         *
         * @param aliases alias tokens
         * @return this stage
         * @throws NullPointerException if {@code aliases} is null
         */
        BuildStage aliases(Iterable<String> aliases);

        /**
         * Builds an immutable {@link EffectRegistration}.
         *
         * @return registration ready for {@link
         *     com.monkey.ktplus.api.spi.EffectRegistrationService#register(EffectRegistration)}
         * @throws IllegalStateException if effect or executor was not set
         */
        EffectRegistration build();
    }

    private static final class Stages implements EffectStage, ExecutorStage, BuildStage {
        private @Nullable Effect effect;
        private @Nullable EffectExecutor executor;
        private final List<String> aliases = new ArrayList<>();

        @Override
        public ExecutorStage effect(Effect effect) {
            this.effect = Objects.requireNonNull(effect, "effect");
            return this;
        }

        @Override
        public BuildStage executor(EffectExecutor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        @Override
        public BuildStage alias(String alias) {
            Objects.requireNonNull(alias, "alias");
            String trimmed = alias.trim();
            if (!trimmed.isEmpty()) {
                aliases.add(trimmed);
            }
            return this;
        }

        @Override
        public BuildStage aliases(String... aliases) {
            Objects.requireNonNull(aliases, "aliases");
            for (String alias : aliases) {
                alias(alias);
            }
            return this;
        }

        @Override
        public BuildStage aliases(Iterable<String> aliases) {
            Objects.requireNonNull(aliases, "aliases");
            for (String alias : aliases) {
                alias(alias);
            }
            return this;
        }

        @Override
        public EffectRegistration build() {
            if (effect == null) {
                throw new IllegalStateException("effect is required");
            }
            if (executor == null) {
                throw new IllegalStateException("executor is required");
            }
            return new EffectRegistration(effect, executor, aliases);
        }
    }
}
