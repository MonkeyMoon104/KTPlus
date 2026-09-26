package com.monkey.ktplus.api.bridge;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public final class ApiEvents {
    private ApiEvents() {}

    public static void call(Event event) {
        Objects.requireNonNull(event, "event");
        if (Bukkit.getServer() == null) {
            return;
        }
        Bukkit.getPluginManager().callEvent(event);
    }
}
