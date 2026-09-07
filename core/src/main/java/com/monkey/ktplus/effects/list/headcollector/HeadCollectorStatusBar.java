package com.monkey.ktplus.effects.list.headcollector;

import com.monkey.ktplus.util.text.TextFormatter;
import java.util.Locale;
import org.bukkit.entity.Player;

final class HeadCollectorStatusBar {
    private HeadCollectorStatusBar() {}

    static void update(Player killer, HeadCollectorSettings settings, int headCount) {
        if (headCount <= 0) {
            clear(killer);
            return;
        }
        double damage = settings.launchDamageForHeads(headCount);
        float health = settings.absorptionForHeads(headCount);
        String ready = headCount >= settings.maxHeads()
                ? " &a● " + settings.readyLabel()
                : "";
        killer.sendActionBar(TextFormatter.component(String.format(
                Locale.US,
                "&6☠ &f%d&7/&f%d &8| &c⚔ &f%.1f &8| &e❤ &f+%.0f%s",
                headCount,
                settings.maxHeads(),
                damage,
                health,
                ready)));
    }

    static void clear(Player killer) {
        killer.sendActionBar(TextFormatter.component(""));
    }
}
