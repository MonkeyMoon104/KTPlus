package com.monkey.ktplus.gui.action;

import com.monkey.ktplus.effects.support.particle.BukkitParticles;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class GuiClickFeedback {
    private GuiClickFeedback() {}

    public static void play(Player player) {
        Objects.requireNonNull(player, "player");
        Location location = player.getLocation().clone().add(0.0, 1.8, 0.0);
        EntityCompat.playSound(player, location, "UI_BUTTON_CLICK", 0.45f, 1.25f);
        BukkitParticles.particleSimple(
                player.getWorld(), "HAPPY_VILLAGER", location, 2, "VILLAGER_HAPPY", "CRIT");
    }
}
