package com.monkey.ktplus.effects.support.entity;

import java.lang.reflect.Method;
import org.bukkit.entity.Firework;

public final class FireworkDetonator {
    private static final Method DETONATE = resolveDetonate();

    private FireworkDetonator() {
    }

    public static void detonate(Firework firework) {
        if (firework == null || !firework.isValid() || DETONATE == null) {
            return;
        }
        try {
            DETONATE.invoke(firework);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method resolveDetonate() {
        try {
            Method method = Firework.class.getMethod("detonate");
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
