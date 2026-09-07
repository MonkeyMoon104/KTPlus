package com.monkey.ktplus.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PluginVersion {
    private PluginVersion() {}

    public static String normalize(String value) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() > 1 && (trimmed.charAt(0) == 'v' || trimmed.charAt(0) == 'V')) {
            trimmed = trimmed.substring(1);
        }
        int cut = indexOfBuildMetadata(trimmed);
        if (cut >= 0) {
            trimmed = trimmed.substring(0, cut);
        }
        return trimmed.trim();
    }

    public static int compare(String left, String right) {
        List<String> a = tokenize(normalize(left));
        List<String> b = tokenize(normalize(right));
        int size = Math.max(a.size(), b.size());
        for (int i = 0; i < size; i++) {
            String leftPart = i < a.size() ? a.get(i) : "0";
            String rightPart = i < b.size() ? b.get(i) : "0";
            int cmp = comparePart(leftPart, rightPart);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    private static int comparePart(String left, String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareToIgnoreCase(right);
    }

    private static List<String> tokenize(String version) {
        List<String> parts = new ArrayList<>();
        if (version.isEmpty()) {
            return parts;
        }
        StringBuilder current = new StringBuilder();
        boolean numeric = Character.isDigit(version.charAt(0));
        for (int i = 0; i < version.length(); i++) {
            char ch = version.charAt(i);
            if (ch == '.' || ch == '-' || ch == '_') {
                flush(parts, current);
                numeric = i + 1 < version.length() && Character.isDigit(version.charAt(i + 1));
                continue;
            }
            boolean digit = Character.isDigit(ch);
            if (!current.isEmpty() && digit != numeric) {
                flush(parts, current);
                numeric = digit;
            }
            current.append(ch);
        }
        flush(parts, current);
        return parts;
    }

    private static void flush(List<String> parts, StringBuilder current) {
        if (current.isEmpty()) {
            return;
        }
        parts.add(current.toString().toLowerCase(Locale.ROOT));
        current.setLength(0);
    }

    private static boolean isNumeric(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int indexOfBuildMetadata(String value) {
        int plus = value.indexOf('+');
        return plus >= 0 ? plus : -1;
    }
}
