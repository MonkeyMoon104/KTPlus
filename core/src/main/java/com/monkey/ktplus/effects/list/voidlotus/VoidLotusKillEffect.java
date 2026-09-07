package com.monkey.ktplus.effects.list.voidlotus;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.voidlotus.animation.VoidLotusAnimation;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.Location;

public final class VoidLotusKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;

    public VoidLotusKillEffect(EffectDefinition definition, VisualEffectService visuals) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        Location loc = context.location();
        if (loc.getWorld() == null) {
            return;
        }
        VoidLotusAnimation.launch(
                session, visuals, loc.clone().add(0.5, 0.2, 0.5), context.killer(), context.config());
    }
}
