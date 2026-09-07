package com.monkey.ktplus.lib.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RelocatedLibraryCacheTest {
    @Test
    void buildsRelocatedJarName() {
        assertEquals("hikaricp-relocated.jar", RelocatedLibraryCache.relocatedFileName("hikaricp.jar"));
        assertEquals("plain-relocated.jar", RelocatedLibraryCache.relocatedFileName("plain"));
    }

    @Test
    void metaFileSitsBesideRelocatedJar(@TempDir Path temp) {
        Path relocated = temp.resolve("lib-relocated.jar");
        Path meta = RelocatedLibraryCache.metaFile(relocated);
        assertEquals("lib-relocated.jar.meta", meta.getFileName().toString());
        assertTrue(meta.getParent().equals(relocated.getParent()));
    }
}
