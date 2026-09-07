package com.monkey.ktplus.effects.list.skeleton.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class SkeletonAnimation {
    public static final String TAG = "ktplus_effect_skeleton";

    private static final int LIFE_TICKS = 110;
    private static final double DESCEND_PER_TICK = 0.028;

    private SkeletonAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location center = context.location().clone();
        Player killer = context.killer();
        Player victim = context.victim() instanceof Player player ? player : null;
        if (center.getWorld() == null) {
            return;
        }

        center.getWorld().strikeLightningEffect(center);
        visuals.particle("CLOUD", center, ParticleScale.scale(80), 1.5, 1.5, 1.5, 0.05, Color.WHITE);
        visuals.particle("SOUL", center.clone().add(0, 1.0, 0), ParticleScale.scale(30), 0.4, 0.6, 0.4, 0.02, null);
        visuals.sound("BLOCK_PORTAL_TRAVEL", center, 2.0f, 0.6f);

        String skeletonType = visuals.entity("SKELETON");
        String displayName = victim != null ? victim.getName() : "Skeleton";
        Location spawnAt = center.clone().add(0, 0.35, 0);

        Entity spawned = session.spawnEntity(
                skeletonType,
                spawnAt,
                entity -> {
                    if (!(entity instanceof Skeleton skeleton)) {
                        return;
                    }
                    EntityCompat.trySetGravity(skeleton, false);
                    EntityCompat.trySetInvulnerable(skeleton, true);
                    EntityCompat.trySetAi(skeleton, false);
                    EntityCompat.trySetSilent(skeleton, true);
                    EntityCompat.trySetCollidable(skeleton, false);
                    skeleton.setRemoveWhenFarAway(false);
                    skeleton.setCanPickupItems(false);
                    skeleton.addScoreboardTag(TAG);
                    skeleton.customName(Component.text(displayName));
                    skeleton.setCustomNameVisible(true);
                    skeleton.addPotionEffect(
                            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60, 0, false, false, false));
                    skeleton.setFireTicks(0);
                    skeleton.setSilent(true);
                    EntityEquipment equipment = skeleton.getEquipment();
                    if (equipment != null) {
                        equipment.clear();
                        if (victim != null) {
                            copyEquipment(victim.getInventory(), equipment);
                        }
                        equipment.setHelmet(null);
                        equipment.setHelmetDropChance(0f);
                    }
                    if (killer != null && killer.isOnline()) {
                        EntityCompat.tryLookAt(skeleton, killer.getEyeLocation());
                    }
                },
                LIFE_TICKS + 20L);

        if (!(spawned instanceof Skeleton skeleton)) {
            return;
        }

        AtomicInteger ticks = new AtomicInteger();
        Location[] loc = {skeleton.getLocation().clone()};

        session.onCleanup(() -> {
            stopSkeletonSounds(center);
            if (skeleton.isValid() && !skeleton.isDead()) {
                skeleton.remove();
            }
        });

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || !skeleton.isValid() || skeleton.isDead()) {
                stopSkeletonSounds(center);
                return false;
            }

            EntityCompat.trySetInvulnerable(skeleton, true);
            EntityCompat.trySetGravity(skeleton, false);
            EntityCompat.trySetAi(skeleton, false);
            EntityCompat.trySetSilent(skeleton, true);
            skeleton.setFireTicks(0);
            skeleton.setSilent(true);

            int age = ticks.getAndIncrement();
            if (age >= LIFE_TICKS) {
                Location end = skeleton.getLocation();
                visuals.particle("DUST", end, ParticleScale.scale(50), 0.55, 0.7, 0.55, 0.02, Color.WHITE);
                visuals.particle("SOUL", end.clone().add(0, 0.8, 0), ParticleScale.scale(24), 0.35, 0.5, 0.35, 0.03, null);
                visuals.particle("CLOUD", end, ParticleScale.scale(28), 0.4, 0.5, 0.4, 0.02, Color.WHITE);
                visuals.sound("BLOCK_LAVA_EXTINGUISH", end, 1.0f, 0.8f);
                stopSkeletonSounds(center);
                skeleton.remove();
                return false;
            }

            loc[0].subtract(0, DESCEND_PER_TICK, 0);
            faceKiller(skeleton, loc[0], killer);
            EntityCompat.tryTeleportAsync(skeleton, loc[0]);

            Location body = loc[0].clone().add(0, 1.0, 0);
            visuals.particle("CLOUD", body, ParticleScale.scale(8), 0.3, 0.45, 0.3, 0.01, Color.WHITE);
            visuals.particle("CRIT", body, ParticleScale.scale(3), 0.18, 0.28, 0.18, 0.0, Color.WHITE);
            if (age % 2 == 0) {
                visuals.particle("SOUL", body, 4, 0.2, 0.35, 0.2, 0.01, null);
            }
            if (age % 3 == 0) {
                visuals.dust(body, Color.fromRGB(220, 230, 240), 1.1f, 3, 0.25, 0.4, 0.25, 0.0);
                visuals.particle("END_ROD", body, 2, 0.15, 0.3, 0.15, 0.0, null);
            }
            return true;
        });
    }

    private static void stopSkeletonSounds(Location center) {
        if (center.getWorld() == null) {
            return;
        }
        double rangeSq = 48.0 * 48.0;
        for (Player player : center.getWorld().getPlayers()) {
            if (!player.isOnline() || player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            EntityCompat.stopAllSounds(player);
            EntityCompat.stopSound(player, "BLOCK_PORTAL_TRAVEL");
            EntityCompat.stopSound(player, "BLOCK_LAVA_EXTINGUISH");
        }
    }

    private static void faceKiller(Skeleton skeleton, Location at, Player killer) {
        if (killer == null || !killer.isOnline() || at.getWorld() == null) {
            return;
        }
        if (!at.getWorld().equals(killer.getWorld())) {
            return;
        }
        Location eyes = killer.getEyeLocation();
        Vector direction = eyes.toVector().subtract(at.clone().add(0.0, 1.6, 0.0).toVector());
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        Location look = at.clone();
        look.setDirection(direction);
        at.setYaw(look.getYaw());
        at.setPitch(look.getPitch());
        EntityCompat.tryLookAt(skeleton, eyes);
    }

    private static void copyEquipment(PlayerInventory inventory, EntityEquipment equipment) {
        equipment.setHelmet(null);
        equipment.setChestplate(inventory.getChestplate());
        equipment.setLeggings(inventory.getLeggings());
        equipment.setBoots(inventory.getBoots());
        ItemStack mainHand = inventory.getItemInMainHand();
        equipment.setItemInMainHand(mainHand);
        ItemStack offHand = inventory.getItemInOffHand();
        equipment.setItemInOffHand(offHand);
        equipment.setHelmetDropChance(0f);
        equipment.setChestplateDropChance(0f);
        equipment.setLeggingsDropChance(0f);
        equipment.setBootsDropChance(0f);
        equipment.setItemInMainHandDropChance(0f);
        equipment.setItemInOffHandDropChance(0f);
    }
}
