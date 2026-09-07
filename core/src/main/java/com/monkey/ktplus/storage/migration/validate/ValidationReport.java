package com.monkey.ktplus.storage.migration.validate;

import java.util.List;
import java.util.Objects;

public final class ValidationReport {
    private final boolean passed;
    private final boolean economicHardFail;
    private final List<String> lines;

    public ValidationReport(boolean passed, boolean economicHardFail, List<String> lines) {
        this.passed = passed;
        this.economicHardFail = economicHardFail;
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
    }

    public boolean passed() {
        return passed;
    }

    public boolean economicHardFail() {
        return economicHardFail;
    }

    public List<String> lines() {
        return lines;
    }
}
