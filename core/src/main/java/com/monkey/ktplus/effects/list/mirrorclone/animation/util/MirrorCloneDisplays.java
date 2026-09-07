package com.monkey.ktplus.effects.list.mirrorclone.animation.util;

import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.compat.MaterialResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class MirrorCloneDisplays {
    private static final int INTERPOLATION_TICKS = 2;
    public static final String TAG = "ktplus_mirrorclone";

    private MirrorCloneDisplays() {}

    public static @Nullable LivingEntity spawnRunner(Location feet, @Nullable Player killer, double speed) {
        World world = feet.getWorld();
        if (world == null) {
            return null;
        }
        Location at = feet.clone();
        at.setYaw(killer != null ? killer.getLocation().getYaw() : 0.0f);
        at.setPitch(0.0f);

        LivingEntity runner = spawnPreferred(world, at);
        if (runner == null) {
            return null;
        }

        EntityCompat.trySetSilent(runner, true);
        EntityCompat.trySetPersistent(runner, false);
        EntityCompat.trySetInvulnerable(runner, true);
        EntityCompat.trySetAi(runner, true);
        runner.setRemoveWhenFarAway(false);
        runner.setCanPickupItems(false);
        runner.setCollidable(false);
        runner.addScoreboardTag(TAG);

        String name = killer != null ? killer.getName() : "Mirror Clone";
        runner.customName(Component.text(name));
        runner.setCustomNameVisible(true);

        if (runner instanceof Ageable ageable) {
            ageable.setAdult();
        }
        if (runner instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
        }

        EntityEquipment equipment = runner.getEquipment();
        if (equipment != null) {
            equipment.clear();
            equipment.setHelmet(null);
            equipment.setChestplate(null);
            equipment.setLeggings(null);
            equipment.setBoots(null);
            equipment.setItemInOffHand(null);
            ItemStack tnt = new ItemStack(Material.TNT);
            equipment.setItemInMainHand(tnt);
            equipment.setItemInMainHandDropChance(0.0f);
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
            equipment.setItemInOffHandDropChance(0.0f);
        }

        AttributeInstance move = runner.getAttribute(Attribute.MOVEMENT_SPEED);
        if (move != null) {
            double base = move.getBaseValue();
            
            double boost = Math.max(1.15, Math.min(2.1, 0.9 + speed * 2.4));
            move.setBaseValue(base * boost);
        }

        return runner;
    }

    public static void removeRunner(@Nullable LivingEntity runner) {
        if (runner != null && runner.isValid() && !runner.isDead()) {
            runner.remove();
        }
    }

    public static @Nullable ItemDisplay spawnShard(Location location) {
        Material glass = MaterialResolver.resolve("GLASS_PANE", "WHITE_STAINED_GLASS_PANE");
        return spawn(location, glass, 0.28f);
    }

    public static void place(
            @Nullable ItemDisplay display, Location at, float yaw, float pitch, float roll, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.08f, scale);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeAll(Iterable<ItemDisplay> displays) {
        for (ItemDisplay display : displays) {
            remove(display);
        }
    }

    private static @Nullable LivingEntity spawnPreferred(World world, Location at) {
        try {
            return world.spawn(at, Husk.class, husk -> {});
        } catch (Throwable ignored) {
            
        }
        try {
            return (LivingEntity) world.spawnEntity(at, EntityType.ZOMBIE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable ItemDisplay spawn(Location location, Material material, float scale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.08f, scale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(13, 13));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.2f);
            display.setShadowStrength(0.45f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGlowing(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(safe, safe, safe),
                    new Quaternionf()));
        });
    }
}
