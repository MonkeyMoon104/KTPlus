package com.monkey.ktplus.effects.api;

public final class EffectPriceCalculator {
    private EffectPriceCalculator() {}

    public static int totalPrice(CategoryDefinition category, int premiumPrice) {
        return category.minPrice() + Math.max(0, premiumPrice);
    }
}
