package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.alchemy.AlchemyCauldronKillEffect;
import com.monkey.ktplus.effects.list.aurafarming.AuraFarmingKillEffect;
import com.monkey.ktplus.effects.list.crystalspire.CrystalSpireKillEffect;
import com.monkey.ktplus.effects.list.dimensionalrift.DimensionalRiftKillEffect;
import com.monkey.ktplus.effects.list.firephoenix.FirePhoenixKillEffect;
import com.monkey.ktplus.effects.list.fireworks.FireworksKillEffect;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksSettings;
import com.monkey.ktplus.effects.list.forgeanvil.ForgeAnvilKillEffect;
import com.monkey.ktplus.effects.list.hauntedchoir.HauntedChoirKillEffect;
import com.monkey.ktplus.effects.list.prismbeam.PrismBeamKillEffect;
import com.monkey.ktplus.effects.list.prismaticnova.PrismaticNovaKillEffect;
import com.monkey.ktplus.effects.list.puppetstrings.PuppetStringsKillEffect;
import com.monkey.ktplus.effects.list.railgun.RailgunKillEffect;
import com.monkey.ktplus.effects.list.serpentcoil.SerpentCoilKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;

public final class EpicEffectsRegistration {
    private EpicEffectsRegistration() {}

    public static void register(
            EffectRegistry registry, EffectDefinitionFactory definitions, VisualEffectService visuals) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        registry.register(new FireworksKillEffect(
                definitions.definition(
                        "fireworks",
                        "Fireworks",
                        "FIREWORK_ROCKET",
                        EffectCategory.EPIC,
                        6000,
                        true,
                        FireworksSettings.from(definitions.config().effectSection("fireworks"))
                                .estimatedRadarDurationTicks()),
                visuals));
        registry.register(new AuraFarmingKillEffect(
                definitions.definition("aurafarming", "Aura Farming", "WHEAT", EffectCategory.EPIC, 7000, true, 420),
                visuals));
        registry.register(new DimensionalRiftKillEffect(
                definitions.definition(
                        "dimensionalrift", "Dimensional Rift", "ENDER_EYE", EffectCategory.EPIC, 9000, true, 220),
                visuals));
        registry.register(new FirePhoenixKillEffect(
                definitions.definition(
                        "firephoenix", "Fire Phoenix", "BLAZE_POWDER", EffectCategory.EPIC, 10000, true, 400),
                visuals));
        registry.register(new PrismaticNovaKillEffect(
                definitions.definition(
                        "prismaticnova", "Prismatic Nova", "PRISMARINE_SHARD", EffectCategory.EPIC, 12000, true, 100),
                visuals));
        registry.register(new RailgunKillEffect(
                definitions.definition("railgun", "Railgun", "LIGHTNING_ROD", EffectCategory.EPIC, 8500, true, 100),
                visuals));
        registry.register(new PuppetStringsKillEffect(
                definitions.definition("puppetstrings", "Puppet Strings", "STRING", EffectCategory.EPIC, 9000, true, 200),
                visuals));
        registry.register(new AlchemyCauldronKillEffect(
                definitions.definition("alchemy", "Alchemy", "CAULDRON", EffectCategory.EPIC, 7500, true, 200),
                visuals));
        registry.register(new CrystalSpireKillEffect(
                definitions.definition(
                        "crystalspire", "Crystal Spire", "AMETHYST_CLUSTER", EffectCategory.EPIC, 9500, true, 220),
                visuals));
        registry.register(new SerpentCoilKillEffect(
                definitions.definition(
                        "serpentcoil", "Serpent Coil", "GREEN_CONCRETE", EffectCategory.EPIC, 8800, true, 220),
                visuals));
        registry.register(new ForgeAnvilKillEffect(
                definitions.definition("forgeanvil", "Forge Anvil", "ANVIL", EffectCategory.EPIC, 8200, true, 160),
                visuals));
        registry.register(new PrismBeamKillEffect(
                definitions.definition("prismbeam", "Prism Beam", "AMETHYST_SHARD", EffectCategory.EPIC, 9000, true, 120),
                visuals));
        registry.register(new HauntedChoirKillEffect(
                definitions.definition(
                        "hauntedchoir", "Haunted Choir", "SKELETON_SKULL", EffectCategory.EPIC, 8600, true, 200),
                visuals));
    }
}
