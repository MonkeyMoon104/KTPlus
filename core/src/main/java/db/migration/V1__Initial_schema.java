package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1__Initial_schema extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS kt_player_effects ("
                            + "uuid VARCHAR(36) PRIMARY KEY, "
                            + "effect_id VARCHAR(64) NOT NULL)");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS kt_killcoins ("
                            + "uuid VARCHAR(36) PRIMARY KEY, "
                            + "balance BIGINT NOT NULL)");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS kt_purchases ("
                            + "uuid VARCHAR(36) NOT NULL, "
                            + "effect_id VARCHAR(64) NOT NULL, "
                            + "PRIMARY KEY (uuid, effect_id))");
        }
    }
}
