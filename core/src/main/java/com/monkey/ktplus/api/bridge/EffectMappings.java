package com.monkey.ktplus.api.bridge;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.EffectCategory;
import com.monkey.ktplus.effects.api.EffectDefinition;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class EffectMappings {
    private EffectMappings() {}

    public static Effect toApi(EffectDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new Effect(
                definition.id(),
                definition.displayName(),
                toApi(definition.category()),
                definition.price(),
                definition.premiumPrice(),
                definition.heavy(),
                definition.maxDurationTicks(),
                definition.icon().name(),
                definition.iconKey());
    }

    public static EffectCategory toApi(com.monkey.ktplus.effects.api.EffectCategory category) {
        Objects.requireNonNull(category, "category");
        EffectCategory mapped = EffectCategory.fromConfigId(category.configId());
        return mapped == null ? EffectCategory.COMMON : mapped;
    }

    public static com.monkey.ktplus.effects.api.EffectCategory toInternal(EffectCategory category) {
        Objects.requireNonNull(category, "category");
        return com.monkey.ktplus.effects.api.EffectCategory.fromConfigId(
                category.configId(), com.monkey.ktplus.effects.api.EffectCategory.COMMON);
    }

    public static EffectDefinition toDefinition(Effect effect) {
        Objects.requireNonNull(effect, "effect");
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(effect.iconMaterial());
        if (material == null) {
            material = org.bukkit.Material.STONE;
        }
        return new EffectDefinition(
                effect.id(),
                effect.displayName(),
                material,
                effect.iconKey(),
                toInternal(effect.category()),
                effect.premiumPrice(),
                effect.price(),
                effect.heavy(),
                effect.maxDurationTicks());
    }

    public static @Nullable EffectDefinition requireDefinition(
            com.monkey.ktplus.effects.registry.EffectRegistry registry, Effect effect) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(effect, "effect");
        return registry
                .find(effect.id())
                .map(killEffect -> killEffect.definition())
                .orElse(null);
    }

    public static String normalizeId(String id) {
        return Objects.requireNonNull(id, "id").toLowerCase(Locale.ROOT);
    }
}
