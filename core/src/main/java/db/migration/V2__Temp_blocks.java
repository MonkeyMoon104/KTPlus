package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__Temp_blocks extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS kt_temp_blocks ("
                            + "world VARCHAR(64) NOT NULL, "
                            + "x INTEGER NOT NULL, "
                            + "y INTEGER NOT NULL, "
                            + "z INTEGER NOT NULL, "
                            + "material VARCHAR(64) NOT NULL, "
                            + "PRIMARY KEY (world, x, y, z))");
        }
    }
}
