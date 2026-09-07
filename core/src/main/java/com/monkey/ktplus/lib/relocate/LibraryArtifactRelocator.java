package com.monkey.ktplus.lib.relocate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;

public final class LibraryArtifactRelocator {
    public void relocate(Path inputJar, Path outputJar) throws IOException {
        Objects.requireNonNull(inputJar, "inputJar");
        Objects.requireNonNull(outputJar, "outputJar");
        Files.createDirectories(outputJar.getParent());
        List<Relocation> relocations = new ArrayList<Relocation>();
        for (Map.Entry<String, String> entry : LibraryRelocationRules.rules().entrySet()) {
            relocations.add(new Relocation(entry.getKey(), entry.getValue()));
        }
        File input = inputJar.toFile();
        File output = outputJar.toFile();
        new JarRelocator(input, output, relocations).run();
    }
}
