package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.chronosphere.ChronosphereKillEffect;
import com.monkey.ktplus.effects.list.cosmicfinale.CosmicFinaleKillEffect;
import com.monkey.ktplus.effects.list.judgment.JudgmentKillEffect;
import com.monkey.ktplus.effects.list.realityglitch.RealityGlitchKillEffect;
import com.monkey.ktplus.effects.list.warden.WardenKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;

public final class UltraEffectsRegistration {
    private UltraEffectsRegistration() {}

    public static void register(
            EffectRegistry registry, EffectDefinitionFactory definitions, VisualEffectService visuals) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        registry.register(new WardenKillEffect(
                definitions.definition("warden", "Warden", "SCULK_SHRIEKER", EffectCategory.ULTRA, 50000, true, 120),
                visuals));
        registry.register(new ChronosphereKillEffect(
                definitions.definition("chronosphere", "Chronosphere", "CLOCK", EffectCategory.ULTRA, 52000, true, 300),
                visuals));
        registry.register(new RealityGlitchKillEffect(
                definitions.definition(
                        "realityglitch", "Reality Glitch", "STRUCTURE_BLOCK", EffectCategory.ULTRA, 56000, true, 70),
                visuals));
        registry.register(new JudgmentKillEffect(
                definitions.definition("judgment", "Judgment", "GOLD_BLOCK", EffectCategory.ULTRA, 62000, true, 300),
                visuals));
        registry.register(new CosmicFinaleKillEffect(
                definitions.definition(
                        "cosmicfinale", "Cosmic Finale", "NETHER_STAR", EffectCategory.ULTRA, 75000, true, 280),
                visuals));
    }
}
