package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.abyssgate.AbyssGateKillEffect;
import com.monkey.ktplus.effects.list.arcade.ArcadeCabinetKillEffect;
import com.monkey.ktplus.effects.list.crystalfracture.CrystalFractureKillEffect;
import com.monkey.ktplus.effects.list.nebularing.NebulaRingKillEffect;
import com.monkey.ktplus.effects.list.riftmaw.RiftMawKillEffect;
import com.monkey.ktplus.effects.list.stormcrown.StormCrownKillEffect;
import com.monkey.ktplus.effects.list.chronosphere.ChronosphereKillEffect;
import com.monkey.ktplus.effects.list.cosmicfinale.CosmicFinaleKillEffect;
import com.monkey.ktplus.effects.list.crownofash.CrownOfAshKillEffect;
import com.monkey.ktplus.effects.list.dragonnest.DragonNestKillEffect;
import com.monkey.ktplus.effects.list.gravitywell.GravityWellKillEffect;
import com.monkey.ktplus.effects.list.judgment.JudgmentKillEffect;
import com.monkey.ktplus.effects.list.kaiju.KaijuFootprintKillEffect;
import com.monkey.ktplus.effects.list.origami.OrigamiKillEffect;
import com.monkey.ktplus.effects.list.realityglitch.RealityGlitchKillEffect;
import com.monkey.ktplus.effects.list.stellarcollapse.StellarCollapseKillEffect;
import com.monkey.ktplus.effects.list.voidlotus.VoidLotusKillEffect;
import com.monkey.ktplus.effects.list.warden.WardenKillEffect;
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
        registry.register(new DragonNestKillEffect(
                definitions.definition(
                        "dragonnest", "Dragon Nest", "DRAGON_EGG", EffectCategory.LEGENDARY, 14000, true, 160),
                visuals));
        registry.register(new GravityWellKillEffect(
                definitions.definition(
                        "gravitywell", "Gravity Well", "CRYING_OBSIDIAN", EffectCategory.LEGENDARY, 16000, true, 200),
                visuals));
        registry.register(new ArcadeCabinetKillEffect(
                definitions.definition("arcade", "Arcade", "JUKEBOX", EffectCategory.LEGENDARY, 13000, true, 260),
                visuals));
        registry.register(new KaijuFootprintKillEffect(
                definitions.definition("kaiju", "Kaiju", "COARSE_DIRT", EffectCategory.LEGENDARY, 18000, true, 240),
                visuals));
        registry.register(new OrigamiKillEffect(
                definitions.definition("origami", "Origami", "PAPER", EffectCategory.LEGENDARY, 14000, true, 240),
                visuals));
        registry.register(new AbyssGateKillEffect(
                definitions.definition("abyssgate", "Abyss Gate", "OBSIDIAN", EffectCategory.LEGENDARY, 16000, true, 180),
                visuals));
        registry.register(new CrownOfAshKillEffect(
                definitions.definition(
                        "crownofash", "Crown of Ash", "FIRE_CHARGE", EffectCategory.LEGENDARY, 251011, true, 240),
                visuals));
        registry.register(new ChronosphereKillEffect(
                definitions.definition(
                        "chronosphere", "Chronosphere", "RECOVERY_COMPASS", EffectCategory.LEGENDARY, 384194, true, 300),
                visuals));
        registry.register(new RealityGlitchKillEffect(
                definitions.definition(
                        "realityglitch",
                        "Reality Glitch",
                        "STRUCTURE_BLOCK",
                        EffectCategory.LEGENDARY,
                        416440,
                        true,
                        70),
                visuals));
        registry.register(new JudgmentKillEffect(
                definitions.definition(
                        "judgment", "Judgment", "GOLD_BLOCK", EffectCategory.LEGENDARY, 448365, true, 300),
                visuals));
        registry.register(new CosmicFinaleKillEffect(
                definitions.definition(
                        "cosmicfinale", "Cosmic Finale", "BEACON", EffectCategory.LEGENDARY, 480000, true, 280),
                visuals));
        registry.register(new WitherKillEffect(
                definitions.definition(
                        "wither", "Wither", "WITHER_SKELETON_SKULL", EffectCategory.LEGENDARY, 440000, true, 300),
                visuals));
        registry.register(new WardenKillEffect(
                definitions.definition(
                        "warden", "Warden", "SCULK_SHRIEKER", EffectCategory.LEGENDARY, 490000, true, 120),
                visuals));
        registry.register(new CrystalFractureKillEffect(
                definitions.definition(
                        "crystalfracture",
                        "Crystal Fracture",
                        "PRISMARINE_CRYSTALS",
                        EffectCategory.LEGENDARY,
                        320000,
                        true,
                        210),
                visuals));
        registry.register(new RiftMawKillEffect(
                definitions.definition(
                        "riftmaw", "Rift Maw", "END_PORTAL_FRAME", EffectCategory.LEGENDARY, 340000, true, 145),
                visuals));
        registry.register(new StormCrownKillEffect(
                definitions.definition(
                        "stormcrown", "Storm Crown", "LIGHTNING_ROD", EffectCategory.LEGENDARY, 370000, true, 220),
                visuals));
        registry.register(new NebulaRingKillEffect(
                definitions.definition(
                        "nebularing", "Nebula Ring", "AMETHYST_SHARD", EffectCategory.LEGENDARY, 395000, true, 160),
                visuals));
    }
}
