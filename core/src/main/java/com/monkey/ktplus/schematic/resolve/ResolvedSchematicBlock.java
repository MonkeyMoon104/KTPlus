package com.monkey.ktplus.schematic.resolve;

import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

public final class ResolvedSchematicBlock {
    private final @Nullable Material material;
    private final boolean skipped;
    private final @Nullable String reason;

    private ResolvedSchematicBlock(@Nullable Material material, boolean skipped, @Nullable String reason) {
        this.material = material;
        this.skipped = skipped;
        this.reason = reason;
    }

    public static ResolvedSchematicBlock resolved(Material material) {
        return new ResolvedSchematicBlock(material, false, null);
    }

    public static ResolvedSchematicBlock skipped(String reason) {
        return new ResolvedSchematicBlock(null, true, reason);
    }

    public static ResolvedSchematicBlock fallback(Material material, String reason) {
        return new ResolvedSchematicBlock(material, false, reason);
    }

    public @Nullable Material material() {
        return material;
    }

    public boolean skipped() {
        return skipped;
    }

    public @Nullable String reason() {
        return reason;
    }

    public boolean isPlaceable() {
        return !skipped && material != null;
    }
}
