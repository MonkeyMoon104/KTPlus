package com.monkey.ktplus.effects.list.enchantcolumn.animation.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class ParticleArm {
    public Location location;
    public Vector direction;

    public ParticleArm(Location location, Vector direction) {
        this.location = location;
        this.direction = direction;
    }
}
