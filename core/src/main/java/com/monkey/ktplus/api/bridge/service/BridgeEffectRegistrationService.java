package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.bridge.ApiRegisteredKillEffect;
import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.spi.EffectExecutor;
import com.monkey.ktplus.api.spi.EffectRegistrationService;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BridgeEffectRegistrationService implements EffectRegistrationService {
    private final EffectRegistry registry;
    private final Set<String> externalIds = ConcurrentHashMap.newKeySet();

    public BridgeEffectRegistrationService(EffectRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void register(Effect effect, EffectExecutor executor, String... aliases) {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(executor, "executor");
        String id = EffectMappings.normalizeId(effect.id());
        if (registry.find(id).isPresent()) {
            throw new IllegalArgumentException("Effect already registered: " + id);
        }
        registry.register(new ApiRegisteredKillEffect(effect, executor), aliases);
        externalIds.add(id);
    }

    @Override
    public boolean unregister(String effectId) {
        Objects.requireNonNull(effectId, "effectId");
        String id = EffectMappings.normalizeId(effectId);
        if (!externalIds.contains(id)) {
            return false;
        }
        if (!registry.unregister(id)) {
            return false;
        }
        externalIds.remove(id);
        return true;
    }

    @Override
    public boolean isRegistered(String effectId) {
        return registry.find(effectId).isPresent();
    }

    @Override
    public boolean isExternal(String effectId) {
        return externalIds.contains(EffectMappings.normalizeId(effectId));
    }

    @Override
    public Collection<String> externalIds() {
        return new ArrayList<>(externalIds);
    }

    @Override
    public Optional<Effect> findExternal(String effectId) {
        Objects.requireNonNull(effectId, "effectId");
        String id = EffectMappings.normalizeId(effectId);
        if (!externalIds.contains(id)) {
            return Optional.empty();
        }
        return registry
                .find(id)
                .map(killEffect -> {
                    if (killEffect instanceof ApiRegisteredKillEffect api) {
                        return api.apiEffect();
                    }
                    return EffectMappings.toApi(killEffect.definition());
                });
    }
}
