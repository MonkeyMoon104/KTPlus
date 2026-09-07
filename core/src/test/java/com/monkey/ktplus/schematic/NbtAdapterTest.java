package com.monkey.ktplus.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.schematic.nbt.NbtFiles;
import com.monkey.ktplus.schematic.nbt.SchematicNbtRoot;
import java.io.ByteArrayInputStream;
import net.querz.nbt.tag.CompoundTag;
import org.junit.jupiter.api.Test;

final class NbtAdapterTest {
    @Test
    void readsGzipCompoundRoot() throws Exception {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 2);
        root.putShort("Width", (short) 1);
        SchematicNbtRoot loaded =
                NbtFiles.readGzip(new ByteArrayInputStream(SchematicTestFixtures.gzipSchematicBytes(root)));
        assertNotNull(loaded);
        assertEquals(2, loaded.getInt("Version", 0));
        assertEquals(1, loaded.getShort("Width", (short) 0));
        assertTrue(loaded.contains("Version"));
    }
}
