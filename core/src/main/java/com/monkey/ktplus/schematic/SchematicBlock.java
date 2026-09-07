package com.monkey.ktplus.schematic;

import java.util.Objects;

public final class SchematicBlock {
    private final int x;
    private final int y;
    private final int z;
    private final String paletteKey;

    public SchematicBlock(int x, int y, int z, String paletteKey) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.paletteKey = Objects.requireNonNull(paletteKey, "paletteKey");
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public String paletteKey() {
        return paletteKey;
    }
}
