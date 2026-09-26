package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.RandomEventService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class BridgeRandomEventService implements RandomEventService {
    private final com.monkey.ktplus.event.random.RandomEventService randomEvents;
    private volatile com.monkey.ktplus.config.ConfigSnapshot config;

    public BridgeRandomEventService(
            com.monkey.ktplus.event.random.RandomEventService randomEvents,
            com.monkey.ktplus.config.ConfigSnapshot config) {
        this.randomEvents = Objects.requireNonNull(randomEvents, "randomEvents");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void reload(com.monkey.ktplus.config.ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean enabled() {
        return config.events().getBoolean("enabled", true);
    }

    @Override
    public void tryTrigger(Player killer, Location location) {
        randomEvents.tryTrigger(killer, location);
    }
}
