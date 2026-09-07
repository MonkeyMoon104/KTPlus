package com.monkey.ktplus.hook.worldguard;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

public final class WorldGuardHook {
    public enum BlockChangeMode {
        RESPECT,
        BYPASS;

        public static BlockChangeMode fromConfig(@Nullable String raw, boolean respectBuildFlag) {
            if (raw != null && !raw.trim().isEmpty()) {
                String normalized = raw.trim().toUpperCase(Locale.ROOT);
                if ("BYPASS".equals(normalized) || "IGNORE".equals(normalized)) {
                    return BYPASS;
                }
                return RESPECT;
            }
            return respectBuildFlag ? RESPECT : BYPASS;
        }
    }

    private final boolean pluginPresent;
    private final boolean available;
    private final BlockChangeMode mode;
    private final String bypassPermission;
    private final @Nullable Object regionQuery;
    private final @Nullable Object buildFlag;
    private final @Nullable Method adaptLocation;
    private final @Nullable Method adaptPlayer;
    private final @Nullable Method testState;

    private WorldGuardHook(
            boolean pluginPresent,
            boolean available,
            BlockChangeMode mode,
            String bypassPermission,
            @Nullable Object regionQuery,
            @Nullable Object buildFlag,
            @Nullable Method adaptLocation,
            @Nullable Method adaptPlayer,
            @Nullable Method testState) {
        this.pluginPresent = pluginPresent;
        this.available = available;
        this.mode = mode;
        this.bypassPermission = bypassPermission;
        this.regionQuery = regionQuery;
        this.buildFlag = buildFlag;
        this.adaptLocation = adaptLocation;
        this.adaptPlayer = adaptPlayer;
        this.testState = testState;
    }

    public static WorldGuardHook create(BlockChangeMode mode, String bypassPermission, Logger logger) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(bypassPermission, "bypassPermission");
        Objects.requireNonNull(logger, "logger");
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !plugin.isEnabled()) {
            return absent(mode, bypassPermission);
        }
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuard.getClass().getMethod("getPlatform").invoke(worldGuard);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = container.getClass().getMethod("createQuery").invoke(container);

            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptLocation = bukkitAdapter.getMethod("adapt", Location.class);
            Method adaptPlayer = bukkitAdapter.getMethod("adapt", Player.class);

            Object buildFlag = Class.forName("com.sk89q.worldguard.protection.flags.Flags")
                    .getField("BUILD")
                    .get(null);

            Method testState = findTestState(query.getClass());
            if (testState == null) {
                logger.warning("[Hooks] WorldGuard RegionQuery.testState missing; RESPECT fail-closed.");
                return broken(mode, bypassPermission);
            }

            logger.info("[Hooks] WorldGuard hooked (block-changes=" + mode.name().toLowerCase(Locale.ROOT) + ").");
            return new WorldGuardHook(
                    true, true, mode, bypassPermission, query, buildFlag, adaptLocation, adaptPlayer, testState);
        } catch (ReflectiveOperationException | RuntimeException error) {
            logger.warning("[Hooks] WorldGuard hook failed: " + error.getMessage() + "; RESPECT fail-closed.");
            return broken(mode, bypassPermission);
        }
    }

    private static @Nullable Method findTestState(Class<?> queryClass) {
        for (Method method : queryClass.getMethods()) {
            if (!"testState".equals(method.getName())) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types.length >= 3) {
                return method;
            }
        }
        return null;
    }

    private static WorldGuardHook absent(BlockChangeMode mode, String bypassPermission) {
        return new WorldGuardHook(false, false, mode, bypassPermission, null, null, null, null, null);
    }

    private static WorldGuardHook broken(BlockChangeMode mode, String bypassPermission) {
        return new WorldGuardHook(true, false, mode, bypassPermission, null, null, null, null, null);
    }

    public boolean available() {
        return available;
    }

    public BlockChangeMode mode() {
        return mode;
    }

    public boolean allowsBlockChange(@Nullable Player player, Location location) {
        return allowsProtectedAction(player, location);
    }

    public boolean allowsEntityPlace(@Nullable Player player, Location location) {
        return allowsProtectedAction(player, location);
    }

    public boolean allowsAreaEffect(@Nullable Player player, Location location) {
        return allowsProtectedAction(player, location);
    }

    public boolean allowsProtectedAction(@Nullable Player player, Location location) {
        Objects.requireNonNull(location, "location");
        if (mode == BlockChangeMode.BYPASS) {
            return true;
        }
        if (!pluginPresent) {
            return true;
        }
        boolean bypassPermitted = player != null && player.hasPermission(bypassPermission);
        if (!available) {
            return evaluateRespect(false, bypassPermitted, false);
        }
        try {
            Object weLocation = adaptLocation.invoke(null, location);
            Object localPlayer = player == null ? null : adaptPlayer.invoke(null, player);
            Object allowed = testState.invoke(regionQuery, weLocation, localPlayer, buildFlag);
            boolean regionAllows = allowed instanceof Boolean && (Boolean) allowed;
            return evaluateRespect(true, bypassPermitted, regionAllows);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return evaluateRespect(true, bypassPermitted, false);
        }
    }

    static boolean evaluateRespect(
            boolean hookAvailable, boolean bypassPermitted, boolean regionAllowsBuild) {
        if (!hookAvailable) {
            return false;
        }
        if (bypassPermitted) {
            return true;
        }
        return regionAllowsBuild;
    }
}
