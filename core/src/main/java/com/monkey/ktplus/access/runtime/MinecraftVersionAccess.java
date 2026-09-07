package com.monkey.ktplus.access.runtime;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.jspecify.annotations.Nullable;

public final class MinecraftVersionAccess {

    private static final Pattern BUKKIT_VERSION = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern CRAFT_PACKAGE = Pattern.compile("v(\\d+)_(\\d+)_R\\d+");

    private static final String CACHED = detect();

    private MinecraftVersionAccess() {}

    public static String minecraftVersion() {
        return CACHED;
    }

    public static boolean isAtLeast(int major, int minor) {
        int[] parts = parse(CACHED);
        if (parts == null) {
            return false;
        }
        if (parts[0] != major) {
            return parts[0] > major;
        }
        return parts[1] >= minor;
    }

    private static String detect() {
        try {
            String fromPaper = invokeMinecraftVersion();
            if (fromPaper != null && !fromPaper.isEmpty()) {
                return normalize(fromPaper);
            }
            String fromBukkit = parseBukkitVersion(safeBukkitVersion());
            if (fromBukkit != null) {
                return fromBukkit;
            }
            String fromPackage = parseCraftPackage();
            if (fromPackage != null) {
                return fromPackage;
            }
        } catch (Throwable ignored) {
            
        }
        return "unknown";
    }

    private static @Nullable String safeBukkitVersion() {
        try {
            return Bukkit.getBukkitVersion();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable String invokeMinecraftVersion() {
        try {
            Object value = Bukkit.class.getMethod("getMinecraftVersion").invoke(null);
            if (value instanceof String) {
                String text = (String) value;
                String trimmed = text.trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private static @Nullable String parseBukkitVersion(@Nullable String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            return null;
        }
        Matcher matcher = BUKKIT_VERSION.matcher(bukkitVersion);
        if (!matcher.find()) {
            return null;
        }
        String major = matcher.group(1);
        String minor = matcher.group(2);
        String patch = matcher.group(3);
        if (patch == null || patch.isEmpty()) {
            return major + "." + minor;
        }
        return major + "." + minor + "." + patch;
    }

    private static @Nullable String parseCraftPackage() {
        try {
            if (Bukkit.getServer() == null) {
                return null;
            }
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            int lastDot = packageName.lastIndexOf('.');
            if (lastDot < 0 || lastDot + 1 >= packageName.length()) {
                return null;
            }
            String revision = packageName.substring(lastDot + 1);
            Matcher matcher = CRAFT_PACKAGE.matcher(revision);
            if (!matcher.matches()) {
                return null;
            }
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));

            if (major == 1 && minor == 7) {
                return "1.7.10";
            }
            if (major == 1 && minor == 8) {
                return "1.8.8";
            }
            if (major == 1 && minor == 9) {
                return "1.9.4";
            }
            if (major == 1 && minor == 10) {
                return "1.10.2";
            }
            if (major == 1 && minor == 11) {
                return "1.11.2";
            }
            if (major == 1 && minor == 12) {
                return "1.12.2";
            }
            if (major == 1 && minor == 13) {
                return "1.13.2";
            }
            if (major == 1 && minor == 14) {
                return "1.14.4";
            }
            if (major == 1 && minor == 15) {
                return "1.15.2";
            }
            if (major == 1 && minor == 16) {
                return "1.16.5";
            }
            return "1." + minor;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String normalize(String version) {
        String trimmed = version.trim();
        Matcher matcher = BUKKIT_VERSION.matcher(trimmed);
        if (matcher.find()) {
            String major = matcher.group(1);
            String minor = matcher.group(2);
            String patch = matcher.group(3);
            if (patch == null || patch.isEmpty()) {
                return major + "." + minor;
            }
            return major + "." + minor + "." + patch;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static int @Nullable [] parse(String version) {
        Matcher matcher = BUKKIT_VERSION.matcher(version);
        if (!matcher.find()) {
            return null;
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new int[] {major, minor, patch};
    }
}
