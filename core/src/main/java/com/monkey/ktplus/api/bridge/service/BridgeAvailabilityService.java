package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.AvailabilityService;
import com.monkey.ktplus.effects.availability.EffectAvailabilityService;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public final class BridgeAvailabilityService implements AvailabilityService {
    private final EffectAvailabilityService availability;
    private final EffectRegistry registry;

    public BridgeAvailabilityService(EffectAvailabilityService availability, EffectRegistry registry) {
        this.availability = Objects.requireNonNull(availability, "availability");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public boolean isEnabled(String effectId) {
        return availability.isEnabled(effectId);
    }

    @Override
    public boolean isDisabled(String effectId) {
        return availability.isDisabled(effectId);
    }

    @Override
    public Set<String> disabledIds() {
        return availability.disabledIds();
    }

    @Override
    public boolean disable(String effectId) {
        Objects.requireNonNull(effectId, "effectId");
        boolean known = registry.find(effectId).isPresent();
        return availability.disable(effectId, known)
                == EffectAvailabilityService.DisableResult.DISABLED;
    }

    @Override
    public boolean enable(String effectId) {
        Objects.requireNonNull(effectId, "effectId");
        boolean known = registry.find(effectId).isPresent();
        return availability.enable(effectId, known) == EffectAvailabilityService.EnableResult.ENABLED;
    }

    @Override
    public Collection<String> filterEnabled(Collection<String> ids) {
        return availability.filterEnabled(ids);
    }

    @Override
    public Collection<String> filterDisabled(Collection<String> knownIds) {
        return availability.filterDisabled(knownIds);
    }
}
