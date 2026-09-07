package com.monkey.ktplus.access.effect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

final class TestPlayers {
    private TestPlayers() {}

    static Player withPermissions(String... permissions) {
        Set<String> granted = new HashSet<String>();
        for (String permission : permissions) {
            granted.add(Objects.requireNonNull(permission, "permission"));
        }
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("hasPermission".equals(name) && args != null && args.length > 0 && args[0] instanceof String) {
                    return granted.contains(args[0]);
                }
                if ("isPermissionSet".equals(name) && args != null && args.length > 0 && args[0] instanceof String) {
                    return granted.contains(args[0]);
                }
                if ("getUniqueId".equals(name)) {
                    return UUID.randomUUID();
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) {
                    return false;
                }
                if (returnType == int.class) {
                    return 0;
                }
                if (returnType == long.class) {
                    return 0L;
                }
                if (returnType == float.class) {
                    return 0F;
                }
                if (returnType == double.class) {
                    return 0D;
                }
                if (returnType == byte.class) {
                    return (byte) 0;
                }
                if (returnType == short.class) {
                    return (short) 0;
                }
                if (returnType == char.class) {
                    return (char) 0;
                }
                return null;
            }
        };
        return (Player) Proxy.newProxyInstance(
                TestPlayers.class.getClassLoader(), new Class<?>[] {Player.class, Permissible.class}, handler);
    }
}
