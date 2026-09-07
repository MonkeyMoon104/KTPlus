package com.monkey.ktplus.nms;

import com.monkey.ktplus.access.runtime.MinecraftVersionAccess;
import com.monkey.ktplus.bridge.IPlatformNmsBridge;
import com.monkey.ktplus.logging.KtPlusLogging;
import java.util.Locale;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

public final class NmsBridgeManager {
    private static @Nullable IPlatformNmsBridge instance;
    private static final String SUPPORTED_VERSIONS = "1.21.x, 26.1.x, 26.2.x";

    private NmsBridgeManager() {}

    public static void init(Logger logger) {
        String version = MinecraftVersionAccess.minecraftVersion();
        String className = resolveBridgeClassName(version);
        if (className == null) {
            KtPlusLogging.warn(
                    logger,
                    "NMS",
                    "Unsupported Minecraft version -> " + version + " | supported=" + SUPPORTED_VERSIONS);
            instance = null;
            return;
        }
        KtPlusLogging.detail(logger, "NMS", "Selected -> " + className + " | MC=" + version);

        try {
            Class<?> clazz = Class.forName(className);
            instance = (IPlatformNmsBridge) clazz.getDeclaredConstructor().newInstance();
            KtPlusLogging.success(logger, "NMS", "Bridge ready -> " + instance.getClass().getSimpleName());
        } catch (ClassNotFoundException failure) {
            throw new RuntimeException("[KTPlus] Bridge class not found: " + className, failure);
        } catch (ReflectiveOperationException failure) {
            throw new RuntimeException("[KTPlus] Failed to load NMS bridge", failure);
        }
    }

    public static String getSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static IPlatformNmsBridge get() {
        if (instance == null) {
            throw new RuntimeException("[KTPlus] NmsBridgeManager is not initialized");
        }
        return instance;
    }

    public static com.monkey.ktplus.bridge.VersionBridge compat() {
        return get().compat();
    }

    public static @Nullable String resolveBridgeClassName(String version) {
        if (version == null || version.isEmpty() || "unknown".equalsIgnoreCase(version)) {
            return null;
        }
        String normalized = version.toLowerCase(Locale.ROOT).trim();

        String bridge = resolve121Bridge(normalized);
        if (bridge != null) {
            return bridge;
        }
        return resolveV26Bridge(normalized);
    }

    private static String bridgeClass(String module) {
        return "com.monkey.ktplus.nms.NMSBridge_" + module;
    }

    private static @Nullable String resolve121Bridge(String version) {
        return switch (version) {
            case "1.21", "1.21.1" -> bridgeClass("v1_21_1");
            case "1.21.3" -> bridgeClass("v1_21_3");
            case "1.21.4" -> bridgeClass("v1_21_4");
            case "1.21.5" -> bridgeClass("v1_21_5");
            case "1.21.6" -> bridgeClass("v1_21_6");
            case "1.21.7" -> bridgeClass("v1_21_7");
            case "1.21.8" -> bridgeClass("v1_21_8");
            case "1.21.9" -> bridgeClass("v1_21_9");
            case "1.21.10" -> bridgeClass("v1_21_10");
            case "1.21.11" -> bridgeClass("v1_21_11");
            default -> null;
        };
    }

    private static @Nullable String resolveV26Bridge(String version) {
        if ("26.1".equals(version) || version.startsWith("26.1.")) {
            return bridgeClass("v26_1");
        }
        if ("26.2".equals(version) || version.startsWith("26.2.")) {
            return bridgeClass("v26_2");
        }
        return null;
    }
}
