package com.monkey.ktplus.storage.migration;

import java.util.Objects;

public final class MigrationRequest {
    private final MigrationDialect sourceDialect;
    private final MigrationDialect targetDialect;
    private final boolean dryRun;
    private final boolean includeTempBlocks;
    private final boolean forcePendingInventory;
    private final boolean allowNonemptyTarget;
    private final String typedTargetConfirmation;

    private MigrationRequest(Builder builder) {
        this.sourceDialect = Objects.requireNonNull(builder.sourceDialect, "sourceDialect");
        this.targetDialect = Objects.requireNonNull(builder.targetDialect, "targetDialect");
        this.dryRun = builder.dryRun;
        this.includeTempBlocks = builder.includeTempBlocks;
        this.forcePendingInventory = builder.forcePendingInventory;
        this.allowNonemptyTarget = builder.allowNonemptyTarget;
        this.typedTargetConfirmation = Objects.requireNonNull(builder.typedTargetConfirmation, "typedTargetConfirmation");
        if (sourceDialect == targetDialect) {
            throw new IllegalArgumentException("source and target dialects must differ");
        }
    }

    public MigrationDialect sourceDialect() {
        return sourceDialect;
    }

    public MigrationDialect targetDialect() {
        return targetDialect;
    }

    public boolean dryRun() {
        return dryRun;
    }

    public boolean includeTempBlocks() {
        return includeTempBlocks;
    }

    public boolean forcePendingInventory() {
        return forcePendingInventory;
    }

    public boolean allowNonemptyTarget() {
        return allowNonemptyTarget;
    }

    public String typedTargetConfirmation() {
        return typedTargetConfirmation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private MigrationDialect sourceDialect;
        private MigrationDialect targetDialect;
        private boolean dryRun;
        private boolean includeTempBlocks;
        private boolean forcePendingInventory;
        private boolean allowNonemptyTarget;
        private String typedTargetConfirmation = "";

        private Builder() {}

        public Builder sourceDialect(MigrationDialect sourceDialect) {
            this.sourceDialect = sourceDialect;
            return this;
        }

        public Builder targetDialect(MigrationDialect targetDialect) {
            this.targetDialect = targetDialect;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder includeTempBlocks(boolean includeTempBlocks) {
            this.includeTempBlocks = includeTempBlocks;
            return this;
        }

        public Builder forcePendingInventory(boolean forcePendingInventory) {
            this.forcePendingInventory = forcePendingInventory;
            return this;
        }

        public Builder allowNonemptyTarget(boolean allowNonemptyTarget) {
            this.allowNonemptyTarget = allowNonemptyTarget;
            return this;
        }

        public Builder typedTargetConfirmation(String typedTargetConfirmation) {
            this.typedTargetConfirmation =
                    typedTargetConfirmation == null ? "" : typedTargetConfirmation;
            return this;
        }

        public MigrationRequest build() {
            return new MigrationRequest(this);
        }
    }
}
