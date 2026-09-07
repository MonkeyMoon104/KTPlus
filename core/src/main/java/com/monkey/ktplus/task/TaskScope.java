package com.monkey.ktplus.task;

import java.util.UUID;

public final class TaskScope {
    private final UUID sessionId;
    private final UUID playerId;
    private final String worldName;

    public TaskScope(UUID sessionId, UUID playerId, String worldName) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.worldName = worldName;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID playerId() {
        return playerId;
    }

    public String worldName() {
        return worldName;
    }
}
