package com.monkey.ktplus.access.platform;

import com.monkey.ktplus.nms.NmsBridgeManager;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

public final class InvuiAccess {
    private static final String INVENTORY_ACCESS =
            "com.monkey.ktplus.libs.inventoryaccess.InventoryAccess";

    private InvuiAccess() {}

    public static boolean isRuntimePresent() {
        try {
            Class.forName("com.monkey.ktplus.libs.invui.gui.Gui", false, InvuiAccess.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void ensureBridgeReady(ClassLoader loader, @Nullable Logger logger) {
        if (!NmsBridgeManager.isInitialized()) {
            return;
        }
        Object bridge = NmsBridgeManager.get();
        try {
            Method method = bridge.getClass().getMethod("ensureInvuiReady", ClassLoader.class, Logger.class);
            method.invoke(bridge, loader, logger);
        } catch (ReflectiveOperationException ignored) {
            
        }
    }

    public static void warmRevision(ClassLoader loader, Object revision, @Nullable Logger logger) {
        String revisionPackage = readRevisionPackage(revision);
        if (revisionPackage == null || revisionPackage.isEmpty()) {
            return;
        }
        String implClass = "com.monkey.ktplus.libs.inventoryaccess." + revisionPackage + ".InventoryUtilsImpl";
        try {
            Class.forName(implClass, true, loader);
            Class<?> inventoryAccess = Class.forName(INVENTORY_ACCESS, true, loader);
            Method getInventoryUtils = inventoryAccess.getMethod("getInventoryUtils");
            getInventoryUtils.invoke(null);
            if (logger != null) {
                logger.info("[GUI] InventoryAccess warmed for " + revisionPackage);
            }
        } catch (Throwable failure) {
            if (logger != null) {
                logger.log(
                        Level.WARNING,
                        "[GUI] InventoryAccess warmup failed for " + revisionPackage + "; InvUI may fall back",
                        failure);
            }
        }
    }

    public static @Nullable String readRevisionPackage(@Nullable Object revision) {
        if (revision == null) {
            return null;
        }
        try {
            Object value = revision.getClass().getMethod("getPackageName").invoke(revision);
            return value instanceof String ? (String) value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
