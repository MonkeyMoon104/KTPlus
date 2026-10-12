package com.monkey.ktplus.permission;

import org.bukkit.permissions.PermissionDefault;

final class EffectPermissionDefaults {
    private EffectPermissionDefaults() {}

    /**
     * Shop access is gated by economy ownership, not by permission default.
     * Keep {@code ktplus.<id>.use} granted so players can open/buy effects; servers that want
     * rank-locks can explicitly deny the node in LuckPerms.
     */
    static PermissionDefault forPrice(int price) {
        return PermissionDefault.TRUE;
    }
}
