package com.monkey.ktplus.storage.migration;

import java.util.Objects;

public final class PhaseResult {
    private final PhaseStatus status;
    private final String message;

    private PhaseResult(PhaseStatus status, String message) {
        this.status = Objects.requireNonNull(status, "status");
        this.message = Objects.requireNonNull(message, "message");
    }

    public static PhaseResult success(String message) {
        return new PhaseResult(PhaseStatus.SUCCESS, message);
    }

    public static PhaseResult skipped(String message) {
        return new PhaseResult(PhaseStatus.SKIPPED, message);
    }

    public static PhaseResult failed(String message) {
        return new PhaseResult(PhaseStatus.FAILED, message);
    }

    public PhaseStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public boolean failed() {
        return status == PhaseStatus.FAILED;
    }

    public boolean succeeded() {
        return status == PhaseStatus.SUCCESS;
    }
}
