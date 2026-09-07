package com.monkey.ktplus.effects.list.headcollector;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HeadDisplayUtil {
    static final float HEAD_SCALE = 0.74f;
    private static final int INTERPOLATION_TICKS = 4;

    private static final Vector3f TRANSLATION = new Vector3f();
    private static final Vector3f SCALE = new Vector3f(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
    private static final Quaternionf IDENTITY = new Quaternionf();

    private HeadDisplayUtil() {}

    public static ItemDisplay spawn(Location location, Player victim) {
        if (location.getWorld() == null) {
            return null;
        }
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(playerHead(victim));
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(48.0f);
            display.setShadowRadius(0.0f);
            display.setShadowStrength(0.0f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTransformation(identityTransform());
        });
    }

    static ItemStack playerHead(Player victim) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setPlayerProfile(victim.getPlayerProfile());
            head.setItemMeta(meta);
        }
        return head;
    }

    public static void setSlowSpin(ItemDisplay display, double yawDegrees) {
        applyRotation(display, 0.0f, (float) Math.toRadians(yawDegrees), 0.0f);
    }

    public static void setTornadoPose(ItemDisplay display, double selfSpinRadians, double orbitAngleRadians) {
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) selfSpinRadians)
                .rotateX(0.55f)
                .rotateZ((float) (orbitAngleRadians * 0.35));
        display.setTransformation(new Transformation(TRANSLATION, rotation, SCALE, IDENTITY));
    }

    public static void setFormationPose(
            ItemDisplay display, Player killer, LivingEntity target, double selfSpinRadians) {
        Location killerLoc = killer.getLocation();
        Location targetLoc = target.getLocation();
        double aimYaw = Math.atan2(
                -(targetLoc.getX() - killerLoc.getX()),
                targetLoc.getZ() - killerLoc.getZ());
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) aimYaw)
                .rotateX(0.12f)
                .rotateY((float) selfSpinRadians);
        display.setTransformation(new Transformation(TRANSLATION, rotation, SCALE, IDENTITY));
    }

    public static void setLaunchPose(ItemDisplay display, double selfSpinRadians, float pitchRadians) {
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) selfSpinRadians)
                .rotateX(pitchRadians);
        display.setTransformation(new Transformation(TRANSLATION, rotation, SCALE, IDENTITY));
    }

    public static void remove(ItemDisplay display) {
        if (display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static void applyRotation(ItemDisplay display, float pitch, float yaw, float roll) {
        Quaternionf rotation = new Quaternionf().rotateZ(roll).rotateY(yaw).rotateX(pitch);
        display.setTransformation(new Transformation(TRANSLATION, rotation, SCALE, IDENTITY));
    }

    private static Transformation identityTransform() {
        return new Transformation(TRANSLATION, new Quaternionf(), SCALE, IDENTITY);
    }
}
