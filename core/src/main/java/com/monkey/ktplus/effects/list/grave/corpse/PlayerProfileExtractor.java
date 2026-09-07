package com.monkey.ktplus.effects.list.grave.corpse;

import com.monkey.ktplus.libs.packetevents.packetevents.protocol.player.TextureProperty;
import com.monkey.ktplus.libs.packetevents.packetevents.protocol.player.UserProfile;
import com.monkey.ktplus.libs.packetevents.packetevents.util.SpigotReflectionUtil;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

final class PlayerProfileExtractor {
    private PlayerProfileExtractor() {}

    static UserProfile profile(Entity victim) {
        String name = displayName(victim);
        List<TextureProperty> textures = textures(victim);
        if (textures == null) {
            textures = Collections.emptyList();
        }
        return new UserProfile(fakeEntityUuid(), name, textures);
    }

    private static UUID fakeEntityUuid() {
        return UUID.randomUUID();
    }

    static String displayName(Entity victim) {
        if (victim instanceof Player) {
            return ((Player) victim).getName();
        }
        String name = victim.getName();
        return name != null ? name : randomName();
    }

    static List<TextureProperty> textures(@Nullable Entity victim) {
        if (victim instanceof Player) {
            try {
                return SpigotReflectionUtil.getUserProfile((Player) victim);
            } catch (Throwable ignored) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    static String randomName() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
