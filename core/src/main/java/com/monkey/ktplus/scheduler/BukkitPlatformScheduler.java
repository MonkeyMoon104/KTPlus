package com.monkey.ktplus.scheduler;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitPlatformScheduler implements PlatformScheduler {
    private final JavaPlugin plugin;

    public BukkitPlatformScheduler(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public ScheduledHandle run(Location location, Runnable action) {
        return new BukkitScheduledHandle(Bukkit.getScheduler().runTask(plugin, action));
    }

    @Override
    public ScheduledHandle runLater(Location location, Runnable action, long delayTicks) {
        return new BukkitScheduledHandle(Bukkit.getScheduler().runTaskLater(plugin, action, Math.max(0L, delayTicks)));
    }

    @Override
    public ScheduledHandle runTimer(Location location, Runnable action, long delayTicks, long periodTicks) {
        return new BukkitScheduledHandle(Bukkit.getScheduler().runTaskTimer(plugin, action, Math.max(0L, delayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public ScheduledHandle run(Entity entity, Runnable action) {
        Objects.requireNonNull(entity, "entity");
        return run(entity.getLocation(), action);
    }

    @Override
    public ScheduledHandle runLater(Entity entity, Runnable action, long delayTicks) {
        Objects.requireNonNull(entity, "entity");
        return runLater(entity.getLocation(), action, delayTicks);
    }

    @Override
    public ScheduledHandle runTimer(Entity entity, Runnable action, long delayTicks, long periodTicks) {
        Objects.requireNonNull(entity, "entity");
        return runTimer(entity.getLocation(), action, delayTicks, periodTicks);
    }

    @Override
    public ScheduledHandle runAsync(Runnable action) {
        return new BukkitScheduledHandle(Bukkit.getScheduler().runTaskAsynchronously(plugin, action));
    }

    @Override
    public ScheduledHandle runAsyncLater(Runnable action, long delayTicks) {
        return new BukkitScheduledHandle(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, action, Math.max(0L, delayTicks)));
    }

    @Override
    public ScheduledHandle runGlobal(Runnable action) {
        return new BukkitScheduledHandle(Bukkit.getScheduler().runTask(plugin, action));
    }

    @Override
    public ScheduledHandle runGlobalLater(Runnable action, long delayTicks) {
        return new BukkitScheduledHandle(Bukkit.getScheduler().runTaskLater(plugin, action, Math.max(0L, delayTicks)));
    }
}
