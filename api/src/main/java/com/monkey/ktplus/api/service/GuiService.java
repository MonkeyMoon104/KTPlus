package com.monkey.ktplus.api.service;

import org.bukkit.entity.Player;

/**
 * Opens and tracks the KTPlus effect selection GUI.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface GuiService {
    /**
     * Opens the effect GUI for the player.
     *
     * <p><b>Side effects:</b> may close other inventories and apply inventory guards.
     *
     * @param player target player
     */
    void open(Player player);

    /**
     * Closes the KTPlus GUI if it is open for the player.
     *
     * @param player target player
     */
    void close(Player player);

    /**
     * @param player target player
     * @return {@code true} if the KTPlus effect GUI is currently open
     */
    boolean isOpen(Player player);
}
