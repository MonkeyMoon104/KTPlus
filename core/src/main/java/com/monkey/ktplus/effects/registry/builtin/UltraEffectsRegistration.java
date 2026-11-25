package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.enchantcolumn.EnchantColumnKillEffect;
import com.monkey.ktplus.effects.list.glowmissile.GlowMissileKillEffect;
import com.monkey.ktplus.effects.list.glowmissile.animation.GlowMissileLauncher;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorKillEffect;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorService;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorSettings;
import com.monkey.ktplus.effects.list.sniper.SniperKillEffect;
import com.monkey.ktplus.effects.list.nightfall.NightfallKillEffect;
import com.monkey.ktplus.effects.list.soulharvest.SoulHarvestKillEffect;
import com.monkey.ktplus.effects.list.voidimplode.VoidImplodeKillEffect;
import com.monkey.ktplus.effects.list.tornado.TornadoKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class UltraEffectsRegistration {
    private UltraEffectsRegistration() {}

    public static void register(
            EffectRegistry registry,
            EffectDefinitionFactory definitions,
            VisualEffectService visuals,
            @Nullable HeadCollectorService headCollector) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        if (headCollector != null) {
            registry.register(new HeadCollectorKillEffect(
                    definitions.definition(
                            "headcollector",
                            "Head Collector",
                            "PLAYER_HEAD",
                            EffectCategory.ULTRA,
                            700000,
                            false,
                            HeadCollectorSettings.INTRO_DURATION_TICKS + 10L),
                    visuals,
                    headCollector));
        }
        registry.register(new EnchantColumnKillEffect(
                definitions.definition(
                        "enchantcolumn",
                        "Enchant Column",
                        "ENCHANTING_TABLE",
                        EffectCategory.ULTRA,
                        900000,
                        true,
                        280),
                visuals));
        registry.register(new GlowMissileKillEffect(
                definitions.definition(
                        "glowmissile",
                        "Glow Missile",
                        "GLOWSTONE_DUST",
                        EffectCategory.ULTRA,
                        1100000,
                        false,
                        GlowMissileLauncher.EFFECT_DURATION_TICKS),
                visuals));
        registry.register(new SniperKillEffect(
                definitions.definition("sniper", "Sniper", "BOW", EffectCategory.ULTRA, 1300000, false, 220),
                visuals));
        registry.register(new TornadoKillEffect(
                definitions.definition("tornado", "Tornado", "WHITE_WOOL", EffectCategory.ULTRA, 1500000, false, 260),
                visuals));
        registry.register(new VoidImplodeKillEffect(
                definitions.definition(
                        "voidimplode", "Void Implode", "ENDER_EYE", EffectCategory.ULTRA, 950000, true, 200),
                visuals));
        registry.register(new SoulHarvestKillEffect(
                definitions.definition(
                        "soulharvest", "Soul Harvest", "SOUL_LANTERN", EffectCategory.ULTRA, 1150000, true, 220),
                visuals));
        registry.register(new NightfallKillEffect(
                definitions.definition(
                        "nightfall", "Nightfall", "BLACK_CONCRETE", EffectCategory.ULTRA, 950000, true, 180),
                visuals));
    }
}
