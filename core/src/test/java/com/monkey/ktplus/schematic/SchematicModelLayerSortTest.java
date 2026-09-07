package com.monkey.ktplus.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.schematic.parse.VarIntReader;
import org.junit.jupiter.api.Test;

final class SchematicModelLayerSortTest {
    @Test
    void sortsByYThenZThenX() {
        SchematicKey key = SchematicKey.modern("demo");
        SchematicModel model = new SchematicModel(
                key,
                3,
                3,
                3,
                0,
                0,
                0,
                java.util.Arrays.asList(
                        new SchematicBlock(2, 2, 2, "minecraft:stone"),
                        new SchematicBlock(0, 0, 0, "minecraft:iron_block"),
                        new SchematicBlock(1, 0, 1, "minecraft:gold_block"),
                        new SchematicBlock(0, 1, 0, "minecraft:diamond_block")));
        assertEquals("minecraft:iron_block", model.blocksSortedByLayer().get(0).paletteKey());
        assertEquals("minecraft:gold_block", model.blocksSortedByLayer().get(1).paletteKey());
        assertEquals("minecraft:diamond_block", model.blocksSortedByLayer().get(2).paletteKey());
        assertEquals("minecraft:stone", model.blocksSortedByLayer().get(3).paletteKey());
    }

    @Test
    void varIntReaderDecodesMultipleValues() {
        byte[] encoded = new byte[] {(byte) 0xFE, 0x01, (byte) 0x90, 0x04};
        int[] values = VarIntReader.decode(encoded);
        assertEquals(2, values.length);
        assertEquals(254, values[0]);
        assertEquals(528, values[1]);
        assertTrue(values[1] > 0);
    }
}
