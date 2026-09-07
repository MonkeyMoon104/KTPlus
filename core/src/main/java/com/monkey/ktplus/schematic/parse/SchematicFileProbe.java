package com.monkey.ktplus.schematic.parse;

import com.monkey.ktplus.schematic.SchematicFormat;
import com.monkey.ktplus.schematic.nbt.SchematicNbtRoot;
import org.jspecify.annotations.Nullable;

public final class SchematicFileProbe {
    private SchematicFileProbe() {}

    public static @Nullable SchematicFormat detect(@Nullable String fileName, SchematicNbtRoot root) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".schem")) {
                return SchematicFormat.MODERN;
            }
            if (lower.endsWith(".schematic")) {
                return null;
            }
        }
        if (root.contains("Version")) {
            return SchematicFormat.MODERN;
        }
        if (root.contains("Materials") || root.contains("Blocks")) {
            return null;
        }
        return null;
    }
}
