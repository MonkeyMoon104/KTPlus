package com.monkey.ktplus.lib;

import com.monkey.ktplus.logging.KtPlusLogging;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class RuntimeLibraryBootstrap {
    private RuntimeLibraryBootstrap() {}

    public static List<LibraryLoadResult> install(JavaPlugin plugin) throws Exception {
        Objects.requireNonNull(plugin, "plugin");
        Logger logger = plugin.getLogger();
        List<LibraryRequest> requests = selectRequests();
        if (requests.isEmpty()) {
            KtPlusLogging.detail(logger, "Libs", "No custom LibraryLoader groups (Maven Central via plugin.yml)");
            return List.of();
        }
        KtPlusLogging.info(logger, "Libs", "Installing " + requests.size() + " non-Central library group(s)");
        LibraryLoader loader =
                new LibraryLoader(plugin.getDataFolder().toPath(), plugin.getClass().getClassLoader(), logger);
        return loader.loadAll(requests);
    }

    static List<LibraryRequest> selectRequests() {
        List<LibraryRequest> requests = new ArrayList<>();
        requests.add(request("packetevents", "PacketEvents", true));
        return requests;
    }

    private static LibraryRequest request(String id, String displayName, boolean optional) {
        return new LibraryRequest(
                id,
                displayName,
                "META-INF/ktplus/libs/" + id + ".properties",
                "ktplus.lib." + id,
                LibraryTrack.MODERN,
                optional);
    }
}
