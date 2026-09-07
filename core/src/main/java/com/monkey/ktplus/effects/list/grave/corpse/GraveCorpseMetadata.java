package com.monkey.ktplus.effects.list.grave.corpse;

import com.monkey.ktplus.libs.packetevents.packetevents.manager.server.ServerVersion;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.entity.data.EntityData;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.entity.data.EntityDataTypes;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.entity.pose.EntityPose;
import com.monkey.ktplus.libs.packetevents.packetevents.util.Vector3i;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class GraveCorpseMetadata {
    private static final byte ALL_SKIN_PARTS = (byte) 0x7F;

    private GraveCorpseMetadata() {}

    static List<EntityData<?>> modernSleeping(Vector3i bedHeadPosition, ServerVersion version) {
        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.SLEEPING));
        metadata.add(new EntityData<>(
                14, EntityDataTypes.OPTIONAL_BLOCK_POSITION, Optional.of(bedHeadPosition)));
        metadata.add(new EntityData<>(skinPartsIndex(version), EntityDataTypes.BYTE, ALL_SKIN_PARTS));
        return metadata;
    }

    private static int skinPartsIndex(ServerVersion version) {
        if (version.isOlderThanOrEquals(ServerVersion.V_1_12_2)) {
            return 10;
        }
        if (version.isNewerThanOrEquals(ServerVersion.V_1_21)) {
            return 16;
        }
        return 17;
    }
}
