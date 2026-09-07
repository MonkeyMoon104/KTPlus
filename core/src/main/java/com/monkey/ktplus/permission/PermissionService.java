package com.monkey.ktplus.permission;

import com.monkey.ktplus.effects.api.EffectDefinition;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class PermissionService {
    public boolean isAdminBypass(Player player) {
        Objects.requireNonNull(player, "player");
        return player.hasPermission("ktplus.admin.bypass") || player.hasPermission("ktplus.admin");
    }

    public boolean canUseEffect(Player player, EffectDefinition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        return isAdminBypass(player) || player.hasPermission(permissionNode(definition.id()));
    }

    public boolean canUseCommand(Player player, String node) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return player.hasPermission("ktplus.admin") || player.hasPermission(node);
    }

    public String permissionNode(String effectId) {
        Objects.requireNonNull(effectId, "effectId");
        return "ktplus." + effectId.toLowerCase(Locale.ROOT) + ".use";
    }
}
