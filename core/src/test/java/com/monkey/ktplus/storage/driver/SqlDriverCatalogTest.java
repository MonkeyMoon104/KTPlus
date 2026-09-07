package com.monkey.ktplus.storage.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SqlDriverCatalogTest {
    @Test
    void sqliteUsesNativeDriverClass() {
        SqlDriverCatalog.DriverEntry entry = SqlDriverCatalog.entryFor("sqlite");
        assertNotNull(entry);
        assertEquals("org.sqlite.JDBC", entry.driverClass());
    }

    @Test
    void mysqlUsesCentralDriverClass() {
        SqlDriverCatalog.DriverEntry entry = SqlDriverCatalog.entryFor("mysql");
        assertNotNull(entry);
        assertEquals("mysql-connector", entry.libraryId());
        assertEquals("com.mysql.cj.jdbc.Driver", entry.driverClass());
    }

    @Test
    void postgresAliasAccepted() {
        SqlDriverCatalog.DriverEntry entry = SqlDriverCatalog.entryFor("postgres");
        assertNotNull(entry);
        assertEquals("postgresql", entry.libraryId());
    }

    @Test
    void unknownDialectReturnsNull() {
        assertNull(SqlDriverCatalog.entryFor("oracle"));
    }
}
