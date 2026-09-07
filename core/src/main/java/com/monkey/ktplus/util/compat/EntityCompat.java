package com.monkey.ktplus.util.compat;

import io.papermc.paper.entity.LookAnchor;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public final class EntityCompat {
    private EntityCompat() {}

    private static final int FIREWORK_FLIGHT_CAP_TICKS = 20 * 60;

    public static Firework spawnSilentFirework(World world, Location location) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(location, "location");
        return world.spawn(location, Firework.class, firework -> firework.setSilent(true));
    }

    public static Arrow spawnArrow(
            World world, Location location, Vector direction, float speed, @Nullable Consumer<Arrow> setup) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(direction, "direction");
        Vector dir = direction.clone();
        if (dir.lengthSquared() < 1.0e-6) {
            dir = new Vector(0.0, 0.0, 1.0);
        } else {
            dir.normalize();
        }
        Vector velocity = dir.multiply(speed);
        return world.spawn(location, Arrow.class, arrow -> {
            arrow.setVelocity(velocity.clone());
            if (setup != null) {
                setup.accept(arrow);
            }
        });
    }

    public static boolean tryExtendFireworkFlight(Firework firework) {
        if (firework == null) {
            return false;
        }
        firework.setTicksFlown(0);
        firework.setTicksToDetonate(FIREWORK_FLIGHT_CAP_TICKS);
        return true;
    }

    public static void trySetSilent(Entity entity, boolean value) {
        if (entity != null) {
            entity.setSilent(value);
        }
    }

    public static void trySetInvulnerable(Entity entity, boolean value) {
        if (entity != null) {
            entity.setInvulnerable(value);
        }
    }

    public static void trySetGravity(Entity entity, boolean value) {
        if (entity != null) {
            entity.setGravity(value);
        }
    }

    public static void trySetPersistent(Entity entity, boolean value) {
        if (entity != null) {
            entity.setPersistent(value);
        }
    }

    public static void trySetAi(LivingEntity entity, boolean value) {
        if (entity != null) {
            entity.setAI(value);
        }
    }

    public static void trySetCollidable(Entity entity, boolean value) {
        if (entity instanceof LivingEntity living) {
            living.setCollidable(value);
        }
    }

    public static void trySetCustomNameVisible(Entity entity, boolean value) {
        if (entity != null) {
            entity.setCustomNameVisible(value);
        }
    }

    public static void trySetHealth(LivingEntity entity, double health) {
        if (entity == null) {
            return;
        }
        var maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double max = maxHealth == null ? health : maxHealth.getValue();
        entity.setHealth(Math.min(health, max));
    }

    public static void trySetAware(Entity entity, boolean value) {
        if (entity instanceof Mob mob) {
            mob.setAware(value);
        }
    }

    public static void trySetCanPickupItems(LivingEntity entity, boolean value) {
        if (entity != null) {
            entity.setCanPickupItems(value);
        }
    }

    public static void trySetRemoveWhenFarAway(LivingEntity entity, boolean value) {
        if (entity != null) {
            entity.setRemoveWhenFarAway(value);
        }
    }

    public static void tryHideWitherBossBar(Entity entity) {
        if (entity instanceof Wither wither) {
            wither.getBossBar().setVisible(false);
        }
    }

    public static void tryHideDragonBossBar(Entity entity) {
        if (entity instanceof EnderDragon dragon && dragon.getBossBar() != null) {
            dragon.getBossBar().setVisible(false);
        }
    }

    public static void tryLookAt(Entity entity, Location location) {
        if (entity != null && location != null) {
            entity.lookAt(location, LookAnchor.EYES);
        }
    }

    public static void tryTeleportAsync(LivingEntity entity, Location location) {
        if (entity == null || location == null) {
            return;
        }
        entity.teleportAsync(location);
    }

    public static void trySetArrowPickup(Arrow arrow, boolean disallow) {
        if (arrow != null) {
            arrow.setPickupStatus(
                    disallow ? AbstractArrow.PickupStatus.DISALLOWED : AbstractArrow.PickupStatus.ALLOWED);
        }
    }

    public static void playSound(Player player, Location location, String sound, float volume, float pitch) {
        playSound(player, location, sound, "PLAYERS", volume, pitch);
    }

    public static void playSound(
            Player player, Location location, String sound, String category, float volume, float pitch) {
        if (player == null || location == null || sound == null) {
            return;
        }
        SoundCategory soundCategory = SoundCategory.valueOf(category);
        player.playSound(location, normalizeSoundKey(sound), soundCategory, volume, pitch);
    }

    public static void stopSound(Player player, String sound) {
        if (player == null || sound == null) {
            return;
        }
        String key = normalizeSoundKey(sound);
        player.stopSound(key);
        if (!key.contains(":")) {
            player.stopSound("minecraft:" + key);
        }
    }

    public static void stopSound(Player player, String sound, String category) {
        if (player == null || sound == null) {
            return;
        }
        if (category == null || category.isBlank()) {
            stopSound(player, sound);
            return;
        }
        SoundCategory soundCategory = SoundCategory.valueOf(category);
        String key = normalizeSoundKey(sound);
        player.stopSound(key, soundCategory);
        if (!key.contains(":")) {
            player.stopSound("minecraft:" + key, soundCategory);
        }
    }

    public static void stopAllSounds(Player player) {
        if (player == null) {
            return;
        }
        try {
            player.stopAllSounds();
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
        }
    }

    private static String normalizeSoundKey(String sound) {
        if (sound == null || sound.isEmpty()) {
            return "";
        }
        String trimmed = sound.trim();
        if (trimmed.contains(":")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        if (trimmed.contains(".")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (upper.startsWith("MUSIC_DISC_")) {
            return "music_disc." + upper.substring("MUSIC_DISC_".length()).toLowerCase(Locale.ROOT);
        }
        return trimmed.toLowerCase(Locale.ROOT).replace('_', '.');
    }
}
