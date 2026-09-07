package com.monkey.ktplus.effects.list.grave.corpse;

import com.monkey.ktplus.libs.packetevents.packetevents.util.Vector3i;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

final class GraveCorpseOrientation {
    
    static final double KILLER_SHIFT = 0.4D;

    static final int HIDDEN_BED_Y_OFFSET = -1;

    private GraveCorpseOrientation() {}

    static Vector stemAxis(BlockFace viewFace) {
        return new Vector(viewFace.getModX(), 0.0D, viewFace.getModZ());
    }

    static float corpseYaw(BlockFace viewFace) {
        switch (viewFace) {
            case NORTH:
                return -90.0F;
            case SOUTH:
                return 90.0F;
            case EAST:
                return 0.0F;
            case WEST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    static Location corpsePosition(Location centerSlab, BlockFace towardKiller) {
        return centerSlab.clone().add(
                0.5D + towardKiller.getModX() * KILLER_SHIFT,
                0.0D,
                0.5D + towardKiller.getModZ() * KILLER_SHIFT);
    }

    static Vector sleepingBodyAxis(float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        return new Vector(Math.cos(radians), 0.0D, Math.sin(radians));
    }

    static Location bedHeadBlock(Location centerSlab, BlockFace viewFace) {
        return centerSlab.clone().add(viewFace.getModX(), 0, viewFace.getModZ());
    }

    static Location hiddenBedHeadBlock(Location centerSlab, BlockFace viewFace) {
        return bedHeadBlock(centerSlab, viewFace).add(0, HIDDEN_BED_Y_OFFSET, 0);
    }

    static Vector3i hiddenBedHeadPosition(Location centerSlab, BlockFace viewFace) {
        Location bed = hiddenBedHeadBlock(centerSlab, viewFace);
        return new Vector3i(bed.getBlockX(), bed.getBlockY(), bed.getBlockZ());
    }
}
