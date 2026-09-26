package com.monkey.ktplus.api.builder;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.EffectCategory;
import java.util.Objects;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

/**
 * Staged fluent builders for {@link Effect}.
 *
 * <p>Prefer {@link Effect#builder(String)} at call sites. Stages enforce required fields
 * ({@code displayName}, icon) before {@link OptionStage#build()}.
 *
 * <p>Defaults when optional setters are omitted: category {@link EffectCategory#COMMON}, prices
 * {@code 0}, {@code heavy == false}, {@code maxDurationTicks == 100}. If {@link
 * OptionStage#premiumPrice(int)} is never called, premium price defaults to the standard price.
 *
 * @since 4.0.3
 */
public final class EffectBuilders {
    private EffectBuilders() {}

    /**
     * Begins a new builder for the given effect id.
     *
     * @param id effect id (non-null; validated further on build)
     * @return display-name stage
     * @throws NullPointerException if {@code id} is null
     */
    public static DisplayNameStage start(String id) {
        return new Stages(Objects.requireNonNull(id, "id"));
    }

    /**
     * Begins a builder pre-populated from an existing effect.
     *
     * @param effect source effect
     * @return option stage (required fields already satisfied)
     * @throws NullPointerException if {@code effect} is null
     */
    public static OptionStage from(Effect effect) {
        Objects.requireNonNull(effect, "effect");
        Stages stages = new Stages(effect.id());
        stages.displayName = effect.displayName();
        stages.category = effect.category();
        stages.price = effect.price();
        stages.premiumPrice = effect.premiumPrice();
        stages.premiumExplicit = true;
        stages.heavy = effect.heavy();
        stages.maxDurationTicks = effect.maxDurationTicks();
        stages.iconMaterial = effect.iconMaterial();
        stages.iconKey = effect.iconKey();
        stages.iconSet = true;
        stages.displayNameSet = true;
        return stages;
    }

    /**
     * First builder stage: set the display name.
     *
     * @since 4.0.3
     */
    public interface DisplayNameStage {
        /**
         * Sets the human-readable display name.
         *
         * @param displayName non-null display name
         * @return icon stage
         * @throws NullPointerException if {@code displayName} is null
         */
        IconStage displayName(String displayName);
    }

    /**
     * Second builder stage: set the GUI icon.
     *
     * @since 4.0.3
     */
    public interface IconStage {
        /**
         * Sets the icon from a Bukkit material.
         *
         * @param material non-null material
         * @return option stage
         * @throws NullPointerException if {@code material} is null
         */
        OptionStage icon(Material material);

        /**
         * Sets the icon from a material name string.
         *
         * @param materialName non-null material name
         * @return option stage
         * @throws NullPointerException if {@code materialName} is null
         */
        OptionStage iconMaterial(String materialName);
    }

    /**
     * Final builder stage: optional fields and {@link #build()}.
     *
     * @since 4.0.3
     */
    public interface OptionStage {
        /**
         * Sets the effect category.
         *
         * @param category non-null category
         * @return this stage
         * @throws NullPointerException if {@code category} is null
         */
        OptionStage category(EffectCategory category);

        /**
         * Sets the standard unlock price (negatives are clamped on build).
         *
         * @param price price in economy units
         * @return this stage
         */
        OptionStage price(int price);

        /**
         * Sets the premium unlock price explicitly.
         *
         * @param premiumPrice premium price in economy units
         * @return this stage
         */
        OptionStage premiumPrice(int premiumPrice);

        /**
         * Marks the effect as free ({@code price} and {@code premiumPrice} both {@code 0}).
         *
         * @return this stage
         */
        OptionStage free();

        /**
         * Marks the effect as heavy ({@code heavy == true}).
         *
         * @return this stage
         */
        OptionStage heavy();

        /**
         * Sets whether the effect is heavy.
         *
         * @param heavy heavy flag
         * @return this stage
         */
        OptionStage heavy(boolean heavy);

        /**
         * Sets the maximum session duration in ticks.
         *
         * @param maxDurationTicks must be {@code >= 1} at build time
         * @return this stage
         */
        OptionStage maxDurationTicks(long maxDurationTicks);

        /**
         * Sets an optional resource-pack / custom-model icon key.
         *
         * @param iconKey key, or {@code null} to omit
         * @return this stage
         */
        OptionStage iconKey(@Nullable String iconKey);

        /**
         * Builds an immutable {@link Effect}.
         *
         * @return new effect
         * @throws IllegalStateException if display name or icon was not set
         * @throws IllegalArgumentException if validation in {@link Effect} fails
         */
        Effect build();
    }

    private static final class Stages implements DisplayNameStage, IconStage, OptionStage {
        private final String id;
        private String displayName = "";
        private EffectCategory category = EffectCategory.COMMON;
        private int price;
        private int premiumPrice;
        private boolean premiumExplicit;
        private boolean heavy;
        private long maxDurationTicks = 100L;
        private String iconMaterial = "STONE";
        private @Nullable String iconKey;
        private boolean displayNameSet;
        private boolean iconSet;

        private Stages(String id) {
            this.id = id;
        }

        @Override
        public IconStage displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.displayNameSet = true;
            return this;
        }

        @Override
        public OptionStage icon(Material material) {
            Objects.requireNonNull(material, "material");
            this.iconMaterial = material.name();
            this.iconSet = true;
            return this;
        }

        @Override
        public OptionStage iconMaterial(String materialName) {
            this.iconMaterial = Objects.requireNonNull(materialName, "materialName");
            this.iconSet = true;
            return this;
        }

        @Override
        public OptionStage category(EffectCategory category) {
            this.category = Objects.requireNonNull(category, "category");
            return this;
        }

        @Override
        public OptionStage price(int price) {
            this.price = price;
            return this;
        }

        @Override
        public OptionStage premiumPrice(int premiumPrice) {
            this.premiumPrice = premiumPrice;
            this.premiumExplicit = true;
            return this;
        }

        @Override
        public OptionStage free() {
            this.price = 0;
            this.premiumPrice = 0;
            this.premiumExplicit = true;
            return this;
        }

        @Override
        public OptionStage heavy() {
            this.heavy = true;
            return this;
        }

        @Override
        public OptionStage heavy(boolean heavy) {
            this.heavy = heavy;
            return this;
        }

        @Override
        public OptionStage maxDurationTicks(long maxDurationTicks) {
            this.maxDurationTicks = maxDurationTicks;
            return this;
        }

        @Override
        public OptionStage iconKey(@Nullable String iconKey) {
            this.iconKey = iconKey;
            return this;
        }

        @Override
        public Effect build() {
            if (!displayNameSet) {
                throw new IllegalStateException("displayName is required");
            }
            if (!iconSet) {
                throw new IllegalStateException("icon is required");
            }
            int resolvedPremium = premiumExplicit ? premiumPrice : price;
            return new Effect(
                    id,
                    displayName,
                    category,
                    price,
                    resolvedPremium,
                    heavy,
                    maxDurationTicks,
                    iconMaterial,
                    iconKey);
        }
    }
}
