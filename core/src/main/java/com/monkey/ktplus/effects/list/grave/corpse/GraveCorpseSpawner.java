package com.monkey.ktplus.effects.list.grave.corpse;

import com.monkey.ktplus.libs.packetevents.packetevents.protocol.player.UserProfile;
import com.monkey.ktplus.effects.list.grave.animation.util.GraveStructurePlacer;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.packet.PacketEventsAvailability;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class GraveCorpseSpawner {
    private static final double VIEW_RADIUS = 48.0D;
    private static final double VIEW_RADIUS_SQUARED = VIEW_RADIUS * VIEW_RADIUS;
    private static final long SPAWN_DELAY_TICKS = 2L;

    private GraveCorpseSpawner() {}

    public static void spawn(
            EffectSession session,
            Entity victim,
            Location anchor,
            BlockFace viewFace,
            BlockFace towardKiller,
            long durationTicks) {
        if (!PacketEventsAvailability.isReady()) {
            return;
        }
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }

        Location centerSlab = GraveStructurePlacer.rotatedBlockLocation(anchor, viewFace, 0, 1, 0);
        UserProfile profile = PlayerProfileExtractor.profile(victim);
        GraveCorpse corpse = new GraveCorpse(centerSlab, viewFace, towardKiller, profile);

        session.onCleanup(corpse::destroy);
        session.runLater(durationTicks, corpse::destroy);
        session.runLater(SPAWN_DELAY_TICKS, () -> showToNearbyPlayers(world, corpse.location(), corpse));
    }

    private static void showToNearbyPlayers(World world, Location corpseLocation, GraveCorpse corpse) {
        Location origin = corpseLocation.clone();
        for (Player observer : world.getPlayers()) {
            if (!observer.getWorld().equals(world)) {
                continue;
            }
            if (observer.getLocation().distanceSquared(origin) <= VIEW_RADIUS_SQUARED) {
                corpse.show(observer);
            }
        }
    }
}
