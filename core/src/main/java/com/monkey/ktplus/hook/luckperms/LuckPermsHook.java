package com.monkey.ktplus.hook.luckperms;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

public final class LuckPermsHook {
    private final boolean available;
    private final boolean grantOnPurchase;
    private final @Nullable Object api;
    private final @Nullable Method modifyUser;
    private final @Nullable Method nodeBuilder;
    private final @Nullable Method nodeBuild;
    private final @Nullable Method userData;
    private final @Nullable Method dataAdd;

    private LuckPermsHook(
            boolean available,
            boolean grantOnPurchase,
            @Nullable Object api,
            @Nullable Method modifyUser,
            @Nullable Method nodeBuilder,
            @Nullable Method nodeBuild,
            @Nullable Method userData,
            @Nullable Method dataAdd) {
        this.available = available;
        this.grantOnPurchase = grantOnPurchase;
        this.api = api;
        this.modifyUser = modifyUser;
        this.nodeBuilder = nodeBuilder;
        this.nodeBuild = nodeBuild;
        this.userData = userData;
        this.dataAdd = dataAdd;
    }

    public static LuckPermsHook create(boolean grantOnPurchase, Logger logger) {
        Objects.requireNonNull(logger, "logger");
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (plugin == null || !plugin.isEnabled()) {
            return disabled(grantOnPurchase);
        }
        try {
            Object api = Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Method modifyUser = userManager.getClass().getMethod("modifyUser", UUID.class, Consumer.class);

            Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
            Method nodeBuilder = nodeClass.getMethod("builder", String.class);
            Object sampleBuilder = nodeBuilder.invoke(null, "ktplus.sample.use");
            Method nodeBuild = sampleBuilder.getClass().getMethod("build");

            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            Method userData = userClass.getMethod("data");
            Method dataAdd = Class.forName("net.luckperms.api.model.data.NodeMap")
                    .getMethod("add", Class.forName("net.luckperms.api.node.Node"));

            logger.info("[Hooks] LuckPerms hooked (grant-on-purchase=" + grantOnPurchase + ").");
            return new LuckPermsHook(
                    true, grantOnPurchase, api, modifyUser, nodeBuilder, nodeBuild, userData, dataAdd);
        } catch (ReflectiveOperationException | RuntimeException error) {
            logger.warning("[Hooks] LuckPerms hook failed: " + error.getMessage());
            return disabled(grantOnPurchase);
        }
    }

    private static LuckPermsHook disabled(boolean grantOnPurchase) {
        return new LuckPermsHook(false, grantOnPurchase, null, null, null, null, null, null);
    }

    public boolean available() {
        return available;
    }

    public void grantEffectPermission(UUID uuid, String permissionNode) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(permissionNode, "permissionNode");
        if (!available || !grantOnPurchase) {
            return;
        }
        try {
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Consumer<Object> consumer = user -> {
                try {
                    Object builder = nodeBuilder.invoke(null, permissionNode);
                    Object node = nodeBuild.invoke(builder);
                    Object data = userData.invoke(user);
                    dataAdd.invoke(data, node);
                } catch (ReflectiveOperationException ignored) {
                }
            };
            modifyUser.invoke(userManager, uuid, consumer);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
