package com.monkey.ktplus.scheduler;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaPlatformScheduler implements PlatformScheduler {
    private final JavaPlugin plugin;
    private final PlatformScheduler fallback;

    public FoliaPlatformScheduler(JavaPlugin plugin, PlatformScheduler fallback) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public ScheduledHandle run(Location location, Runnable action) {
        return invokeRegion("run", location, action, 0L, 0L);
    }

    @Override
    public ScheduledHandle runLater(Location location, Runnable action, long delayTicks) {
        return invokeRegion("runDelayed", location, action, Math.max(1L, delayTicks), 0L);
    }

    @Override
    public ScheduledHandle runTimer(Location location, Runnable action, long delayTicks, long periodTicks) {
        return invokeRegion("runAtFixedRate", location, action, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    }

    @Override
    public ScheduledHandle run(Entity entity, Runnable action) {
        return invokeEntity("run", entity, action, 0L, 0L);
    }

    @Override
    public ScheduledHandle runLater(Entity entity, Runnable action, long delayTicks) {
        return invokeEntity("runDelayed", entity, action, Math.max(1L, delayTicks), 0L);
    }

    @Override
    public ScheduledHandle runTimer(Entity entity, Runnable action, long delayTicks, long periodTicks) {
        return invokeEntity("runAtFixedRate", entity, action, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    }

    @Override
    public ScheduledHandle runAsync(Runnable action) {
        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method method = scheduler.getClass().getMethod("runNow", org.bukkit.plugin.Plugin.class, Consumer.class);
            Object task = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> action.run());
            return new ReflectiveScheduledHandle(task);
        } catch (ReflectiveOperationException ex) {
            return fallback.runAsync(action);
        }
    }

    @Override
    public ScheduledHandle runAsyncLater(Runnable action, long delayTicks) {
        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method method = scheduler.getClass().getMethod(
                    "runDelayed",
                    org.bukkit.plugin.Plugin.class,
                    Consumer.class,
                    long.class,
                    java.util.concurrent.TimeUnit.class);
            Object task = method.invoke(
                    scheduler,
                    plugin,
                    (Consumer<Object>) ignored -> action.run(),
                    Math.max(1L, delayTicks) * 50L,
                    java.util.concurrent.TimeUnit.MILLISECONDS);
            return new ReflectiveScheduledHandle(task);
        } catch (ReflectiveOperationException ex) {
            return fallback.runAsyncLater(action, delayTicks);
        }
    }

    @Override
    public ScheduledHandle runGlobal(Runnable action) {
        return invokeGlobal("run", action, 0L);
    }

    @Override
    public ScheduledHandle runGlobalLater(Runnable action, long delayTicks) {
        return invokeGlobal("runDelayed", action, Math.max(1L, delayTicks));
    }

    private ScheduledHandle invokeGlobal(String methodName, Runnable action, long delayTicks) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method method;
            Object task;
            if ("run".equals(methodName)) {
                method = scheduler.getClass().getMethod(
                        methodName, org.bukkit.plugin.Plugin.class, Consumer.class);
                task = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> action.run());
            } else {
                method = scheduler.getClass().getMethod(
                        methodName, org.bukkit.plugin.Plugin.class, Consumer.class, long.class);
                task = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> action.run(), delayTicks);
            }
            return new ReflectiveScheduledHandle(task);
        } catch (ReflectiveOperationException ex) {
            if ("runDelayed".equals(methodName)) {
                return fallback.runGlobalLater(action, delayTicks);
            }
            return fallback.runGlobal(action);
        }
    }

    private ScheduledHandle invokeEntity(String methodName, Entity entity, Runnable action, long first, long second) {
        Objects.requireNonNull(entity, "entity");
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            Method method;
            Object task;
            if ("run".equals(methodName)) {
                method = scheduler.getClass().getMethod(
                        methodName,
                        org.bukkit.plugin.Plugin.class,
                        Consumer.class,
                        Runnable.class);
                task = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> action.run(), (Runnable) null);
            } else if ("runDelayed".equals(methodName)) {
                method = scheduler.getClass().getMethod(
                        methodName,
                        org.bukkit.plugin.Plugin.class,
                        Consumer.class,
                        Runnable.class,
                        long.class);
                task = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> action.run(), (Runnable) null, first);
            } else {
                method = scheduler.getClass().getMethod(
                        methodName,
                        org.bukkit.plugin.Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class);
                task = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> action.run(), first, second);
            }
            return new ReflectiveScheduledHandle(task);
        } catch (ReflectiveOperationException ex) {
            if ("runAtFixedRate".equals(methodName)) {
                return fallback.runTimer(entity, action, first, second);
            }
            if ("runDelayed".equals(methodName)) {
                return fallback.runLater(entity, action, first);
            }
            return fallback.run(entity, action);
        }
    }

    private ScheduledHandle invokeRegion(String methodName, Location location, Runnable action, long first, long second) {
        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Method method;
            Object task;
            if ("run".equals(methodName)) {
                method = scheduler.getClass().getMethod(
                        methodName, org.bukkit.plugin.Plugin.class, Location.class, Consumer.class);
                task = method.invoke(scheduler, plugin, location, (Consumer<Object>) ignored -> action.run());
            } else if ("runDelayed".equals(methodName)) {
                method = scheduler.getClass().getMethod(
                        methodName, org.bukkit.plugin.Plugin.class, Location.class, Consumer.class, long.class);
                task = method.invoke(scheduler, plugin, location, (Consumer<Object>) ignored -> action.run(), first);
            } else {
                method = scheduler.getClass().getMethod(
                        methodName, org.bukkit.plugin.Plugin.class, Location.class, Consumer.class, long.class, long.class);
                task = method.invoke(scheduler, plugin, location, (Consumer<Object>) ignored -> action.run(), first, second);
            }
            return new ReflectiveScheduledHandle(task);
        } catch (ReflectiveOperationException ex) {
            if ("runAtFixedRate".equals(methodName)) {
                return fallback.runTimer(location, action, first, second);
            }
            if ("runDelayed".equals(methodName)) {
                return fallback.runLater(location, action, first);
            }
            return fallback.run(location, action);
        }
    }
}
