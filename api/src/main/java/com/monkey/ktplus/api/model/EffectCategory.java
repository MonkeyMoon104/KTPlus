package com.monkey.ktplus.api.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Rarity / GUI grouping for kill effects.
 *
 * <p>Each constant maps to a stable {@link #configId()} used in {@code config.yml} (kebab-case).
 * Use {@link #fromConfigId(String)} when reading config; use {@link #ordered()} for GUI layout.
 *
 * @since 4.0.3
 */
public enum EffectCategory {
    /** Baseline / common rarity. */
    COMMON("common"),
    /** Uncommon / non-common tier. */
    NON_COMMON("non-common"),
    /** Rare tier. */
    RARE("rare"),
    /** Very-rare tier. */
    VERY_RARE("very-rare"),
    /** Epic tier. */
    EPIC("epic"),
    /** Legendary tier. */
    LEGENDARY("legendary"),
    /** Highest / ultra tier. */
    ULTRA("ultra");

    private static final List<EffectCategory> ORDER = Collections.unmodifiableList(Arrays.asList(values()));

    private final String configId;

    EffectCategory(String configId) {
        this.configId = configId;
    }

    /**
     * Stable kebab-case id used in configuration files.
     *
     * @return config id (e.g. {@code very-rare})
     */
    public String configId() {
        return configId;
    }

    /**
     * Categories in declaration / display order.
     *
     * @return unmodifiable list of all values
     */
    public static List<EffectCategory> ordered() {
        return ORDER;
    }

    /**
     * Parses a config id (case-insensitive; underscores accepted as hyphens).
     *
     * @param raw config token, or {@code null}/blank
     * @return matching category, or {@code null} if unknown / blank
     */
    public static @Nullable EffectCategory fromConfigId(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (EffectCategory category : values()) {
            if (category.configId.equals(normalized)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Parses a required config id.
     *
     * @param raw config token
     * @return matching category
     * @throws NullPointerException if {@code raw} does not match any category
     */
    public static EffectCategory requireConfigId(String raw) {
        return Objects.requireNonNull(fromConfigId(raw), "category");
    }
}
