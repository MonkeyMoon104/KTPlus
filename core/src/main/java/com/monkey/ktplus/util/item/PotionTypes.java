package com.monkey.ktplus.util.item;

import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

public final class PotionTypes {
    private PotionTypes() {}

    public static @Nullable PotionEffectType strength() {
        return resolve("STRENGTH", "INCREASE_DAMAGE");
    }

    public static @Nullable PotionEffectType levitation() {
        return resolve("LEVITATION", null);
    }

    public static @Nullable PotionEffectType resolve(String modern, @Nullable String legacy) {
        PotionEffectType type = registryGet(modern);
        if (type != null) {
            return type;
        }
        if (legacy != null) {
            return registryGet(legacy);
        }
        return null;
    }

    public static @Nullable PotionEffectType byKey(String typeKey) {
        if (typeKey == null || typeKey.isBlank()) {
            return null;
        }
        String normalized = typeKey.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        PotionEffectType direct = registryGet(normalized);
        if (direct != null) {
            return direct;
        }
        return registryGet(normalized.toLowerCase(Locale.ROOT).replace('_', '.'));
    }

    private static @Nullable PotionEffectType registryGet(String key) {
        if (key.contains(".")) {
            return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(key));
        }
        return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
    }
}
