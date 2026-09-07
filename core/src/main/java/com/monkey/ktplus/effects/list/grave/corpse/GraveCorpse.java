package com.monkey.ktplus.effects.list.grave.corpse;

import com.monkey.ktplus.libs.packetevents.packetevents.PacketEvents;
import com.monkey.ktplus.libs.packetevents.packetevents.manager.server.ServerVersion;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.entity.data.EntityData;
import com.monkey.ktplus.libs.packetevents.packetevents.util.SpigotConversionUtil;
import com.monkey.ktplus.libs.packetevents.packetevents.util.Vector3i;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.player.UserProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

final class GraveCorpse {
    private final int entityId;
    private final Location centerSlab;
    private final BlockFace viewFace;
    private final Location location;
    private final FakePlayerCorpseNpc npc;
    private final Set<Player> seeingPlayers = new CopyOnWriteArraySet<>();
    private volatile boolean destroyed;

    GraveCorpse(Location centerSlab, BlockFace viewFace, BlockFace towardKiller, UserProfile profile) {
        this.entityId = EntityIdAllocator.allocate();
        this.centerSlab = centerSlab.clone();
        this.viewFace = viewFace;
        this.location = GraveCorpseBedUtil.corpseLocation(centerSlab, viewFace, towardKiller);
        this.npc = new FakePlayerCorpseNpc(profile, entityId);
        this.npc.setLocation(SpigotConversionUtil.fromBukkitLocation(this.location));
    }

    int entityId() {
        return entityId;
    }

    Location location() {
        return location.clone();
    }

    void show(Player observer) {
        if (destroyed || observer == null || !observer.isOnline()) {
            return;
        }
        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(observer.getUniqueId());
        if (channel == null) {
            return;
        }
        seeingPlayers.add(observer);
        npc.spawn(channel);
        applySleepingMetadata(observer, channel);
    }

    void hide(Player observer) {
        if (observer == null) {
            return;
        }
        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(observer.getUniqueId());
        if (channel != null) {
            npc.despawn(channel);
        }
        seeingPlayers.remove(observer);
        restoreBedOverlay(observer);
    }

    private void restoreBedOverlay(Player observer) {
        Location hiddenBed = GraveCorpseOrientation.hiddenBedHeadBlock(centerSlab, viewFace);
        GraveCorpseModernBed.hide(observer, hiddenBed);
    }

    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        for (Player observer : new ArrayList<>(seeingPlayers)) {
            hide(observer);
        }
        seeingPlayers.clear();
        npc.despawnAll();
        EntityIdAllocator.release(entityId);
    }

    private void applySleepingMetadata(Player observer, Object channel) {
        ServerVersion version =
                PacketEvents.getAPI().getServerManager().getVersion();
        Location hiddenBed = GraveCorpseOrientation.hiddenBedHeadBlock(centerSlab, viewFace);
        Vector3i bedHeadPos = GraveCorpseOrientation.hiddenBedHeadPosition(centerSlab, viewFace);
        GraveCorpseModernBed.show(observer, hiddenBed, viewFace);
        sendMetadata(channel, GraveCorpseMetadata.modernSleeping(bedHeadPos, version));
        syncRotation(channel);
        lockTransform(channel);
    }

    private void lockTransform(Object channel) {
        com.monkey.ktplus.libs.packetevents.packetevents.protocol.world.Location peLocation =
                SpigotConversionUtil.fromBukkitLocation(location);
        WrapperPlayServerEntityTeleport teleport =
                new WrapperPlayServerEntityTeleport(entityId, peLocation, true);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, teleport);
    }

    private void syncRotation(Object channel) {
        float bodyYaw = location.getYaw();
        WrapperPlayServerEntityRotation bodyRotation =
                new WrapperPlayServerEntityRotation(entityId, bodyYaw, 0.0F, true);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, bodyRotation);
    }

    private void sendMetadata(Object channel, List<EntityData<?>> metadata) {
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }
}
