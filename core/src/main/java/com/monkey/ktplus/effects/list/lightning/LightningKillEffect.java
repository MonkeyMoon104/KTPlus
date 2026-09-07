package com.monkey.ktplus.effects.list.lightning;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.lightning.animation.LightningRadarWave;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;

public final class LightningKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;

    public LightningKillEffect(EffectDefinition definition, VisualEffectService visuals) {
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
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        world.strikeLightningEffect(loc);
        visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", loc, 1.8f, 1.0f);
        visuals.particle("ELECTRIC_SPARK", loc.clone().add(0, 1.0, 0), 40, 0.4, 1.0, 0.4, 0.08, null);
        visuals.particle("ENCHANTED_HIT", loc, 24, 0.5, 0.8, 0.5, 0.15, null);

        LightningSettings settings = LightningSettings.from(context.config().effectSection("lightning"));
        LightningRadarWave.start(session, visuals, context.killer(), loc, settings);
    }
}
