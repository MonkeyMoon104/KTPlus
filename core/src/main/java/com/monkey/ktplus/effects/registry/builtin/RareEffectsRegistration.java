package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.boomerang.BoomerangKillEffect;
import com.monkey.ktplus.effects.list.cactusbarrage.CactusBarrageKillEffect;
import com.monkey.ktplus.effects.list.chainsaw.ChainsawKillEffect;
import com.monkey.ktplus.effects.list.explosion.ExplosionKillEffect;
import com.monkey.ktplus.effects.list.geyser.GeyserKillEffect;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorKillEffect;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorService;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorSettings;
import com.monkey.ktplus.effects.list.hookshot.HookshotKillEffect;
import com.monkey.ktplus.effects.list.icerink.IceRinkKillEffect;
import com.monkey.ktplus.effects.list.lightningrod.LightningRodKillEffect;
import com.monkey.ktplus.effects.list.mace.MaceKillEffect;
import com.monkey.ktplus.effects.list.meteorshower.MeteorShowerKillEffect;
import com.monkey.ktplus.effects.list.mirrorclone.MirrorCloneKillEffect;
import com.monkey.ktplus.effects.list.skeleton.SkeletonKillEffect;
import com.monkey.ktplus.effects.list.tornado.TornadoKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class RareEffectsRegistration {
    private RareEffectsRegistration() {}

    public static void register(
            EffectRegistry registry,
            EffectDefinitionFactory definitions,
            VisualEffectService visuals,
            @Nullable HeadCollectorService headCollector) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        registry.register(new TornadoKillEffect(
                definitions.definition("tornado", "Tornado", "WHITE_WOOL", EffectCategory.RARE, 3500, false, 260),
                visuals));
        registry.register(new ExplosionKillEffect(
                definitions.definition("explosion", "Explosion", "TNT", EffectCategory.RARE, 500, false, 70),
                visuals));
        if (headCollector != null) {
            registry.register(new HeadCollectorKillEffect(
                    definitions.definition(
                            "headcollector",
                            "Head Collector",
                            "PLAYER_HEAD",
                            EffectCategory.RARE,
                            2500,
                            false,
                            HeadCollectorSettings.INTRO_DURATION_TICKS + 10L),
                    visuals,
                    headCollector));
        }
        registry.register(new MaceKillEffect(
                definitions.definition("mace", "Mace", "IRON_AXE", EffectCategory.RARE, 1000, false, 90), visuals));
        registry.register(new SkeletonKillEffect(
                definitions.definition("skeleton", "Skeleton", "SKELETON_SKULL", EffectCategory.RARE, 2000, false, 120),
                visuals));
        registry.register(new LightningRodKillEffect(
                definitions.definition(
                        "lightningrod", "Lightning Rod", "BLAZE_ROD", EffectCategory.RARE, 2200, false, 200),
                visuals));
        registry.register(new BoomerangKillEffect(
                definitions.definition("boomerang", "Boomerang", "WOODEN_SWORD", EffectCategory.RARE, 1800, false, 140),
                visuals));
        registry.register(new MirrorCloneKillEffect(
                definitions.definition(
                        "mirrorclone", "Mirror Clone", "ARMOR_STAND", EffectCategory.RARE, 2500, false, 160),
                visuals));
        registry.register(new GeyserKillEffect(
                definitions.definition("geyser", "Geyser", "WATER_BUCKET", EffectCategory.RARE, 2000, false, 150),
                visuals));
        registry.register(new CactusBarrageKillEffect(
                definitions.definition("cactusbarrage", "Cactus Barrage", "CACTUS", EffectCategory.RARE, 1700, false, 130),
                visuals));
        registry.register(new ChainsawKillEffect(
                definitions.definition("chainsaw", "Chainsaw", "IRON_SWORD", EffectCategory.RARE, 2400, false, 140),
                visuals));
        registry.register(new MeteorShowerKillEffect(
                definitions.definition(
                        "meteorshower", "Meteor Shower", "MAGMA_BLOCK", EffectCategory.RARE, 2600, false, 160),
                visuals));
        registry.register(new IceRinkKillEffect(
                definitions.definition("icerink", "Ice Rink", "PACKED_ICE", EffectCategory.RARE, 2300, false, 150),
                visuals));
        registry.register(new HookshotKillEffect(
                definitions.definition("hookshot", "Hookshot", "FISHING_ROD", EffectCategory.RARE, 2100, false, 100),
                visuals));
    }
}
