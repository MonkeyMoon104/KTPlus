package com.monkey.ktplus.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.schematic.parse.SchematicParser;
import java.nio.file.Path;
import net.querz.nbt.tag.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SpongeSchematicReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void readsSpongeV2Schematic() throws Exception {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 2);
        root.putShort("Width", (short) 2);
        root.putShort("Height", (short) 1);
        root.putShort("Length", (short) 2);
        CompoundTag palette = new CompoundTag();
        palette.putInt("minecraft:air", 0);
        palette.putInt("minecraft:iron_block", 1);
        palette.putInt("minecraft:gold_block", 2);
        root.put("Palette", palette);
        int[] indices = {1, 2, 0, 0};
        root.putByteArray("BlockData", encodeVarInts(indices));

        Path file = tempDir.resolve("demo.schem");
        SchematicTestFixtures.writeGzipSchematic(root, file);

        SchematicLoadResult result = SchematicParser.parse(file, SchematicFormat.MODERN, "demo");
        assertTrue(result.success(), result.error());
        SchematicModel model = result.model();
        assertNotNull(model);
        assertEquals(2, model.width());
        assertEquals(1, model.height());
        assertEquals(2, model.length());
        assertEquals(2, model.blockCount());
        assertEquals("minecraft:iron_block", model.blocks().get(0).paletteKey());
        assertEquals("minecraft:gold_block", model.blocks().get(1).paletteKey());
    }

    @Test
    void readsSingleBlockSchematic() throws Exception {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 2);
        root.putShort("Width", (short) 1);
        root.putShort("Height", (short) 1);
        root.putShort("Length", (short) 1);
        CompoundTag palette = new CompoundTag();
        palette.putInt("minecraft:stone", 0);
        root.put("Palette", palette);
        root.putByteArray("BlockData", encodeVarInts(new int[] {0}));

        Path file = tempDir.resolve("single.schem");
        SchematicTestFixtures.writeGzipSchematic(root, file);
        SchematicLoadResult result = SchematicParser.parse(file, SchematicFormat.MODERN, "single");
        assertTrue(result.success(), result.error());
        assertEquals(1, result.model().blockCount());
        assertEquals("minecraft:stone", result.model().blocks().get(0).paletteKey());
    }

    private static byte[] encodeVarInts(int[] values) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int value : values) {
            int current = value;
            while ((current & ~0x7F) != 0) {
                out.write((current & 0x7F) | 0x80);
                current >>>= 7;
            }
            out.write(current);
        }
        return out.toByteArray();
    }
}
