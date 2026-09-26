package com.monkey.ktplus.api.bridge;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.spi.EffectExecutor;
import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.runtime.EffectSession;
import java.util.Objects;

public final class ApiRegisteredKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final Effect apiEffect;
    private final EffectExecutor executor;

    public ApiRegisteredKillEffect(Effect apiEffect, EffectExecutor executor) {
        this.apiEffect = Objects.requireNonNull(apiEffect, "apiEffect");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.definition = EffectMappings.toDefinition(apiEffect);
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        executor.execute(new BridgeEffectPlayContext(context, session, apiEffect));
    }

    public Effect apiEffect() {
        return apiEffect;
    }
}
