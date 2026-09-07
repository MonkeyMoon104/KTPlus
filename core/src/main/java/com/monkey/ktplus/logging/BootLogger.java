package com.monkey.ktplus.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class BootLogger {
    private static final String[] TITLE = {
        "  _  _______     _   ",
        " | |/ /_   _|  _| |_ ",
        " | ' <  | |   |_   _|",
        " |_|\\_\\ |_|     |_|  ",
        "                     "
    };

    private static final String[] SUBTITLE = {
        "   ____     __                        __",
        "  / __/__  / /  ___ ____  _______ ___/ /",
        " / _// _ \\/ _ \\/ _ `/ _ \\/ __/ -_) _  / ",
        "/___/_//_/_//_/\\_,_/_//_/\\__/\\__/\\_,_/ "
    };

    private final JavaPlugin plugin;
    private final Logger logger;
    private final long startedAtNanos;
    private final List<PhaseSnapshot> phases = new ArrayList<>();
    private @Nullable PhaseSnapshot currentPhase;

    public BootLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.startedAtNanos = System.nanoTime();

        KtPlusLogging.brandArt(logger, TITLE);
        KtPlusLogging.brandArt(logger, SUBTITLE);
        KtPlusLogging.banner(
                logger,
                "Boot",
                "KTPlus Boot Start",
                "Plugin: " + plugin.getPluginMeta().getName() + " v" + plugin.getPluginMeta().getVersion(),
                "Authors: " + String.join(", ", plugin.getPluginMeta().getAuthors()),
                "Server: " + Bukkit.getName() + " | MC: " + Bukkit.getMinecraftVersion(),
                "Java: " + System.getProperty("java.version")
                        + " | Threads: "
                        + Thread.activeCount()
                        + " | "
                        + memorySnapshot());
    }

    public void beginPhase(int index, String category, String title) {
        currentPhase = new PhaseSnapshot(index, System.nanoTime());
        String phaseModule = phaseModule(index);
        KtPlusLogging.section(
                logger,
                phaseModule,
                "PHASE " + String.format(Locale.ROOT, "%02d", index) + " - " + title);
        KtPlusLogging.infoAccent(logger, category, phaseModule, "Step started");
    }

    public void completePhase(String summary) {
        if (currentPhase == null) {
            return;
        }
        currentPhase.complete(System.nanoTime());
        phases.add(currentPhase);
        KtPlusLogging.detailAccent(
                logger,
                "Boot",
                activeModule(),
                "Done in " + KtPlusLogging.formatDuration(currentPhase.durationNanos()) + " | " + summary);
        currentPhase = null;
    }

    public void info(String category, String message) {
        KtPlusLogging.infoAccent(logger, category, activeModule(), message);
    }

    public void detail(String category, String message) {
        KtPlusLogging.detailAccent(logger, category, activeModule(), message);
    }

    public void success(String category, String message) {
        KtPlusLogging.successAccent(logger, category, activeModule(), message);
    }

    public void warn(String category, String message) {
        KtPlusLogging.warnAccent(logger, category, activeModule(), message);
    }

    public void complete() {
        long total = System.nanoTime() - startedAtNanos;
        KtPlusLogging.banner(
                logger,
                "Boot",
                "KTPlus Boot Complete",
                "Plugin: " + plugin.getPluginMeta().getName() + " enabled",
                "Elapsed: " + KtPlusLogging.formatDuration(total),
                "Phases: " + phases.size() + " completed",
                memorySnapshot());
    }

    public void fail(Throwable error) {
        long elapsed = System.nanoTime() - startedAtNanos;
        KtPlusLogging.banner(
                logger,
                "Boot",
                "KTPlus Boot Failed",
                "Plugin: " + plugin.getPluginMeta().getName(),
                "Elapsed: " + KtPlusLogging.formatDuration(elapsed));
        KtPlusLogging.error(logger, "Error", "Startup exception -> " + safeMessage(error), error);
    }

    public Logger logger() {
        return logger;
    }

    private String activeModule() {
        return currentPhase == null ? "Boot" : phaseModule(currentPhase.index);
    }

    private static String phaseModule(int index) {
        return switch (index) {
            case 1 -> "Phase01";
            case 2 -> "Phase02";
            case 3 -> "Phase03";
            case 4 -> "Phase04";
            case 5 -> "Phase05";
            case 6 -> "Phase06";
            case 7 -> "Phase07";
            case 8 -> "Phase08";
            case 9 -> "Phase09";
            default -> "Boot";
        };
    }

    private static String memorySnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long mb = 1024L * 1024L;
        long used = (runtime.totalMemory() - runtime.freeMemory()) / mb;
        long total = runtime.totalMemory() / mb;
        long max = runtime.maxMemory() / mb;
        return "Mem: " + used + "MB/" + total + "MB (max " + max + "MB)";
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }

    private static final class PhaseSnapshot {
        private final int index;
        private final long startedAtNanos;
        private long completedAtNanos;

        private PhaseSnapshot(int index, long startedAtNanos) {
            this.index = index;
            this.startedAtNanos = startedAtNanos;
        }

        private void complete(long completedAtNanos) {
            this.completedAtNanos = completedAtNanos;
        }

        private long durationNanos() {
            return completedAtNanos - startedAtNanos;
        }
    }
}
