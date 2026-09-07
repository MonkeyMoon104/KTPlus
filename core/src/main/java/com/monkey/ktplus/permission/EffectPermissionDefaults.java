package com.monkey.ktplus.permission;

import org.bukkit.permissions.PermissionDefault;

final class EffectPermissionDefaults {
    private EffectPermissionDefaults() {}

    static PermissionDefault forPrice(int price) {
        return price <= 0 ? PermissionDefault.TRUE : PermissionDefault.FALSE;
    }
}
