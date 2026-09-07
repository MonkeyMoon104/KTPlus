package com.monkey.ktplus.storage.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlBinder {
    void bind(PreparedStatement statement) throws SQLException;
}
