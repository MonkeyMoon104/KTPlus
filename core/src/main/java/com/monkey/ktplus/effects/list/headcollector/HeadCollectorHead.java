package com.monkey.ktplus.effects.list.headcollector;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

final class HeadCollectorHead {
    enum Phase {
        APPROACH,
        ORBIT,
        FORMATION,
        LAUNCH
    }

    enum LaunchPhase {
        RISE,
        STRIKE
    }

    final ItemDisplay display;
    final String trailParticle;
    final int slotIndex;
    Phase phase = Phase.APPROACH;
    LaunchPhase launchPhase = LaunchPhase.RISE;
    double selfRotation;
    int launchTicks;
    boolean formationArrived;
    @Nullable Location lastTrailLocation;
    @Nullable Location launchOrigin;
    @Nullable Location launchPeak;
    @Nullable LivingEntity launchTarget;
    int trailCooldown;

    HeadCollectorHead(ItemDisplay display, String trailParticle, int slotIndex) {
        this.display = display;
        this.trailParticle = trailParticle;
        this.slotIndex = slotIndex;
    }
}
