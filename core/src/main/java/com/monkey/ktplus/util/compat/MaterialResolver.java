package com.monkey.ktplus.util.compat;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class MaterialResolver {
    private MaterialResolver() {}

    public static Material resolve(@Nullable String value, String fallbackKey) {
        Objects.requireNonNull(fallbackKey, "fallbackKey");
        Material matched = match(value);
        if (matched != null) {
            return matched;
        }
        matched = match(fallbackKey);
        if (matched != null) {
            return matched;
        }
        matched = match("STONE");
        if (matched != null) {
            return matched;
        }
        throw new IllegalStateException("Unable to resolve material for '" + value + "' / '" + fallbackKey + "'");
    }

    public static @Nullable Material find(@Nullable String value) {
        return match(value);
    }

    public static ItemStack stainedGlassPane(String modernName, String legacyFallback) {
        Objects.requireNonNull(modernName, "modernName");
        Objects.requireNonNull(legacyFallback, "legacyFallback");
        return new ItemStack(resolve(modernName, legacyFallback), 1);
    }

    private static @Nullable Material match(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(normalized);
        if (material != null) {
            return material;
        }
        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
