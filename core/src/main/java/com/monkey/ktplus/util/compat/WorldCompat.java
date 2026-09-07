package com.monkey.ktplus.util.compat;

import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;
import com.monkey.ktplus.util.item.SoundKeys;

public final class WorldCompat {
    private WorldCompat() {}

    public static void playSound(World world, Location location, String sound, float volume, float pitch) {
        if (world == null || location == null || sound == null) {
            return;
        }
        String key = SoundKeys.normalize(sound);
        try {
            world.getClass()
                    .getMethod("playSound", Location.class, String.class, float.class, float.class)
                    .invoke(world, location, key, volume, pitch);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Class<?> soundClass = Class.forName("org.bukkit.Sound");
            Object soundEnum = EnumCompat.valueOfOrNull(soundClass, sound);
            if (soundEnum != null) {
                world.getClass()
                        .getMethod("playSound", Location.class, soundClass, float.class, float.class)
                        .invoke(world, location, soundEnum, volume, pitch);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
