package com.monkey.ktplus.storage.migration;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MigrationLock {
    private final AtomicBoolean held = new AtomicBoolean(false);

    public boolean tryAcquire() {
        return held.compareAndSet(false, true);
    }

    public void release() {
        held.set(false);
    }

    public boolean isHeld() {
        return held.get();
    }
}
