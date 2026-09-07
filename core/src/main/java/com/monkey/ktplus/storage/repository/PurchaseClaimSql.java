package com.monkey.ktplus.storage.repository;

final class PurchaseClaimSql {
    private PurchaseClaimSql() {}

    static String forDialect(String dialect) {
        if ("sqlite".equals(dialect)) {
            return "INSERT OR IGNORE INTO kt_purchases (uuid, effect_id) VALUES (?, ?)";
        }
        return "INSERT INTO kt_purchases (uuid, effect_id) VALUES (?, ?)";
    }
}
