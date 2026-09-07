package com.monkey.ktplus.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface PlatformScheduler {
    ScheduledHandle run(Location location, Runnable action);

    ScheduledHandle runLater(Location location, Runnable action, long delayTicks);

    ScheduledHandle runTimer(Location location, Runnable action, long delayTicks, long periodTicks);

    ScheduledHandle run(Entity entity, Runnable action);

    ScheduledHandle runLater(Entity entity, Runnable action, long delayTicks);

    ScheduledHandle runTimer(Entity entity, Runnable action, long delayTicks, long periodTicks);

    ScheduledHandle runAsync(Runnable action);

    ScheduledHandle runAsyncLater(Runnable action, long delayTicks);

    ScheduledHandle runGlobal(Runnable action);

    ScheduledHandle runGlobalLater(Runnable action, long delayTicks);
}
