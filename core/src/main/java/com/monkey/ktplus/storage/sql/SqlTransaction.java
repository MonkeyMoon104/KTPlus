package com.monkey.ktplus.storage.sql;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlTransaction {
    void execute(Connection connection) throws SQLException;
}
