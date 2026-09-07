package com.monkey.ktplus.storage.migration.snapshot;

import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class SnapshotManifest {
    public static final String FILE_NAME = "manifest.properties";

    private final MigrationDialect sourceDialect;
    private final String kind;
    private final long createdAtEpochMs;
    private final Map<String, TableFingerprint> tables;

    public SnapshotManifest(
            MigrationDialect sourceDialect,
            String kind,
            long createdAtEpochMs,
            Map<String, TableFingerprint> tables) {
        this.sourceDialect = Objects.requireNonNull(sourceDialect, "sourceDialect");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.createdAtEpochMs = createdAtEpochMs;
        this.tables = Collections.unmodifiableMap(new LinkedHashMap<String, TableFingerprint>(
                Objects.requireNonNull(tables, "tables")));
    }

    public MigrationDialect sourceDialect() {
        return sourceDialect;
    }

    public String kind() {
        return kind;
    }

    public long createdAtEpochMs() {
        return createdAtEpochMs;
    }

    public Map<String, TableFingerprint> tables() {
        return tables;
    }

    public @Nullable TableFingerprint table(String name) {
        return tables.get(name);
    }

    public void writeTo(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Files.createDirectories(directory);
        Path file = directory.resolve(FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("# KTPlus migration snapshot manifest");
            writer.newLine();
            writer.write("version=1");
            writer.newLine();
            writer.write("kind=" + kind);
            writer.newLine();
            writer.write("sourceDialect=" + sourceDialect.configValue());
            writer.newLine();
            writer.write("createdAtEpochMs=" + createdAtEpochMs);
            writer.newLine();
            writer.write("createdAt=" + Instant.ofEpochMilli(createdAtEpochMs));
            writer.newLine();
            for (TableFingerprint fingerprint : tables.values()) {
                String prefix = "table." + fingerprint.tableName() + ".";
                writer.write(prefix + "rowCount=" + fingerprint.rowCount());
                writer.newLine();
                writer.write(prefix + "checksum=" + fingerprint.contentChecksum());
                writer.newLine();
                if (fingerprint.sumBalance() != null) {
                    writer.write(prefix + "sumBalance=" + fingerprint.sumBalance());
                    writer.newLine();
                }
            }
        }
    }

    public static SnapshotManifest readFrom(Path directory) throws IOException {
        Path file = directory.resolve(FILE_NAME);
        Map<String, String> values = new LinkedHashMap<String, String>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                values.put(trimmed.substring(0, eq), trimmed.substring(eq + 1));
            }
        }
        MigrationDialect dialect = MigrationDialect.parse(values.getOrDefault("sourceDialect", "sqlite"));
        String kind = values.getOrDefault("kind", "unknown");
        long createdAt = Long.parseLong(values.getOrDefault("createdAtEpochMs", "0"));
        Map<String, TableFingerprint> tables = new LinkedHashMap<String, TableFingerprint>();
        for (String key : values.keySet()) {
            if (!key.startsWith("table.") || !key.endsWith(".rowCount")) {
                continue;
            }
            String table = key.substring("table.".length(), key.length() - ".rowCount".length());
            long rowCount = Long.parseLong(values.get(key));
            String checksum = values.getOrDefault("table." + table + ".checksum", "");
            Long sumBalance = null;
            String sumRaw = values.get("table." + table + ".sumBalance");
            if (sumRaw != null && !sumRaw.isBlank()) {
                sumBalance = Long.valueOf(Long.parseLong(sumRaw));
            }
            tables.put(table, new TableFingerprint(table, rowCount, checksum, sumBalance));
        }
        return new SnapshotManifest(dialect, kind, createdAt, tables);
    }
}
