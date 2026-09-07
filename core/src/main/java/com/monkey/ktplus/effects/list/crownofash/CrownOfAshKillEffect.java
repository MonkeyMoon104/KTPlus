package com.monkey.ktplus.effects.list.crownofash;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.crownofash.animation.CrownOfAshAnimation;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;

public final class CrownOfAshKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;

    public CrownOfAshKillEffect(EffectDefinition definition, VisualEffectService visuals) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        if (context.location().getWorld() == null) {
            return;
        }
        CrownOfAshAnimation.start(session, visuals, context);
    }
}
