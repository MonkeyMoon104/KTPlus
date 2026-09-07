package com.monkey.ktplus.effects.support.entity;

import java.util.Locale;
import org.bukkit.entity.EntityType;
import org.jspecify.annotations.Nullable;

public final class SupportedEntities {
    private SupportedEntities() {}

    public static boolean isPresent(@Nullable String typeName) {
        return resolve(typeName) != null;
    }

    public static @Nullable EntityType resolve(@Nullable String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return null;
        }
        try {
            return EntityType.valueOf(typeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
