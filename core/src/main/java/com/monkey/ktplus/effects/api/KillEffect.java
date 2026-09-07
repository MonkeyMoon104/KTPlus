package com.monkey.ktplus.effects.api;

import com.monkey.ktplus.effects.runtime.EffectSession;

public interface KillEffect {
    EffectDefinition definition();

    void execute(EffectContext context, EffectSession session);
}
