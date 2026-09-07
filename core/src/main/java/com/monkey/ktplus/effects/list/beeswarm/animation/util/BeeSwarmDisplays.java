package com.monkey.ktplus.effects.list.beeswarm.animation.util;

import com.monkey.ktplus.util.compat.EntityCompat;
import org.bukkit.Location;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.jspecify.annotations.Nullable;

public final class BeeSwarmDisplays {
    private BeeSwarmDisplays() {}

    public static @Nullable Bee spawnBee(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        
        if (at.getY() - at.getBlockY() < 0.4) {
            at.add(0.0, 0.6, 0.0);
        }
        try {
            Bee bee = (Bee) location.getWorld().spawnEntity(at, EntityType.BEE);
            EntityCompat.trySetSilent(bee, true);
            EntityCompat.trySetInvulnerable(bee, true);
            EntityCompat.trySetPersistent(bee, false);
            EntityCompat.trySetGravity(bee, false);
            
            EntityCompat.trySetAi(bee, false);
            bee.setCollidable(false);
            bee.setRemoveWhenFarAway(true);
            bee.setAnger(0);
            bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
            bee.setHasStung(false);
            return bee;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void place(@Nullable Bee bee, Location at, float yawDegrees) {
        if (bee == null || !bee.isValid() || bee.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(yawDegrees);
        target.setPitch(0.0f);
        bee.teleport(target);
    }

    public static void remove(@Nullable Bee bee) {
        if (bee != null && bee.isValid() && !bee.isDead()) {
            bee.remove();
        }
    }

    public static void removeAll(Iterable<Bee> bees) {
        for (Bee bee : bees) {
            remove(bee);
        }
    }
}
