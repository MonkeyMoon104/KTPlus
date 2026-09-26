package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.service.PermissionService;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class BridgePermissionService implements PermissionService {
    private final com.monkey.ktplus.permission.PermissionService permission;
    private final EffectRegistry registry;

    public BridgePermissionService(
            com.monkey.ktplus.permission.PermissionService permission, EffectRegistry registry) {
        this.permission = Objects.requireNonNull(permission, "permission");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public boolean isAdminBypass(Player player) {
        return permission.isAdminBypass(player);
    }

    @Override
    public boolean canUse(Player player, Effect effect) {
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        return definition != null && permission.canUseEffect(player, definition);
    }

    @Override
    public boolean canUse(Player player, String effectId) {
        Objects.requireNonNull(effectId, "effectId");
        return registry
                .find(effectId)
                .map(killEffect -> permission.canUseEffect(player, killEffect.definition()))
                .orElse(false);
    }

    @Override
    public String permissionNode(String effectId) {
        return permission.permissionNode(effectId);
    }
}
