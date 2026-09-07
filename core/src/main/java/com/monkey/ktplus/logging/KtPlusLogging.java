package com.monkey.ktplus.logging;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KtPlusLogging {
    static final String RESET = "\u001B[0m";
    static final String BOLD = "\u001B[1m";
    static final String DIM = "\u001B[2m";

    static final String WHITE = "\u001B[38;5;255m";
    static final String CREAM = "\u001B[38;5;230m";
    static final String ORANGE = "\u001B[38;5;208m";
    static final String AMBER = "\u001B[38;5;214m";
    static final String GOLD = "\u001B[38;5;178m";
    static final String TEAL = "\u001B[38;5;80m";
    static final String MINT = "\u001B[38;5;121m";
    static final String INDIGO = "\u001B[38;5;105m";
    static final String ROSE = "\u001B[38;5;211m";
    static final String CORAL = "\u001B[38;5;209m";
    static final String SLATE = "\u001B[38;5;250m";
    static final String RED = "\u001B[38;5;203m";
    static final String LIME = "\u001B[38;5;154m";

    static final int SECTION_WIDTH = 78;

    private KtPlusLogging() {}

    public static void brandArt(Logger logger, String... lines) {
        separator(logger, RED, '=');
        for (String line : lines) {
            logger.info(BOLD + ORANGE + consoleSafe(line) + RESET);
        }
        separator(logger, RED, '=');
    }

    public static void banner(Logger logger, String module, String... lines) {
        String accent = moduleColor(module);
        separator(logger, accent, '=');
        for (String line : lines) {
            logger.info(format(module, accent, fit(line, SECTION_WIDTH), CREAM));
        }
        separator(logger, accent, '=');
    }

    public static void section(Logger logger, String module, String title) {
        String accent = moduleColor(module);
        separator(logger, accent, '-');
        logger.info(format(module, accent, fit(title, SECTION_WIDTH), CREAM));
        separator(logger, accent, '-');
    }

    public static void info(Logger logger, String module, String message) {
        emit(logger, Level.INFO, module, CREAM, message);
    }

    public static void detail(Logger logger, String module, String message) {
        emit(logger, Level.INFO, module, SLATE, "-> " + message);
    }

    public static void success(Logger logger, String module, String message) {
        emit(logger, Level.INFO, module, LIME, "+ " + message);
    }

    public static void warn(Logger logger, String module, String message) {
        emit(logger, Level.WARNING, module, AMBER, "! " + message);
    }

    public static void error(Logger logger, String module, String message, Throwable error) {
        logger.log(Level.SEVERE, format(module, RED, "x " + message, RED), error);
    }

    public static void infoAccent(Logger logger, String module, String accentModule, String message) {
        emit(logger, Level.INFO, module, accentModule, CREAM, message);
    }

    public static void detailAccent(Logger logger, String module, String accentModule, String message) {
        emit(logger, Level.INFO, module, accentModule, SLATE, "-> " + message);
    }

    public static void successAccent(Logger logger, String module, String accentModule, String message) {
        emit(logger, Level.INFO, module, accentModule, LIME, "+ " + message);
    }

    public static void warnAccent(Logger logger, String module, String accentModule, String message) {
        emit(logger, Level.WARNING, module, accentModule, AMBER, "! " + message);
    }

    public static String formatDuration(long nanos) {
        double millis = nanos / 1_000_000.0D;
        if (millis < 1000.0D) {
            return String.format(Locale.ROOT, "%.2fms", millis);
        }
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0D);
    }

    public static String fit(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        if (max <= 3) {
            return text.substring(0, Math.max(0, max));
        }
        return text.substring(0, max - 3) + "...";
    }

    public static String consoleSafe(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return message.replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2011', '-')
                .replace('\u2212', '-')
                .replace("\u2192", "->")
                .replace("\u2190", "<-")
                .replace('\u2026', '.');
    }

    private static void separator(Logger logger, String color, char symbol) {
        logger.info(BOLD + color + repeatChar(symbol, SECTION_WIDTH) + RESET);
    }

    private static void emit(Logger logger, Level level, String module, String messageColor, String message) {
        emit(logger, level, module, module, messageColor, message);
    }

    private static void emit(
            Logger logger, Level level, String module, String accentModule, String messageColor, String message) {
        String line = format(module, moduleColor(accentModule), message, messageColor);
        if (Level.INFO.equals(level)) {
            logger.info(line);
        } else if (Level.WARNING.equals(level)) {
            logger.warning(line);
        } else {
            logger.log(level, line);
        }
    }

    private static String format(String module, String accentColor, String message, String messageColor) {
        return BOLD + accentColor + "[" + module.toUpperCase(Locale.ROOT) + "]" + RESET
                + " " + messageColor + consoleSafe(message) + RESET;
    }

    private static String moduleColor(String module) {
        return switch (module) {
            case "Boot" -> AMBER;
            case "Config" -> TEAL;
            case "Platform" -> INDIGO;
            case "NMS" -> GOLD;
            case "Storage" -> CORAL;
            case "Economy", "Services" -> MINT;
            case "Effects", "Effect" -> ORANGE;
            case "Category", "Categories" -> AMBER;
            case "Hooks", "Hook" -> ROSE;
            case "Gui", "GUI" -> INDIGO;
            case "Commands", "Listeners" -> TEAL;
            case "Metrics", "Update", "Libs" -> GOLD;
            case "Warn" -> AMBER;
            case "Error" -> RED;
            case "Phase01" -> TEAL;
            case "Phase02" -> INDIGO;
            case "Phase03" -> CORAL;
            case "Phase04" -> MINT;
            case "Phase05" -> ORANGE;
            case "Phase06" -> ROSE;
            case "Phase07" -> GOLD;
            case "Phase08" -> LIME;
            case "Phase09" -> RED;
            default -> CREAM;
        };
    }

    private static String repeatChar(char symbol, int count) {
        return String.valueOf(symbol).repeat(Math.max(0, count));
    }
}
