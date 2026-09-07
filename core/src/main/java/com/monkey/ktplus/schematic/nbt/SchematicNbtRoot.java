package com.monkey.ktplus.schematic.nbt;

import java.util.List;
import net.querz.nbt.tag.CompoundTag;
import org.jspecify.annotations.Nullable;

public interface SchematicNbtRoot {
    @Nullable String rootName();

    boolean contains(String key);

    int getInt(String key, int fallback);

    short getShort(String key, short fallback);

    @Nullable SchematicNbtRoot getCompound(String key);

    byte @Nullable [] getByteArray(String key);

    int @Nullable [] getIntArray(String key);

    @Nullable String getString(String key);

    CompoundTag rawCompound();
}
