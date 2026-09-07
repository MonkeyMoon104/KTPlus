package com.monkey.ktplus.storage.migration.transfer;

import java.util.List;
import java.util.Objects;

public final class TableSpec {
    private final String name;
    private final List<String> columns;
    private final List<String> primaryKeyColumns;
    private final TableCriticality criticality;
    private final boolean includeSumBalance;

    public TableSpec(
            String name,
            List<String> columns,
            List<String> primaryKeyColumns,
            TableCriticality criticality,
            boolean includeSumBalance) {
        this.name = Objects.requireNonNull(name, "name");
        this.columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
        this.primaryKeyColumns = List.copyOf(Objects.requireNonNull(primaryKeyColumns, "primaryKeyColumns"));
        this.criticality = Objects.requireNonNull(criticality, "criticality");
        this.includeSumBalance = includeSumBalance;
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("columns must not be empty for " + name);
        }
        if (primaryKeyColumns.isEmpty()) {
            throw new IllegalArgumentException("primaryKeyColumns must not be empty for " + name);
        }
    }

    public String name() {
        return name;
    }

    public List<String> columns() {
        return columns;
    }

    public List<String> primaryKeyColumns() {
        return primaryKeyColumns;
    }

    public TableCriticality criticality() {
        return criticality;
    }

    public boolean includeSumBalance() {
        return includeSumBalance;
    }

    public String selectSql() {
        StringBuilder sql = new StringBuilder("SELECT ");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(columns.get(i));
        }
        sql.append(" FROM ").append(name).append(" ORDER BY ");
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(primaryKeyColumns.get(i));
        }
        return sql.toString();
    }
}
