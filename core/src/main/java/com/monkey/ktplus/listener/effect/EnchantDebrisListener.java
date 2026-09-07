package com.monkey.ktplus.listener.effect;

import com.monkey.ktplus.effects.list.earthquake.animation.util.EarthquakeDebris;
import com.monkey.ktplus.effects.list.enchantcolumn.animation.util.EnchantGroundBurst;
import java.util.Objects;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public final class EnchantDebrisListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        Objects.requireNonNull(event, "event");
        if (!(event.getEntity() instanceof FallingBlock falling)) {
            return;
        }
        if (!falling.getScoreboardTags().contains(EnchantGroundBurst.TAG)
                && !falling.getScoreboardTags().contains(EarthquakeDebris.TAG)) {
            return;
        }
        event.setCancelled(true);
        falling.remove();
    }
}
