package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.EffectCategory;
import com.monkey.ktplus.api.service.EffectService;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class BridgeEffectService implements EffectService {
    private final EffectRegistry registry;

    public BridgeEffectService(EffectRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public Optional<Effect> find(String idOrAlias) {
        Objects.requireNonNull(idOrAlias, "idOrAlias");
        return registry.find(idOrAlias).map(effect -> EffectMappings.toApi(effect.definition()));
    }

    @Override
    public Collection<Effect> all() {
        return registry.all().stream()
                .map(effect -> EffectMappings.toApi(effect.definition()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Collection<Effect> byCategory(EffectCategory category) {
        Objects.requireNonNull(category, "category");
        com.monkey.ktplus.effects.api.EffectCategory internal = EffectMappings.toInternal(category);
        return registry.all().stream()
                .filter(effect -> effect.definition().category() == internal)
                .map(effect -> EffectMappings.toApi(effect.definition()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Collection<String> ids() {
        return registry.all().stream()
                .map(effect -> effect.definition().id().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public boolean exists(String idOrAlias) {
        Objects.requireNonNull(idOrAlias, "idOrAlias");
        return registry.find(idOrAlias).isPresent();
    }
}
