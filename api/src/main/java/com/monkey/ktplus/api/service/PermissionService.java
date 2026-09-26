package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.Effect;
import org.bukkit.entity.Player;

/**
 * Effect permission checks and admin bypass.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface PermissionService {
    /**
     * Whether the player bypasses ownership/economy gates as an admin.
     *
     * @param player player to check
     * @return {@code true} if admin bypass applies
     */
    boolean isAdminBypass(Player player);

    /**
     * Whether the player has permission to use the effect.
     *
     * @param player player to check
     * @param effect effect
     * @return {@code true} if permitted
     */
    boolean canUse(Player player, Effect effect);

    /**
     * Whether the player has permission to use the effect id.
     *
     * @param player player to check
     * @param effectId effect id
     * @return {@code true} if permitted
     */
    boolean canUse(Player player, String effectId);

    /**
     * Builds the standard permission node for an effect id ({@code ktplus.<id>.use}).
     *
     * @param effectId effect id
     * @return permission node
     */
    String permissionNode(String effectId);
}
