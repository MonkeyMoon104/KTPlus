package com.monkey.ktplus.effects.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public enum EffectCategory {
    COMMON("common"),
    NON_COMMON("non-common"),
    RARE("rare"),
    VERY_RARE("very-rare"),
    EPIC("epic"),
    LEGENDARY("legendary"),
    ULTRA("ultra");

    private static final List<EffectCategory> ORDER = Collections.unmodifiableList(Arrays.asList(values()));

    private final String configId;

    EffectCategory(String configId) {
        this.configId = configId;
    }

    public String configId() {
        return configId;
    }

    public static List<EffectCategory> ordered() {
        return ORDER;
    }

    public static EffectCategory fromConfigId(@Nullable String raw, EffectCategory fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (EffectCategory category : values()) {
            if (category.configId.equals(normalized)) {
                return category;
            }
        }
        return fallback;
    }

    public static EffectCategory requireConfigId(String raw) {
        return Objects.requireNonNull(fromConfigId(raw, null), "category");
    }
}
