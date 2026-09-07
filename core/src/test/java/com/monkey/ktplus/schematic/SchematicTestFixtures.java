package com.monkey.ktplus.schematic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;

final class SchematicTestFixtures {
    private SchematicTestFixtures() {}

    static void writeGzipSchematic(CompoundTag root, Path file) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new NBTSerializer(true).toStream(new NamedTag("Schematic", root), bytes);
        Files.write(file, bytes.toByteArray());
    }

    static byte[] gzipSchematicBytes(CompoundTag root) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new NBTSerializer(true).toStream(new NamedTag("Schematic", root), bytes);
        return bytes.toByteArray();
    }
}
