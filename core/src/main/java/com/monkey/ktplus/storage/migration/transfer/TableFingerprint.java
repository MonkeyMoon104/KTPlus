package com.monkey.ktplus.storage.migration.transfer;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class TableFingerprint {
    private final String tableName;
    private final long rowCount;
    private final String contentChecksum;
    private final @Nullable Long sumBalance;

    public TableFingerprint(String tableName, long rowCount, String contentChecksum, @Nullable Long sumBalance) {
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        this.rowCount = rowCount;
        this.contentChecksum = Objects.requireNonNull(contentChecksum, "contentChecksum");
        this.sumBalance = sumBalance;
    }

    public String tableName() {
        return tableName;
    }

    public long rowCount() {
        return rowCount;
    }

    public String contentChecksum() {
        return contentChecksum;
    }

    public @Nullable Long sumBalance() {
        return sumBalance;
    }
}
