package com.monkey.ktplus.util.compat;

import org.bukkit.Material;
import org.bukkit.util.Vector;

public final class MaterialCompat {
    private MaterialCompat() {}

    public static boolean isAir(Material type) {
        if (type == null) {
            return true;
        }
        try {
            return (Boolean) Material.class.getMethod("isAir").invoke(type);
        } catch (ReflectiveOperationException ex) {
            return type == Material.AIR;
        }
    }

    public static boolean isReplaceableGround(Material type, Material grassAlias) {
        return type == Material.DIRT || type == grassAlias || isCoarseDirt(type) || isPodzol(type);
    }

    public static boolean isCoarseDirt(Material type) {
        Material resolved = MaterialResolver.find("COARSE_DIRT");
        return resolved != null && type == resolved;
    }

    public static boolean isPodzol(Material type) {
        Material resolved = MaterialResolver.find("PODZOL");
        return resolved != null && type == resolved;
    }

    public static Vector rotateAroundY(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }
}
