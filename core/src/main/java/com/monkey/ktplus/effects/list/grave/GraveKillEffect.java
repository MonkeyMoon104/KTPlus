package com.monkey.ktplus.effects.list.grave;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.grave.animation.GraveLauncher;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.RepeatingParticles;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Location;

public final class GraveKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;

    public GraveKillEffect(EffectDefinition definition, VisualEffectService visuals) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        Location loc = context.location().clone();
        if (loc.getWorld() == null) {
            return;
        }
        visuals.sound("ENTITY_WITHER_BREAK_BLOCK", loc, 2.0f, 1.0f);
        RepeatingParticles.play(session, visuals, "LARGE_SMOKE", loc, 10, 1.0, 1.0, 1.0, 0.02, 1L, 10, Color.GRAY);
        RepeatingParticles.play(session, visuals, "SOUL", loc, 20, 1.0, 1.0, 1.0, 0.02, 1L, 10, Color.GRAY);
        GraveLauncher.launch(session, visuals, context, loc.clone().add(0.5, 0, 0.5));
    }
}
