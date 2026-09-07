package com.monkey.ktplus.storage.migration.transfer;

import com.monkey.ktplus.storage.migration.MigrationRequest;
import java.util.ArrayList;
import java.util.List;

public final class MigrationTableCatalog {
    public static final TableSpec KILLCOINS = new TableSpec(
            "kt_killcoins",
            List.of("uuid", "balance"),
            List.of("uuid"),
            TableCriticality.CRITICAL,
            true);

    public static final TableSpec PURCHASES = new TableSpec(
            "kt_purchases",
            List.of("uuid", "effect_id"),
            List.of("uuid", "effect_id"),
            TableCriticality.CRITICAL,
            false);

    public static final TableSpec PLAYER_EFFECTS = new TableSpec(
            "kt_player_effects",
            List.of("uuid", "effect_id"),
            List.of("uuid"),
            TableCriticality.HIGH,
            false);

    public static final TableSpec REVIEW_CLAIMS = new TableSpec(
            "kt_review_claims",
            List.of("uuid", "platform", "account_key", "claimed_at"),
            List.of("uuid", "platform"),
            TableCriticality.HIGH,
            false);

    public static final TableSpec PENDING_INVENTORY = new TableSpec(
            "kt_pending_inventory",
            List.of("uuid", "payload", "created_at"),
            List.of("uuid"),
            TableCriticality.CONDITIONAL,
            false);

    public static final TableSpec TEMP_BLOCKS = new TableSpec(
            "kt_temp_blocks",
            List.of("world", "x", "y", "z", "material", "block_data"),
            List.of("world", "x", "y", "z"),
            TableCriticality.REGENERABLE,
            false);

    private MigrationTableCatalog() {}

    
    public static List<TableSpec> forSnapshot() {
        return List.of(KILLCOINS, PURCHASES, PLAYER_EFFECTS, REVIEW_CLAIMS, PENDING_INVENTORY, TEMP_BLOCKS);
    }

    
    public static List<TableSpec> forTransfer(MigrationRequest request) {
        List<TableSpec> tables = new ArrayList<TableSpec>();
        tables.add(KILLCOINS);
        tables.add(PURCHASES);
        tables.add(PLAYER_EFFECTS);
        tables.add(REVIEW_CLAIMS);
        tables.add(PENDING_INVENTORY);
        if (request.includeTempBlocks()) {
            tables.add(TEMP_BLOCKS);
        }
        return List.copyOf(tables);
    }

    public static List<TableSpec> economyPair() {
        return List.of(KILLCOINS, PURCHASES);
    }
}
