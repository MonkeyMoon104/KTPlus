package com.monkey.ktplus.effects.registry;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.CategoryDefinition;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.EffectPriceCalculator;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.Objects;

public final class EffectDefinitionFactory {
    private final ConfigSnapshot config;
    private final VisualEffectService visuals;

    public EffectDefinitionFactory(ConfigSnapshot config, VisualEffectService visuals) {
        this.config = Objects.requireNonNull(config, "config");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    public ConfigSnapshot config() {
        return config;
    }

    public EffectDefinition definition(
            String id,
            String fallbackName,
            String fallbackIcon,
            EffectCategory fallbackCategory,
            int fallbackPremium,
            boolean heavy,
            long duration) {
        String iconKey = config.effectIconKey(id);
        if (iconKey == null || iconKey.trim().isEmpty()) {
            iconKey = fallbackIcon;
        }
        EffectCategory category = config.effectCategory(id, fallbackCategory);
        CategoryDefinition categoryDefinition = config.categoryDefinition(category);
        int premium = config.effectPremiumPrice(id, fallbackPremium);
        int totalPrice = EffectPriceCalculator.totalPrice(categoryDefinition, premium);
        return new EffectDefinition(
                id,
                config.effectName(id, fallbackName),
                MaterialResolver.resolve(iconKey, visuals.material(fallbackIcon)),
                iconKey,
                category,
                premium,
                totalPrice,
                heavy,
                duration);
    }
}
