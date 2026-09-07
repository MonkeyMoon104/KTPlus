package com.monkey.ktplus.effects.registry;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorService;
import com.monkey.ktplus.effects.registry.builtin.CommonEffectsRegistration;
import com.monkey.ktplus.effects.registry.builtin.EpicEffectsRegistration;
import com.monkey.ktplus.effects.registry.builtin.LegendaryEffectsRegistration;
import com.monkey.ktplus.effects.registry.builtin.NonCommonEffectsRegistration;
import com.monkey.ktplus.effects.registry.builtin.RareEffectsRegistration;
import com.monkey.ktplus.effects.registry.builtin.UltraEffectsRegistration;
import com.monkey.ktplus.effects.registry.builtin.VeryRareEffectsRegistration;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.schematic.SchematicLibrary;
import java.util.Objects;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

public final class BuiltInEffectRegistrar {
    private final ConfigSnapshot config;
    private final VisualEffectService visuals;
    private final SchematicLibrary schematics;
    private final Logger logger;
    private final @Nullable HeadCollectorService headCollector;

    public BuiltInEffectRegistrar(
            ConfigSnapshot config,
            VisualEffectService visuals,
            SchematicLibrary schematics,
            Logger logger) {
        this(config, visuals, schematics, logger, null);
    }

    public BuiltInEffectRegistrar(
            ConfigSnapshot config,
            VisualEffectService visuals,
            SchematicLibrary schematics,
            Logger logger,
            @Nullable HeadCollectorService headCollector) {
        this.config = Objects.requireNonNull(config, "config");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.schematics = Objects.requireNonNull(schematics, "schematics");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.headCollector = headCollector;
    }

    public void registerAll(EffectRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        EffectDefinitionFactory definitions = new EffectDefinitionFactory(config, visuals);
        CommonEffectsRegistration.register(registry, definitions, visuals);
        NonCommonEffectsRegistration.register(registry, definitions, visuals);
        RareEffectsRegistration.register(registry, definitions, visuals, headCollector);
        VeryRareEffectsRegistration.register(registry, definitions, visuals);
        EpicEffectsRegistration.register(registry, definitions, visuals);
        LegendaryEffectsRegistration.register(registry, definitions, visuals);
        UltraEffectsRegistration.register(registry, definitions, visuals);
    }
}
