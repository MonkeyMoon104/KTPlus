package com.monkey.ktplus.scheduler;

import org.bukkit.scheduler.BukkitTask;

public final class BukkitScheduledHandle implements ScheduledHandle {
    private final BukkitTask task;
    private volatile boolean cancelled;

    public BukkitScheduledHandle(BukkitTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        cancelled = true;
        task.cancel();
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }
}
