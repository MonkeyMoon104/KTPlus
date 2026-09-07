package com.monkey.ktplus.effects.list.aurafarming;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.aurafarming.animation.AuraTrailAnimation;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.resourcepack.ResourcePackSettings;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class AuraFarmingKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;

    public AuraFarmingKillEffect(EffectDefinition definition, VisualEffectService visuals) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        Player killer = context.killer();
        if (killer == null || !killer.isOnline()) {
            return;
        }
        String soundName = ResourcePackSettings.soundName(context.config().resourcePack(), "aura-farm", "kt.aurab");
        EntityCompat.playSound(killer, context.location(), soundName, 1.0f, 1.0f);
        session.onCleanup(() -> EntityCompat.stopSound(killer, soundName, "PLAYERS"));
        session.resetDeadline(420L);
        AuraTrailAnimation.start(session, visuals, context, context.location().clone());
    }
}
