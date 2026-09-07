package com.monkey.ktplus.effects.runtime.block;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jspecify.annotations.Nullable;

public final class PersistedBlockPayload {
    private PersistedBlockPayload() {}

    public static String capture(Block block) {
        Objects.requireNonNull(block, "block");
        String asString = block.getBlockData().getAsString();
        if (asString != null && !asString.isEmpty()) {
            return "blockdata:" + asString;
        }
        return "";
    }

    public static void restore(Block block, Material material, @Nullable String payload) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(material, "material");
        String value = payload == null ? "" : payload.trim();
        if (value.startsWith("blockdata:")) {
            block.setBlockData(Bukkit.createBlockData(value.substring("blockdata:".length())));
            return;
        }
        block.setType(material, false);
    }
}
