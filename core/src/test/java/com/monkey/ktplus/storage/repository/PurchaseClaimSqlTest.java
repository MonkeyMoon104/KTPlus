package com.monkey.ktplus.storage.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PurchaseClaimSqlTest {
    @Test
    void sqliteUsesInsertOrIgnore() {
        assertTrue(PurchaseClaimSql.forDialect("sqlite").contains("INSERT OR IGNORE"));
    }

    @Test
    void otherDialectsUsePlainInsert() {
        assertEquals(
                "INSERT INTO kt_purchases (uuid, effect_id) VALUES (?, ?)",
                PurchaseClaimSql.forDialect("mysql"));
    }
}
