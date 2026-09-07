package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.blackhole.BlackHoleKillEffect;
import com.monkey.ktplus.effects.list.bloodmoon.BloodMoonKillEffect;
import com.monkey.ktplus.effects.list.clockrewind.ClockRewindKillEffect;
import com.monkey.ktplus.effects.list.enchantcolumn.EnchantColumnKillEffect;
import com.monkey.ktplus.effects.list.glowmissile.GlowMissileKillEffect;
import com.monkey.ktplus.effects.list.glowmissile.animation.GlowMissileLauncher;
import com.monkey.ktplus.effects.list.quillstorm.QuillStormKillEffect;
import com.monkey.ktplus.effects.list.shockwave.ShockwaveKillEffect;
import com.monkey.ktplus.effects.list.sniper.SniperKillEffect;
import com.monkey.ktplus.effects.list.solarflare.SolarFlareKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;

public final class VeryRareEffectsRegistration {
    private VeryRareEffectsRegistration() {}

    public static void register(
            EffectRegistry registry, EffectDefinitionFactory definitions, VisualEffectService visuals) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        registry.register(new EnchantColumnKillEffect(
                definitions.definition(
                        "enchantcolumn", "Enchant Column", "ENCHANTING_TABLE", EffectCategory.VERY_RARE, 5000, true, 280),
                visuals));
        registry.register(new GlowMissileKillEffect(
                definitions.definition(
                        "glowmissile",
                        "Glow Missile",
                        "GLOWSTONE_DUST",
                        EffectCategory.VERY_RARE,
                        3000,
                        false,
                        GlowMissileLauncher.EFFECT_DURATION_TICKS),
                visuals));
        registry.register(new ShockwaveKillEffect(
                definitions.definition(
                        "shockwave", "Shockwave", "REDSTONE", EffectCategory.VERY_RARE, 4000, false, 140),
                visuals));
        registry.register(new SniperKillEffect(
                definitions.definition("sniper", "Sniper", "BOW", EffectCategory.VERY_RARE, 2000, false, 220),
                visuals));
        registry.register(new BlackHoleKillEffect(
                definitions.definition(
                        "blackhole", "Black Hole", "BLACK_CONCRETE", EffectCategory.VERY_RARE, 4500, false, 220),
                visuals));
        registry.register(new ClockRewindKillEffect(
                definitions.definition("clockrewind", "Clock Rewind", "CLOCK", EffectCategory.VERY_RARE, 4200, false, 160),
                visuals));
        registry.register(new SolarFlareKillEffect(
                definitions.definition("solarflare", "Solar Flare", "GLOWSTONE", EffectCategory.VERY_RARE, 4800, false, 140),
                visuals));
        registry.register(new QuillStormKillEffect(
                definitions.definition("quillstorm", "Quill Storm", "ARROW", EffectCategory.VERY_RARE, 4600, false, 150),
                visuals));
        registry.register(new BloodMoonKillEffect(
                definitions.definition(
                        "bloodmoon", "Blood Moon", "REDSTONE_BLOCK", EffectCategory.VERY_RARE, 5200, true, 200),
                visuals));
    }
}
