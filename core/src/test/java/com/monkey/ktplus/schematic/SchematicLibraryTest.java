package com.monkey.ktplus.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.schematic.config.SchematicBuildConfig;
import com.monkey.ktplus.schematic.resolve.SchematicFallbackPolicy;
import com.monkey.ktplus.schematic.resolve.SchematicMaterialResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.querz.nbt.tag.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SchematicLibraryTest {
    @TempDir
    Path tempDir;

    @Test
    void scansModernFolder() throws Exception {
        Path dataFolder = tempDir.resolve("KTPlus");
        Files.createDirectories(dataFolder);
        writeModern(dataFolder.resolve("schematics/modern/demo.schem"));

        SchematicLibrary library = new SchematicLibrary(
                dataFolder.toFile(),
                Logger.getLogger("test"),
                path -> SchematicLibraryTest.class.getResourceAsStream("/" + path));
        SchematicLibrary.LoadSummary summary = library.load();
        assertEquals(1, summary.modernLoaded());
        assertEquals(1, summary.totalCached());
        assertTrue(library.modern("demo").isPresent());
    }

    @Test
    void buildConfigDefaultsAreApplied() {
        SchematicBuildConfig config = new SchematicBuildConfig(
                "glowmissile", 6, 1L, SchematicFallbackPolicy.defaults(), 200L);
        assertEquals("glowmissile", config.schematicId());
        assertEquals(6, config.blocksPerTick());
    }

    private static void writeModern(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 2);
        root.putShort("Width", (short) 1);
        root.putShort("Height", (short) 1);
        root.putShort("Length", (short) 1);
        CompoundTag palette = new CompoundTag();
        palette.putInt("minecraft:stone", 0);
        root.put("Palette", palette);
        root.putByteArray("BlockData", new byte[] {0});
        SchematicTestFixtures.writeGzipSchematic(root, path);
    }
}
