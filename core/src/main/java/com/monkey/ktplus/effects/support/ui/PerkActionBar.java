package com.monkey.ktplus.effects.support.ui;

import com.monkey.ktplus.util.text.TextFormatter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class PerkActionBar {
    private PerkActionBar() {}

    public static void show(@Nullable Player player, String text) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.sendActionBar(TextFormatter.component(text));
    }

    public static void clear(@Nullable Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.sendActionBar(TextFormatter.component(""));
    }
}
