package com.monkey.ktplus.lib;

import com.monkey.ktplus.lib.model.LibraryDefinition;
import java.util.Objects;

public final class LibraryLoadResult {
    public enum Source {
        REUSED,
        CACHED,
        DOWNLOADED
    }

    private final LibraryDefinition definition;
    private final Source source;

    private LibraryLoadResult(LibraryDefinition definition, Source source) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.source = Objects.requireNonNull(source, "source");
    }

    public static LibraryLoadResult reused(LibraryDefinition definition) {
        return new LibraryLoadResult(definition, Source.REUSED);
    }

    public static LibraryLoadResult cached(LibraryDefinition definition) {
        return new LibraryLoadResult(definition, Source.CACHED);
    }

    public static LibraryLoadResult downloaded(LibraryDefinition definition) {
        return new LibraryLoadResult(definition, Source.DOWNLOADED);
    }

    public LibraryDefinition definition() {
        return definition;
    }

    public Source source() {
        return source;
    }
}
