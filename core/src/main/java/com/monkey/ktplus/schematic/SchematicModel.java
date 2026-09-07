package com.monkey.ktplus.schematic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class SchematicModel {
    private final SchematicKey key;
    private final int width;
    private final int height;
    private final int length;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final List<SchematicBlock> blocks;
    private final List<SchematicBlock> blocksSortedByLayer;

    public SchematicModel(
            SchematicKey key,
            int width,
            int height,
            int length,
            int offsetX,
            int offsetY,
            int offsetZ,
            List<SchematicBlock> blocks) {
        this.key = Objects.requireNonNull(key, "key");
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.length = Math.max(0, length);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.blocks = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(blocks, "blocks")));
        List<SchematicBlock> sorted = new ArrayList<>(this.blocks);
        sorted.sort(Comparator.comparingInt(SchematicBlock::y)
                .thenComparingInt(SchematicBlock::z)
                .thenComparingInt(SchematicBlock::x));
        this.blocksSortedByLayer = Collections.unmodifiableList(sorted);
    }

    public SchematicKey key() {
        return key;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int length() {
        return length;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public int offsetZ() {
        return offsetZ;
    }

    public List<SchematicBlock> blocks() {
        return blocks;
    }

    public List<SchematicBlock> blocksSortedByLayer() {
        return blocksSortedByLayer;
    }

    public int blockCount() {
        return blocks.size();
    }
}
