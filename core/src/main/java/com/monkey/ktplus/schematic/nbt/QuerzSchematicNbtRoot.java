package com.monkey.ktplus.schematic.nbt;

import java.util.Objects;
import net.querz.nbt.tag.CompoundTag;
import org.jspecify.annotations.Nullable;

final class QuerzSchematicNbtRoot implements SchematicNbtRoot {
    private final @Nullable String rootName;
    private final CompoundTag compound;

    QuerzSchematicNbtRoot(@Nullable String rootName, CompoundTag compound) {
        this.rootName = rootName;
        this.compound = Objects.requireNonNull(compound, "compound");
    }

    @Override
    public @Nullable String rootName() {
        return rootName;
    }

    @Override
    public boolean contains(String key) {
        return compound.containsKey(key);
    }

    @Override
    public int getInt(String key, int fallback) {
        return compound.containsKey(key) ? compound.getInt(key) : fallback;
    }

    @Override
    public short getShort(String key, short fallback) {
        return compound.containsKey(key) ? compound.getShort(key) : fallback;
    }

    @Override
    public @Nullable SchematicNbtRoot getCompound(String key) {
        CompoundTag child = compound.getCompoundTag(key);
        if (child == null) {
            return null;
        }
        return new QuerzSchematicNbtRoot(null, child);
    }

    @Override
    public byte @Nullable [] getByteArray(String key) {
        if (!compound.containsKey(key)) {
            return null;
        }
        return compound.getByteArray(key);
    }

    @Override
    public int @Nullable [] getIntArray(String key) {
        if (!compound.containsKey(key)) {
            return null;
        }
        return compound.getIntArray(key);
    }

    @Override
    public @Nullable String getString(String key) {
        if (!compound.containsKey(key)) {
            return null;
        }
        return compound.getString(key);
    }

    @Override
    public CompoundTag rawCompound() {
        return compound;
    }
}
