package com.monkey.ktplus.schematic.nbt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;

public final class NbtFiles {
    private NbtFiles() {}

    public static SchematicNbtRoot readGzip(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        NamedTag named = NBTUtil.read(path.toFile());
        return toRoot(named);
    }

    public static SchematicNbtRoot readGzip(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        try (InputStream stream = new BufferedInputStream(input)) {
            NamedTag named = new NBTDeserializer(false).fromStream(detectDecompression(stream));
            return toRoot(named);
        }
    }

    private static SchematicNbtRoot toRoot(NamedTag named) throws IOException {
        if (named == null || named.getTag() == null) {
            throw new IOException("empty nbt root");
        }
        if (!(named.getTag() instanceof CompoundTag)) {
            throw new IOException("expected compound root, got " + named.getTag().getClass().getSimpleName());
        }
        CompoundTag compound = (CompoundTag) named.getTag();
        return new QuerzSchematicNbtRoot(named.getName(), compound);
    }

    private static InputStream detectDecompression(InputStream input) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(input, 2);
        int signature = (pushback.read() & 0xFF) + (pushback.read() << 8);
        pushback.unread(signature >> 8);
        pushback.unread(signature & 0xFF);
        if (signature == GZIPInputStream.GZIP_MAGIC) {
            return new GZIPInputStream(pushback);
        }
        return pushback;
    }
}
