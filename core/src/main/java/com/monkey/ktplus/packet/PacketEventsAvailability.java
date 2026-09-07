package com.monkey.ktplus.packet;

import com.monkey.ktplus.libs.packetevents.packetevents.PacketEvents;

public final class PacketEventsAvailability {
    private PacketEventsAvailability() {}

    public static boolean isReady() {
        try {
            return PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
