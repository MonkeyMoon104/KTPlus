package com.monkey.ktplus.effects.list.grave.corpse;

import com.monkey.ktplus.libs.packetevents.packetevents.PacketEvents;
import com.monkey.ktplus.libs.packetevents.packetevents.manager.server.ServerVersion;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.entity.type.EntityTypes;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.player.GameMode;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.player.UserProfile;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.world.Location;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.PacketWrapper;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.monkey.ktplus.libs.packetevents.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;

final class FakePlayerCorpseNpc {
    private final int entityId;
    private final UserProfile profile;
    private final String teamName;
    private Location location = new Location(0.0, 0.0, 0.0, 0.0f, 0.0f);
    private final Set<Object> channels = new HashSet<>();

    FakePlayerCorpseNpc(UserProfile profile, int entityId) {
        this.profile = profile;
        this.entityId = entityId;
        this.teamName = "kt-grave-" + entityId;
    }

    void setLocation(Location location) {
        this.location = location;
    }

    int entityId() {
        return entityId;
    }

    boolean hasSpawned(Object channel) {
        return channels.contains(channel);
    }

    void spawn(Object channel) {
        if (hasSpawned(channel)) {
            return;
        }
        PacketWrapper<?> playerInfo;
        if (isModernPlayerInfo()) {
            playerInfo = new WrapperPlayServerPlayerInfoUpdate(
                    WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, modernPlayerInfo());
        } else {
            playerInfo = new WrapperPlayServerPlayerInfo(
                    WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, legacyPlayerInfo());
        }
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, playerInfo);

        PacketWrapper<?> spawnPacket;
        if (isModernSpawn()) {
            spawnPacket = new WrapperPlayServerSpawnEntity(
                    entityId,
                    profile.getUUID(),
                    EntityTypes.PLAYER,
                    location,
                    location.getYaw(),
                    0,
                    null);
        } else {
            spawnPacket = new WrapperPlayServerSpawnPlayer(entityId, profile.getUUID(), location);
        }
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, spawnPacket);

        PacketEvents.getAPI()
                .getProtocolManager()
                .sendPacket(channel, createTeamPacket(WrapperPlayServerTeams.TeamMode.CREATE));
        channels.add(channel);
    }

    void despawn(Object channel) {
        if (!hasSpawned(channel)) {
            return;
        }
        PacketEvents.getAPI()
                .getProtocolManager()
                .sendPacket(channel, teamPacket(WrapperPlayServerTeams.TeamMode.REMOVE));
        removeFromTabList(channel);
        PacketEvents.getAPI()
                .getProtocolManager()
                .sendPacket(channel, new WrapperPlayServerDestroyEntities(entityId));
        channels.remove(channel);
    }

    void despawnAll() {
        for (Object channel : new HashSet<>(channels)) {
            despawn(channel);
        }
    }

    private void removeFromTabList(Object channel) {
        PacketWrapper<?> removePacket;
        if (isModernPlayerInfo()) {
            removePacket = new WrapperPlayServerPlayerInfoRemove(profile.getUUID());
        } else {
            removePacket = new WrapperPlayServerPlayerInfo(
                    WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, legacyPlayerInfo());
        }
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, removePacket);
    }

    private WrapperPlayServerTeams teamPacket(WrapperPlayServerTeams.TeamMode mode) {
        if (mode == WrapperPlayServerTeams.TeamMode.REMOVE) {
            return new WrapperPlayServerTeams(teamName, mode, (WrapperPlayServerTeams.ScoreBoardTeamInfo) null);
        }
        return createTeamPacket(mode);
    }

    private WrapperPlayServerTeams createTeamPacket(WrapperPlayServerTeams.TeamMode mode) {
        return new WrapperPlayServerTeams(
                teamName,
                mode,
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                        Component.text(teamName),
                        null,
                        null,
                        WrapperPlayServerTeams.NameTagVisibility.NEVER,
                        WrapperPlayServerTeams.CollisionRule.NEVER,
                        null,
                        WrapperPlayServerTeams.OptionData.NONE),
                profile.getName());
    }

    private WrapperPlayServerPlayerInfo.PlayerData legacyPlayerInfo() {
        return new WrapperPlayServerPlayerInfo.PlayerData(null, profile, GameMode.SURVIVAL, 0);
    }

    private WrapperPlayServerPlayerInfoUpdate.PlayerInfo modernPlayerInfo() {
        return new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile, true, 0, GameMode.SURVIVAL, null, null);
    }

    private static boolean isModernPlayerInfo() {
        return PacketEvents.getAPI()
                .getServerManager()
                .getVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_19_3);
    }

    private static boolean isModernSpawn() {
        return PacketEvents.getAPI()
                .getServerManager()
                .getVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_20_2);
    }
}
