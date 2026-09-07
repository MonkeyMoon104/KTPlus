package com.monkey.ktplus.effects.registry.builtin;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.list.bubbletrap.BubbleTrapKillEffect;
import com.monkey.ktplus.effects.list.echobat.EchoBatKillEffect;
import com.monkey.ktplus.effects.list.end.EndKillEffect;
import com.monkey.ktplus.effects.list.end.EndSettings;
import com.monkey.ktplus.effects.list.grave.GraveKillEffect;
import com.monkey.ktplus.effects.list.lightning.LightningKillEffect;
import com.monkey.ktplus.effects.list.lightning.LightningSettings;
import com.monkey.ktplus.effects.list.paintbomb.PaintBombKillEffect;
import com.monkey.ktplus.effects.list.sandstorm.SandstormKillEffect;
import com.monkey.ktplus.effects.list.totem.TotemKillEffect;
import com.monkey.ktplus.effects.list.vinegrasp.VineGraspKillEffect;
import com.monkey.ktplus.effects.registry.EffectDefinitionFactory;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;

public final class NonCommonEffectsRegistration {
    private NonCommonEffectsRegistration() {}

    public static void register(
            EffectRegistry registry, EffectDefinitionFactory definitions, VisualEffectService visuals) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(visuals, "visuals");

        registry.register(new EndKillEffect(
                definitions.definition(
                        "end",
                        "End",
                        "ENDER_PEARL",
                        EffectCategory.NON_COMMON,
                        250,
                        false,
                        EndSettings.EFFECT_DURATION_TICKS + 40L),
                visuals));
        registry.register(new LightningKillEffect(definitions.definition(
                "lightning",
                "Lightning",
                "BLAZE_ROD",
                EffectCategory.NON_COMMON,
                200,
                false,
                LightningSettings.resolveMaxDurationTicks(definitions.config().effectSection("lightning"))),
                visuals));
        registry.register(new TotemKillEffect(
                definitions.definition("totem", "Totem", "GOLD_INGOT", EffectCategory.NON_COMMON, 500, false, 260),
                visuals));
        registry.register(new GraveKillEffect(
                definitions.definition("grave", "Grave", "COBBLESTONE", EffectCategory.NON_COMMON, 500, false, 100),
                visuals));
        registry.register(new BubbleTrapKillEffect(
                definitions.definition(
                        "bubbletrap", "Bubble Trap", "HEART_OF_THE_SEA", EffectCategory.NON_COMMON, 1200, false, 180),
                visuals));
        registry.register(new SandstormKillEffect(
                definitions.definition("sandstorm", "Sandstorm", "SAND", EffectCategory.NON_COMMON, 1100, false, 160),
                visuals));
        registry.register(new VineGraspKillEffect(
                definitions.definition("vinegrasp", "Vine Grasp", "VINE", EffectCategory.NON_COMMON, 1300, false, 160),
                visuals));
        registry.register(new EchoBatKillEffect(
                definitions.definition("echobat", "Echo Bat", "ECHO_SHARD", EffectCategory.NON_COMMON, 1400, false, 170),
                visuals));
        registry.register(new PaintBombKillEffect(
                definitions.definition(
                        "paintbomb", "Paint Bomb", "PINK_CONCRETE_POWDER", EffectCategory.NON_COMMON, 1250, false, 120),
                visuals));
    }
}
