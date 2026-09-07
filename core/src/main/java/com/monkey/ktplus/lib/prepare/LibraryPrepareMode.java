package com.monkey.ktplus.lib.prepare;

import com.monkey.ktplus.lib.model.LibraryDefinition;
import com.monkey.ktplus.lib.LibraryLoadResult;

public enum LibraryPrepareMode {
    CACHED("cached"),
    LOCAL("local+relocated"),
    DOWNLOADED("downloaded+relocated");

    private final String logLabel;

    LibraryPrepareMode(String logLabel) {
        this.logLabel = logLabel;
    }

    public String logLabel() {
        return logLabel;
    }

    public LibraryLoadResult toResult(LibraryDefinition definition) {
        if (this == DOWNLOADED) {
            return LibraryLoadResult.downloaded(definition);
        }
        return LibraryLoadResult.cached(definition);
    }
}
