package com.monkey.ktplus.storage.driver;

import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class SqlDriverInstaller {
    private SqlDriverInstaller() {}

    public static @Nullable String install(JavaPlugin plugin, String dialect) throws Exception {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(dialect, "dialect");
        SqlDriverCatalog.DriverEntry entry = SqlDriverCatalog.entryFor(dialect);
        if (entry == null) {
            return null;
        }
        Logger logger = plugin.getLogger();
        try {
            Class.forName(entry.driverClass(), true, plugin.getClass().getClassLoader());
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException(
                    "SQL driver missing for "
                            + entry.displayName()
                            + " ("
                            + entry.driverClass()
                            + "). Ensure it is listed under libraries in plugin.yml.",
                    error);
        }
        logger.info("[Storage] Using Spigot-loaded driver " + entry.driverClass());
        return entry.driverClass();
    }
}
