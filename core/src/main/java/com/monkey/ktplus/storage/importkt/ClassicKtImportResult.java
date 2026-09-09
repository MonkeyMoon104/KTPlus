package com.monkey.ktplus.storage.importkt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ClassicKtImportResult {
    private final boolean success;
    private final String message;
    private final long balancesRead;
    private final long balancesImported;
    private final long balancesSkipped;
    private final long purchasesRead;
    private final long purchasesImported;
    private final long purchasesSkipped;
    private final long purchasesUnknown;
    private final long selectionsRead;
    private final long selectionsImported;
    private final long selectionsSkipped;
    private final long selectionsUnknown;
    private final List<String> operatorMessages;
    private final List<String> unknownEffectIds;
    private final String confirmationToken;

    private ClassicKtImportResult(Builder builder) {
        this.success = builder.success;
        this.message = Objects.requireNonNull(builder.message, "message");
        this.balancesRead = builder.balancesRead;
        this.balancesImported = builder.balancesImported;
        this.balancesSkipped = builder.balancesSkipped;
        this.purchasesRead = builder.purchasesRead;
        this.purchasesImported = builder.purchasesImported;
        this.purchasesSkipped = builder.purchasesSkipped;
        this.purchasesUnknown = builder.purchasesUnknown;
        this.selectionsRead = builder.selectionsRead;
        this.selectionsImported = builder.selectionsImported;
        this.selectionsSkipped = builder.selectionsSkipped;
        this.selectionsUnknown = builder.selectionsUnknown;
        this.operatorMessages = List.copyOf(builder.operatorMessages);
        this.unknownEffectIds = List.copyOf(builder.unknownEffectIds);
        this.confirmationToken = Objects.requireNonNull(builder.confirmationToken, "confirmationToken");
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public long balancesRead() {
        return balancesRead;
    }

    public long balancesImported() {
        return balancesImported;
    }

    public long balancesSkipped() {
        return balancesSkipped;
    }

    public long purchasesRead() {
        return purchasesRead;
    }

    public long purchasesImported() {
        return purchasesImported;
    }

    public long purchasesSkipped() {
        return purchasesSkipped;
    }

    public long purchasesUnknown() {
        return purchasesUnknown;
    }

    public long selectionsRead() {
        return selectionsRead;
    }

    public long selectionsImported() {
        return selectionsImported;
    }

    public long selectionsSkipped() {
        return selectionsSkipped;
    }

    public long selectionsUnknown() {
        return selectionsUnknown;
    }

    public List<String> operatorMessages() {
        return operatorMessages;
    }

    public List<String> unknownEffectIds() {
        return unknownEffectIds;
    }

    public String confirmationToken() {
        return confirmationToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean success;
        private String message = "";
        private long balancesRead;
        private long balancesImported;
        private long balancesSkipped;
        private long purchasesRead;
        private long purchasesImported;
        private long purchasesSkipped;
        private long purchasesUnknown;
        private long selectionsRead;
        private long selectionsImported;
        private long selectionsSkipped;
        private long selectionsUnknown;
        private final List<String> operatorMessages = new ArrayList<String>();
        private final List<String> unknownEffectIds = new ArrayList<String>();
        private String confirmationToken = "";

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = Objects.requireNonNull(message, "message");
            return this;
        }

        public Builder balancesRead(long balancesRead) {
            this.balancesRead = balancesRead;
            return this;
        }

        public Builder balancesImported(long balancesImported) {
            this.balancesImported = balancesImported;
            return this;
        }

        public Builder balancesSkipped(long balancesSkipped) {
            this.balancesSkipped = balancesSkipped;
            return this;
        }

        public Builder purchasesRead(long purchasesRead) {
            this.purchasesRead = purchasesRead;
            return this;
        }

        public Builder purchasesImported(long purchasesImported) {
            this.purchasesImported = purchasesImported;
            return this;
        }

        public Builder purchasesSkipped(long purchasesSkipped) {
            this.purchasesSkipped = purchasesSkipped;
            return this;
        }

        public Builder purchasesUnknown(long purchasesUnknown) {
            this.purchasesUnknown = purchasesUnknown;
            return this;
        }

        public Builder selectionsRead(long selectionsRead) {
            this.selectionsRead = selectionsRead;
            return this;
        }

        public Builder selectionsImported(long selectionsImported) {
            this.selectionsImported = selectionsImported;
            return this;
        }

        public Builder selectionsSkipped(long selectionsSkipped) {
            this.selectionsSkipped = selectionsSkipped;
            return this;
        }

        public Builder selectionsUnknown(long selectionsUnknown) {
            this.selectionsUnknown = selectionsUnknown;
            return this;
        }

        public Builder addMessage(String line) {
            operatorMessages.add(Objects.requireNonNull(line, "line"));
            return this;
        }

        public Builder unknownEffectIds(List<String> ids) {
            unknownEffectIds.clear();
            unknownEffectIds.addAll(Objects.requireNonNull(ids, "ids"));
            Collections.sort(unknownEffectIds);
            return this;
        }

        public Builder confirmationToken(String confirmationToken) {
            this.confirmationToken = Objects.requireNonNull(confirmationToken, "confirmationToken");
            return this;
        }

        public ClassicKtImportResult build() {
            return new ClassicKtImportResult(this);
        }
    }
}
