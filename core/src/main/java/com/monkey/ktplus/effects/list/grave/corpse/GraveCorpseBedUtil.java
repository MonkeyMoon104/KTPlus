package com.monkey.ktplus.effects.list.grave.corpse;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

final class GraveCorpseBedUtil {
    private static final double CORPSE_Y_OFFSET = 0.5625D;

    private GraveCorpseBedUtil() {}

    static Location corpseLocation(Location centerSlab, BlockFace viewFace, BlockFace towardKiller) {
        Location location = GraveCorpseOrientation.corpsePosition(centerSlab, towardKiller);
        location.setY(centerSlab.getY() + CORPSE_Y_OFFSET);
        location.setPitch(0.0F);
        location.setYaw(GraveCorpseOrientation.corpseYaw(viewFace));
        return location;
    }
}
