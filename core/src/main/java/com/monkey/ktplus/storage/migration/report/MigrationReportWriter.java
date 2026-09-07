package com.monkey.ktplus.storage.migration.report;

import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.PhaseResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public final class MigrationReportWriter {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private MigrationReportWriter() {}

    public static Path write(MigrationContext context) throws IOException {
        Objects.requireNonNull(context, "context");
        Path reportPath = context.backupsDirectory()
                .resolve("migration-report-" + TS.format(LocalDateTime.now()) + ".md");
        Files.createDirectories(reportPath.getParent());
        StringBuilder md = new StringBuilder();
        md.append("# KTPlus migration report\n\n");
        md.append("- generatedAt: ").append(Instant.now()).append('\n');
        md.append("- source: ").append(context.request().sourceDialect().configValue()).append('\n');
        md.append("- target: ").append(context.request().targetDialect().configValue()).append('\n');
        md.append("- dryRun: ").append(context.request().dryRun()).append('\n');
        md.append("- configSwapped: ").append(context.configSwapped()).append('\n');
        md.append("- validationPassed: ").append(context.validationPassed()).append('\n');
        md.append("- economicHardFail: ").append(context.economicValidationHardFailed()).append('\n');
        
        md.append("- snapshotPath: ")
                .append(context.snapshotDirectory() == null ? "(none)" : context.snapshotDirectory())
                .append('\n');
        md.append("- exportPath: ")
                .append(context.exportDirectory() == null ? "(none)" : context.exportDirectory())
                .append('\n');
        md.append('\n');
        md.append("## Phases\n\n");
        for (Map.Entry<String, PhaseResult> entry : context.phaseResults().entrySet()) {
            md.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue().status())
                    .append(" — ")
                    .append(entry.getValue().message())
                    .append('\n');
        }
        md.append('\n');
        md.append("## Row counts\n\n");
        md.append("| table | exported | imported | skipped |\n");
        md.append("|---|---:|---:|---:|\n");
        for (String table : context.exportedRowCounts().keySet()) {
            md.append("| ")
                    .append(table)
                    .append(" | ")
                    .append(context.exportedRowCounts().getOrDefault(table, Long.valueOf(0L)))
                    .append(" | ")
                    .append(context.importedRowCounts().getOrDefault(table, Long.valueOf(0L)))
                    .append(" | ")
                    .append(context.skippedRowCounts().getOrDefault(table, Long.valueOf(0L)))
                    .append(" |\n");
        }
        
        for (String table : context.skippedRowCounts().keySet()) {
            if (!context.exportedRowCounts().containsKey(table)) {
                md.append("| ")
                        .append(table)
                        .append(" | 0 | ")
                        .append(context.importedRowCounts().getOrDefault(table, Long.valueOf(0L)))
                        .append(" | ")
                        .append(context.skippedRowCounts().get(table))
                        .append(" |\n");
            }
        }
        md.append('\n');
        md.append("## Operator messages\n\n");
        for (String message : context.operatorMessages()) {
            md.append("- ").append(message).append('\n');
        }
        md.append('\n');
        md.append("## Warnings\n\n");
        if (context.warnings().isEmpty()) {
            md.append("- (none)\n");
        } else {
            for (String warning : context.warnings()) {
                md.append("- ").append(warning).append('\n');
            }
        }
        Files.writeString(reportPath, md.toString(), StandardCharsets.UTF_8);
        context.setReportPath(reportPath);
        return reportPath;
    }
}
