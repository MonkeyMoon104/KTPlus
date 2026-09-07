package com.monkey.ktplus.task;

import com.monkey.ktplus.scheduler.ScheduledHandle;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TaskRegistry {
    private final ConcurrentHashMap<UUID, Set<ScheduledHandle>> tasksBySession = new ConcurrentHashMap<>();

    public ScheduledHandle track(TaskScope scope, ScheduledHandle handle) {
        tasksBySession.computeIfAbsent(scope.sessionId(), ignored -> ConcurrentHashMap.newKeySet()).add(handle);
        return handle;
    }

    public void cancelSession(UUID sessionId) {
        Set<ScheduledHandle> handles = tasksBySession.remove(sessionId);
        if (handles == null) {
            return;
        }
        for (ScheduledHandle handle : handles) {
            handle.cancel();
        }
    }

    public void forget(UUID sessionId, ScheduledHandle handle) {
        Set<ScheduledHandle> handles = tasksBySession.getOrDefault(sessionId, Collections.emptySet());
        handles.remove(handle);
    }

    public void cancelAll() {
        for (UUID sessionId : tasksBySession.keySet()) {
            cancelSession(sessionId);
        }
    }
}
