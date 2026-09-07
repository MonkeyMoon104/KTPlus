package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V5__Review_claims extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS kt_review_claims ("
                            + "uuid VARCHAR(36) NOT NULL, "
                            + "platform VARCHAR(16) NOT NULL, "
                            + "account_key VARCHAR(64) NOT NULL, "
                            + "claimed_at BIGINT NOT NULL, "
                            + "PRIMARY KEY (uuid, platform))");
            try {
                statement.executeUpdate(
                        "CREATE UNIQUE INDEX IF NOT EXISTS kt_review_claims_account "
                                + "ON kt_review_claims (platform, account_key)");
            } catch (Exception ignored) {
                try {
                    statement.executeUpdate(
                            "CREATE UNIQUE INDEX kt_review_claims_account "
                                    + "ON kt_review_claims (platform, account_key)");
                } catch (Exception ignoredAgain) {
                    
                }
            }
        }
    }
}
