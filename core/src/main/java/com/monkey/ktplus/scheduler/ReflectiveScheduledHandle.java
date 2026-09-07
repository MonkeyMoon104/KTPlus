package com.monkey.ktplus.scheduler;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReflectiveScheduledHandle implements ScheduledHandle {
    private final Object task;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ReflectiveScheduledHandle(Object task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            try {
                Method cancel = task.getClass().getMethod("cancel");
                cancel.invoke(task);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    @Override
    public boolean cancelled() {
        return cancelled.get();
    }
}
