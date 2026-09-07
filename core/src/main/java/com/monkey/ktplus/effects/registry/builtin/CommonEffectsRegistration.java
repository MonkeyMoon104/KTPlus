package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.beeswarm.BeeSwarmKillEffect;
import com.monkey.ktplus.effects.list.cloud.CloudKillEffect;
import com.monkey.ktplus.effects.list.earthquake.EarthquakeKillEffect;
import com.monkey.ktplus.effects.list.hearts.HeartsKillEffect;
import com.monkey.ktplus.effects.list.inksquid.InkSquidKillEffect;
import com.monkey.ktplus.effects.list.lanternrise.LanternRiseKillEffect;
import com.monkey.ktplus.effects.list.magnet.MagnetKillEffect;
import com.monkey.ktplus.effects.list.notes.NotesKillEffect;
import com.monkey.ktplus.effects.list.smoke.SmokeKillEffect;
import com.monkey.ktplus.effects.list.snowballstorm.SnowballStormKillEffect;
import com.monkey.ktplus.effects.list.sparkler.SparklerKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;

public final class CommonEffectsRegistration {
    private CommonEffectsRegistration() {}

    public static void register(
            EffectRegistry registry, EffectDefinitionFactory definitions, VisualEffectService visuals) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        registry.register(new CloudKillEffect(
                definitions.definition("cloud", "Cloud", "WHITE_WOOL", EffectCategory.COMMON, 500, false, 200),
                visuals));
        registry.register(new HeartsKillEffect(
                definitions.definition("hearts", "Hearts", "POPPY", EffectCategory.COMMON, 500, false, 130),
                visuals));
        registry.register(new NotesKillEffect(
                definitions.definition("notes", "Notes", "NOTE_BLOCK", EffectCategory.COMMON, 1000, false, 200),
                visuals));
        registry.register(new EarthquakeKillEffect(
                definitions.definition("earthquake", "Earthquake", "DIRT", EffectCategory.COMMON, 800, false, 140),
                visuals));
        registry.register(new SmokeKillEffect(
                definitions.definition("smoke", "Smoke", "COAL", EffectCategory.COMMON, 750, false, 220),
                visuals));
        registry.register(new MagnetKillEffect(
                definitions.definition("magnet", "Magnet", "IRON_BLOCK", EffectCategory.COMMON, 600, false, 160),
                visuals));
        registry.register(new InkSquidKillEffect(
                definitions.definition("inksquid", "Ink Squid", "INK_SAC", EffectCategory.COMMON, 700, false, 160),
                visuals));
        registry.register(new BeeSwarmKillEffect(
                definitions.definition("beeswarm", "Bee Swarm", "HONEYCOMB", EffectCategory.COMMON, 800, false, 200),
                visuals));
        registry.register(new SparklerKillEffect(
                definitions.definition("sparkler", "Sparkler", "BLAZE_POWDER", EffectCategory.COMMON, 550, false, 140),
                visuals));
        registry.register(new SnowballStormKillEffect(
                definitions.definition(
                        "snowballstorm", "Snowball Storm", "SNOWBALL", EffectCategory.COMMON, 650, false, 130),
                visuals));
        registry.register(new LanternRiseKillEffect(
                definitions.definition("lanternrise", "Lantern Rise", "LANTERN", EffectCategory.COMMON, 700, false, 150),
                visuals));
    }
}
