package com.monkey.ktplus.effects.runtime.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public final class TemporaryBlockChange {
    private final String key;
    private final Block block;
    private final BlockState original;

    public TemporaryBlockChange(String key, Block block, BlockState original) {
        this.key = key;
        this.block = block;
        this.original = original;
    }

    public String key() {
        return key;
    }

    public void restore() {
        original.update(true, false);
    }

    public Block block() {
        return block;
    }
}
