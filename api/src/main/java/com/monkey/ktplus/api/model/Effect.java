package com.monkey.ktplus.api.model;

import com.monkey.ktplus.api.builder.EffectBuilders;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

/**
 * Immutable kill-effect descriptor used throughout the public API.
 *
 * <p>Identity is the normalized {@link #id()} compared <em>case-insensitively</em>: {@link
 * #equals(Object)} and {@link #hashCode()} ignore display fields and treat {@code "Lightning"} and
 * {@code "lightning"} as the same effect.
 *
 * <p>Build new instances with {@link #builder(String)}:
 *
 * <pre>{@code
 * Effect effect = Effect.builder("my_effect")
 *         .displayName("My Effect")
 *         .icon(Material.BLAZE_POWDER)
 *         .category(EffectCategory.RARE)
 *         .price(500)
 *         .build();
 * }</pre>
 *
 * @param id unique effect token (trimmed, non-blank); compared case-insensitively for identity
 * @param displayName human-readable name (trimmed, non-blank)
 * @param category rarity / GUI grouping
 * @param price standard unlock price ({@code >= 0}; negatives clamped to {@code 0})
 * @param premiumPrice alternate / premium unlock price ({@code >= 0})
 * @param heavy whether the effect counts against the heavy-session budget
 * @param maxDurationTicks upper bound for session lifetime in ticks ({@code >= 1})
 * @param iconMaterial Bukkit material name for GUI icons (normalized uppercase)
 * @param iconKey optional custom model / resource-pack icon key; blank becomes {@code null}
 * @since 4.0.3
 * @see EffectBuilders
 */
public record Effect(
        String id,
        String displayName,
        EffectCategory category,
        int price,
        int premiumPrice,
        boolean heavy,
        long maxDurationTicks,
        String iconMaterial,
        @Nullable String iconKey) {
    /**
     * Compact constructor: validates and normalizes components.
     *
     * @throws NullPointerException if a required component is null
     * @throws IllegalArgumentException if {@code id}, {@code displayName}, or {@code iconMaterial}
     *     is blank, or {@code maxDurationTicks < 1}
     */
    public Effect {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(iconMaterial, "iconMaterial");
        id = normalizeToken(id, "id");
        displayName = displayName.trim();
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        iconMaterial = normalizeToken(iconMaterial, "iconMaterial").toUpperCase(Locale.ROOT);
        if (iconKey != null) {
            String trimmed = iconKey.trim();
            iconKey = trimmed.isEmpty() ? null : trimmed;
        }
        price = Math.max(0, price);
        premiumPrice = Math.max(0, premiumPrice);
        if (maxDurationTicks < 1L) {
            throw new IllegalArgumentException("maxDurationTicks must be >= 1");
        }
    }

    /**
     * Starts a staged builder for a new effect with the given {@code id}.
     *
     * <pre>{@code
     * Effect e = Effect.builder("fireworks")
     *         .displayName("Fireworks")
     *         .iconMaterial("FIREWORK_ROCKET")
     *         .free()
     *         .build();
     * }</pre>
     *
     * @param id effect id (validated when {@link EffectBuilders.OptionStage#build()} runs)
     * @return display-name builder stage
     * @throws NullPointerException if {@code id} is null
     */
    public static EffectBuilders.DisplayNameStage builder(String id) {
        return EffectBuilders.start(id);
    }

    /**
     * Returns a builder pre-filled from this instance (copy-on-write style edits).
     *
     * @return option stage ready for further mutation and {@link EffectBuilders.OptionStage#build()}
     */
    public EffectBuilders.OptionStage toBuilder() {
        return EffectBuilders.from(this);
    }

    /**
     * Returns a copy with a different display name.
     *
     * @param displayName new display name
     * @return new effect instance
     */
    public Effect withDisplayName(String displayName) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns a copy with a different category.
     *
     * @param category new category
     * @return new effect instance
     */
    public Effect withCategory(EffectCategory category) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns a copy with a different standard price.
     *
     * @param price new price ({@code >= 0} after clamping)
     * @return new effect instance
     */
    public Effect withPrice(int price) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns a copy with a different premium price.
     *
     * @param premiumPrice new premium price ({@code >= 0} after clamping)
     * @return new effect instance
     */
    public Effect withPremiumPrice(int premiumPrice) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns a copy with the heavy flag set or cleared.
     *
     * @param heavy whether this effect is heavy
     * @return new effect instance
     */
    public Effect withHeavy(boolean heavy) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns a copy with a different max duration.
     *
     * @param maxDurationTicks duration in ticks ({@code >= 1})
     * @return new effect instance
     */
    public Effect withMaxDurationTicks(long maxDurationTicks) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns a copy using {@code material.name()} as the icon material.
     *
     * @param material Bukkit material
     * @return new effect instance
     * @throws NullPointerException if {@code material} is null
     */
    public Effect withIcon(Material material) {
        Objects.requireNonNull(material, "material");
        return withIconMaterial(material.name());
    }

    /**
     * Returns a copy with a different icon material name.
     *
     * @param iconMaterial material name (normalized uppercase)
     * @return new effect instance
     */
    public Effect withIconMaterial(String iconMaterial) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns a copy with a different optional icon key.
     *
     * @param iconKey custom icon key, or {@code null} / blank to clear
     * @return new effect instance
     */
    public Effect withIconKey(@Nullable String iconKey) {
        return new Effect(
                id, displayName, category, price, premiumPrice, heavy, maxDurationTicks, iconMaterial, iconKey);
    }

    /**
     * Returns the default use-permission node for this effect ({@code ktplus.<id>.use}).
     *
     * @return permission node string
     */
    public String permissionNode() {
        return "ktplus." + id + ".use";
    }

    /**
     * Whether the standard price is zero (no charge for a normal purchase).
     *
     * @return {@code true} if {@link #price()} is {@code <= 0}
     */
    public boolean free() {
        return price <= 0;
    }

    /**
     * Resolves {@link #iconMaterial()} to a Bukkit {@link Material}, falling back to {@link
     * Material#STONE} if unknown.
     *
     * @return non-null material
     */
    public Material icon() {
        Material material = Material.matchMaterial(iconMaterial);
        return material == null ? Material.STONE : material;
    }

    /**
     * Case-insensitive id equality; other components are ignored.
     *
     * @param other other object
     * @return {@code true} if {@code other} is an {@code Effect} with the same id ignoring case
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Effect effect)) {
            return false;
        }
        return id.equalsIgnoreCase(effect.id);
    }

    /**
     * Hash code derived from the lower-cased id, consistent with {@link #equals(Object)}.
     *
     * @return hash of {@code id.toLowerCase(Locale.ROOT)}
     */
    @Override
    public int hashCode() {
        return id.toLowerCase(Locale.ROOT).hashCode();
    }

    private static String normalizeToken(String value, String name) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
