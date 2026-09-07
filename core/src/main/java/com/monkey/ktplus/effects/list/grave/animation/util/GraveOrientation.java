package com.monkey.ktplus.effects.list.grave.animation.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public final class GraveOrientation {
    private GraveOrientation() {}

    public static BlockFace viewFace(Location killerLocation) {
        Vector direction = killerLocation.getDirection();
        direction.setY(0);
        if (direction.lengthSquared() < 0.0001D) {
            return yawToFace(killerLocation.getYaw());
        }
        direction.normalize();
        return vectorToFace(direction.getX(), direction.getZ());
    }

    public static BlockFace towardKiller(BlockFace viewFace) {
        return viewFace.getOppositeFace();
    }

    public static Location offsetFace(Location origin, BlockFace face) {
        return origin.clone().add(face.getModX(), 0, face.getModZ());
    }

    private static BlockFace vectorToFace(double x, double z) {
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private static BlockFace yawToFace(float yaw) {
        float rot = (yaw % 360 + 360) % 360;
        if (rot >= 45 && rot < 135) {
            return BlockFace.WEST;
        }
        if (rot >= 135 && rot < 225) {
            return BlockFace.NORTH;
        }
        if (rot >= 225 && rot < 315) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }
}
