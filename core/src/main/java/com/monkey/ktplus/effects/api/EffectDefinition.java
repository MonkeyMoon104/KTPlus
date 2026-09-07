package com.monkey.ktplus.effects.api;

import com.monkey.ktplus.util.item.IconItemStacks;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class EffectDefinition {
    private final String id;
    private final String displayName;
    private final Material icon;
    private final @Nullable String iconKey;
    private final EffectCategory category;
    private final int premiumPrice;
    private final int price;
    private final boolean heavy;
    private final long maxDurationTicks;

    public EffectDefinition(
            String id, String displayName, Material icon, int price, boolean heavy, long maxDurationTicks) {
        this(id, displayName, icon, null, EffectCategory.COMMON, price, price, heavy, maxDurationTicks);
    }

    public EffectDefinition(
            String id,
            String displayName,
            Material icon,
            @Nullable String iconKey,
            EffectCategory category,
            int premiumPrice,
            int price,
            boolean heavy,
            long maxDurationTicks) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.iconKey = iconKey;
        this.category = Objects.requireNonNull(category, "category");
        this.premiumPrice = Math.max(0, premiumPrice);
        this.price = Math.max(0, price);
        this.heavy = heavy;
        this.maxDurationTicks = maxDurationTicks;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public ItemStack iconItem() {
        return IconItemStacks.create(icon, iconKey);
    }

    public EffectCategory category() {
        return category;
    }

    public int premiumPrice() {
        return premiumPrice;
    }

    public int price() {
        return price;
    }

    public boolean heavy() {
        return heavy;
    }

    public long maxDurationTicks() {
        return maxDurationTicks;
    }

    public String permissionNode() {
        return "ktplus." + id + ".use";
    }
}
