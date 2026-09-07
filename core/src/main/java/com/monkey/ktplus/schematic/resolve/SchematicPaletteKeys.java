package com.monkey.ktplus.schematic.resolve;

import java.util.Locale;

public final class SchematicPaletteKeys {
    private SchematicPaletteKeys() {}

    public static String baseBlockId(String paletteKey) {
        String normalized = paletteKey.toLowerCase(Locale.ROOT);
        int bracket = normalized.indexOf('[');
        if (bracket > 0) {
            normalized = normalized.substring(0, bracket);
        }
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon < normalized.length() - 1) {
            normalized = normalized.substring(colon + 1);
        }
        return normalized;
    }
}
