package com.monkey.ktplus.schematic.parse;

import com.monkey.ktplus.schematic.SchematicFormat;
import com.monkey.ktplus.schematic.SchematicKey;
import com.monkey.ktplus.schematic.SchematicLoadResult;
import com.monkey.ktplus.schematic.nbt.NbtFiles;
import com.monkey.ktplus.schematic.nbt.SchematicNbtRoot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class SchematicParser {
    private SchematicParser() {}

    public static SchematicLoadResult parse(Path path, SchematicFormat format, String id) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        SchematicKey key = new SchematicKey(format, id);
        try {
            SchematicNbtRoot root = NbtFiles.readGzip(path);
            SchematicFormat detected = SchematicFileProbe.detect(path.getFileName().toString(), root);
            if (detected != SchematicFormat.MODERN) {
                return SchematicLoadResult.failure("Legacy .schematic format is not supported in KTPlus");
            }
            return SpongeSchematicReader.read(key, root);
        } catch (IOException error) {
            return SchematicLoadResult.failure(error.getMessage() == null ? error.toString() : error.getMessage());
        } catch (RuntimeException error) {
            return SchematicLoadResult.failure(error.getMessage() == null ? error.toString() : error.getMessage());
        }
    }
}
