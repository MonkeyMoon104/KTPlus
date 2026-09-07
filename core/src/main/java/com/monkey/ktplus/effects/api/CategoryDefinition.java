package com.monkey.ktplus.effects.api;

import java.util.Objects;

public final class CategoryDefinition {
    private final EffectCategory category;
    private final String displayName;
    private final String tabIconKey;
    private final int minPrice;

    public CategoryDefinition(
            EffectCategory category, String displayName, String tabIconKey, int minPrice) {
        this.category = Objects.requireNonNull(category, "category");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.tabIconKey = Objects.requireNonNull(tabIconKey, "tabIconKey");
        this.minPrice = Math.max(0, minPrice);
    }

    public EffectCategory category() {
        return category;
    }

    public String displayName() {
        return displayName;
    }

    public String tabIconKey() {
        return tabIconKey;
    }

    public int minPrice() {
        return minPrice;
    }
}
