package com.monkey.ktplus.bridge.compat;

import com.monkey.ktplus.bridge.DefaultVersionBridge;
import com.monkey.ktplus.bridge.VersionBridge;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class CompatBridgeRegistry {
    private static final VersionBridge V1_21 = new CompatBridge_v1_21();
    private static final VersionBridge V26_2 = new CompatBridge_v26_2();
    private static final Map<String, VersionBridge> BY_MODULE;

    static {
        Map<String, VersionBridge> map = new HashMap<>();
        map.put("v1_21_1", V1_21);
        map.put("v1_21_3", V1_21);
        map.put("v1_21_4", V1_21);
        map.put("v1_21_5", V1_21);
        map.put("v1_21_6", V1_21);
        map.put("v1_21_7", V1_21);
        map.put("v1_21_8", V1_21);
        map.put("v1_21_9", V1_21);
        map.put("v1_21_10", V1_21);
        map.put("v1_21_11", V1_21);
        map.put("v26_1", V26_2);
        map.put("v26_2", V26_2);
        BY_MODULE = Collections.unmodifiableMap(map);
    }

    private CompatBridgeRegistry() {}

    public static VersionBridge forModule(String module) {
        return BY_MODULE.getOrDefault(Objects.requireNonNull(module, "module"), new DefaultVersionBridge());
    }

    public static @Nullable String moduleForVersion(String version) {
        if (version == null || version.isEmpty() || "unknown".equalsIgnoreCase(version)) {
            return null;
        }
        String normalized = version.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("26.")) {
            return normalized.startsWith("26.1") ? "v26_1" : "v26_2";
        }
        if (!normalized.startsWith("1.21")) {
            return null;
        }
        return switch (normalized) {
            case "1.21", "1.21.1" -> "v1_21_1";
            case "1.21.3" -> "v1_21_3";
            case "1.21.4" -> "v1_21_4";
            case "1.21.5" -> "v1_21_5";
            case "1.21.6" -> "v1_21_6";
            case "1.21.7" -> "v1_21_7";
            case "1.21.8" -> "v1_21_8";
            case "1.21.9" -> "v1_21_9";
            case "1.21.10" -> "v1_21_10";
            case "1.21.11" -> "v1_21_11";
            default -> null;
        };
    }
}
