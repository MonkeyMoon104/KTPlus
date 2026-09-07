package com.monkey.ktplus.schematic.parse;

import com.monkey.ktplus.schematic.SchematicBlock;
import com.monkey.ktplus.schematic.SchematicKey;
import com.monkey.ktplus.schematic.SchematicLoadResult;
import com.monkey.ktplus.schematic.SchematicModel;
import com.monkey.ktplus.schematic.nbt.SchematicNbtRoot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.querz.nbt.tag.CompoundTag;
import org.jspecify.annotations.Nullable;

public final class SpongeSchematicReader {
    private SpongeSchematicReader() {}

    public static SchematicLoadResult read(SchematicKey key, SchematicNbtRoot root) {
        int version = root.getInt("Version", 0);
        if (version != 2 && version != 3) {
            return SchematicLoadResult.failure("unsupported sponge version: " + version);
        }
        int width = Short.toUnsignedInt(root.getShort("Width", (short) 0));
        int height = Short.toUnsignedInt(root.getShort("Height", (short) 0));
        int length = Short.toUnsignedInt(root.getShort("Length", (short) 0));
        if (width <= 0 || height <= 0 || length <= 0) {
            return SchematicLoadResult.failure("invalid schematic dimensions");
        }
        int[] offset = readOffset(root);
        CompoundTag paletteCompound = readPaletteCompound(root, version);
        if (paletteCompound == null) {
            return SchematicLoadResult.failure("missing palette");
        }
        int[] blockIndices = readBlockIndices(root, version, width, height, length);
        if (blockIndices == null) {
            return SchematicLoadResult.failure("missing block data");
        }
        Map<Integer, String> palette = readPalette(paletteCompound);
        List<SchematicBlock> blocks = new ArrayList<>();
        int volume = width * height * length;
        for (int index = 0; index < Math.min(volume, blockIndices.length); index++) {
            String stateKey = palette.get(blockIndices[index]);
            if (stateKey == null || isAirState(stateKey)) {
                continue;
            }
            int x = index % width;
            int temp = index / width;
            int z = temp % length;
            int y = temp / length;
            blocks.add(new SchematicBlock(x, y, z, stateKey));
        }
        return SchematicLoadResult.success(new SchematicModel(
                key, width, height, length, offset[0], offset[1], offset[2], blocks));
    }

    private static int[] readOffset(SchematicNbtRoot root) {
        int[] offset = root.getIntArray("Offset");
        if (offset == null || offset.length < 3) {
            return new int[] {0, 0, 0};
        }
        return new int[] {offset[0], offset[1], offset[2]};
    }

    private static @Nullable CompoundTag readPaletteCompound(SchematicNbtRoot root, int version) {
        if (version >= 3) {
            SchematicNbtRoot blocksSection = root.getCompound("Blocks");
            if (blocksSection != null) {
                SchematicNbtRoot palette = blocksSection.getCompound("Palette");
                if (palette != null) {
                    return palette.rawCompound();
                }
            }
        }
        SchematicNbtRoot palette = root.getCompound("Palette");
        return palette == null ? null : palette.rawCompound();
    }

    private static @Nullable int[] readBlockIndices(
            SchematicNbtRoot root, int version, int width, int height, int length) {
        byte[] blockData = null;
        if (version >= 3) {
            SchematicNbtRoot blocksSection = root.getCompound("Blocks");
            if (blocksSection != null) {
                blockData = blocksSection.getByteArray("Data");
            }
        }
        if (blockData == null) {
            blockData = root.getByteArray("BlockData");
        }
        if (blockData != null && blockData.length > 0) {
            return VarIntReader.decode(blockData);
        }
        int[] intArray = root.getIntArray("BlockData");
        if (intArray != null && intArray.length > 0) {
            return intArray;
        }
        int volume = width * height * length;
        if (blockData != null && blockData.length == volume) {
            int[] direct = new int[blockData.length];
            for (int i = 0; i < blockData.length; i++) {
                direct[i] = blockData[i] & 0xFF;
            }
            return direct;
        }
        return null;
    }

    private static Map<Integer, String> readPalette(CompoundTag paletteCompound) {
        Map<Integer, String> palette = new HashMap<>();
        for (String stateKey : paletteCompound.keySet()) {
            palette.put(paletteCompound.getInt(stateKey), stateKey);
        }
        return palette;
    }

    private static boolean isAirState(String stateKey) {
        String normalized = stateKey.toLowerCase(Locale.ROOT);
        return normalized.equals("minecraft:air")
                || normalized.equals("minecraft:cave_air")
                || normalized.equals("minecraft:void_air")
                || normalized.equals("minecraft:structure_void");
    }
}
