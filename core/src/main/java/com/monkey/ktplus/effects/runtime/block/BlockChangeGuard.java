package com.monkey.ktplus.effects.runtime.block;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface BlockChangeGuard {
    boolean allows(@Nullable Player actor, Location location);
}
