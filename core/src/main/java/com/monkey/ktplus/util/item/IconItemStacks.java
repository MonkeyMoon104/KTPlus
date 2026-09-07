package com.monkey.ktplus.util.item;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class IconItemStacks {
    private IconItemStacks() {}

    public static ItemStack create(Material material, @Nullable String requestedKey) {
        Objects.requireNonNull(material, "material");
        Material resolved = resolveSkullMaterial(material, requestedKey);
        return new ItemStack(resolved, 1);
    }

    private static Material resolveSkullMaterial(Material material, @Nullable String requestedKey) {
        if (requestedKey == null || requestedKey.trim().isEmpty()) {
            return material;
        }
        String materialName = material.name();
        if (!materialName.contains("SKULL") && material != Material.PLAYER_HEAD) {
            return material;
        }
        String key = requestedKey.trim().toUpperCase(Locale.ROOT);
        if (key.contains("WITHER")) {
            return Material.WITHER_SKELETON_SKULL;
        }
        if (key.contains("ZOMBIE")) {
            return Material.ZOMBIE_HEAD;
        }
        if (key.contains("CREEPER")) {
            return Material.CREEPER_HEAD;
        }
        if (key.contains("DRAGON")) {
            return Material.DRAGON_HEAD;
        }
        return Material.PLAYER_HEAD;
    }
}
