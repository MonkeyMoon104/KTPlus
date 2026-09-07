package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.abyssgate.AbyssGateKillEffect;
import com.monkey.ktplus.effects.list.arcade.ArcadeCabinetKillEffect;
import com.monkey.ktplus.effects.list.crownofash.CrownOfAshKillEffect;
import com.monkey.ktplus.effects.list.dragonnest.DragonNestKillEffect;
import com.monkey.ktplus.effects.list.gravitywell.GravityWellKillEffect;
import com.monkey.ktplus.effects.list.kaiju.KaijuFootprintKillEffect;
import com.monkey.ktplus.effects.list.origami.OrigamiKillEffect;
import com.monkey.ktplus.effects.list.stellarcollapse.StellarCollapseKillEffect;
import com.monkey.ktplus.effects.list.voidlotus.VoidLotusKillEffect;
import com.monkey.ktplus.effects.list.wither.WitherKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;

public final class LegendaryEffectsRegistration {
    private LegendaryEffectsRegistration() {}

    public static void register(
            EffectRegistry registry, EffectDefinitionFactory definitions, VisualEffectService visuals) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        registry.register(new StellarCollapseKillEffect(
                definitions.definition(
                        "stellarcollapse", "Stellar Collapse", "NETHER_STAR", EffectCategory.LEGENDARY, 15000, true, 120),
                visuals));
        registry.register(new VoidLotusKillEffect(
                definitions.definition(
                        "voidlotus", "Void Lotus", "END_PORTAL_FRAME", EffectCategory.LEGENDARY, 10000, true, 120),
                visuals));
        registry.register(new WitherKillEffect(
                definitions.definition(
                        "wither", "Wither", "WITHER_SKELETON_SKULL", EffectCategory.LEGENDARY, 25000, true, 300),
                visuals));
        registry.register(new DragonNestKillEffect(
                definitions.definition(
                        "dragonnest", "Dragon Nest", "DRAGON_EGG", EffectCategory.LEGENDARY, 14000, true, 160),
                visuals));
        registry.register(new GravityWellKillEffect(
                definitions.definition(
                        "gravitywell", "Gravity Well", "ENDER_PEARL", EffectCategory.LEGENDARY, 16000, true, 200),
                visuals));
        registry.register(new ArcadeCabinetKillEffect(
                definitions.definition("arcade", "Arcade", "JUKEBOX", EffectCategory.LEGENDARY, 13000, true, 260),
                visuals));
        registry.register(new KaijuFootprintKillEffect(
                definitions.definition("kaiju", "Kaiju", "DIRT", EffectCategory.LEGENDARY, 18000, true, 240),
                visuals));
        registry.register(new OrigamiKillEffect(
                definitions.definition("origami", "Origami", "PAPER", EffectCategory.LEGENDARY, 14000, true, 240),
                visuals));
        registry.register(new AbyssGateKillEffect(
                definitions.definition("abyssgate", "Abyss Gate", "OBSIDIAN", EffectCategory.LEGENDARY, 16000, true, 180),
                visuals));
        registry.register(new CrownOfAshKillEffect(
                definitions.definition(
                        "crownofash", "Crown of Ash", "BLAZE_POWDER", EffectCategory.LEGENDARY, 15500, true, 240),
                visuals));
    }
}
