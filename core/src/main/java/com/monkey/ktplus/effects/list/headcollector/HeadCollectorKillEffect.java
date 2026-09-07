package com.monkey.ktplus.effects.list.headcollector;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.headcollector.animation.HeadIntroAnimation;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class HeadCollectorKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;
    private final HeadCollectorService collector;

    public HeadCollectorKillEffect(
            EffectDefinition definition, VisualEffectService visuals, HeadCollectorService collector) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.collector = Objects.requireNonNull(collector, "collector");
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        if (!(context.victim() instanceof Player victim)) {
            session.complete();
            return;
        }
        HeadCollectorSettings settings =
                HeadCollectorSettings.from(context.config().effectSection("headcollector"));
        int inherited = collector.inheritHeads(context.killer(), victim, settings);
        if (inherited > 0) {
            context.killer()
                    .sendMessage(context.config()
                            .message("headcollector-inherited")
                            .replace("%amount%", String.valueOf(inherited)));
        }
        HeadIntroAnimation.play(
                session, visuals, collector, context.killer(), victim, context.location(), settings);
    }
}
