package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__Pending_inventory extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS kt_pending_inventory ("
                            + "uuid VARCHAR(36) PRIMARY KEY, "
                            + "payload TEXT NOT NULL, "
                            + "created_at BIGINT NOT NULL)");
        }
    }
}
