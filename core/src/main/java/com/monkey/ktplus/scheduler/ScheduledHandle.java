package com.monkey.ktplus.scheduler;

public interface ScheduledHandle {
    void cancel();

    boolean cancelled();
}
