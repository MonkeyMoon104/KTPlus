package com.monkey.ktplus.effects.list.wither.animation;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.list.wither.animation.util.WitherParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ExpandingRing;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.entity.SupportedEntities;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

public final class WitherOrbitalAnimation {
    private final EffectSession session;
    private final VisualEffectService visuals;
    private final Location center;
    private final Player killer;
    private final ConfigSnapshot config;

    public WitherOrbitalAnimation(
            EffectSession session,
            VisualEffectService visuals,
            Location center,
            Player killer,
            ConfigSnapshot config) {
        this.session = Objects.requireNonNull(session, "session");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.center = Objects.requireNonNull(center, "center").clone();
        this.killer = Objects.requireNonNull(killer, "killer");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void start() {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        String entityType = visuals.entity("WITHER");
        if (!SupportedEntities.isPresent(entityType)) {
            ExpandingRing.play(session, visuals, "SOUL", "WITCH", center, Color.BLACK, 50, 0.1, 0.02, 2L);
            session.runLater(
                    100L, () -> WitherParticles.spawnFinalImplosion(session, visuals, center, killer, config));
            return;
        }

        Entity wither1 = spawnControlledWither(entityType, center.clone().add(5, 0, 0));
        Entity wither2 = spawnControlledWither(entityType, center.clone().add(-5, 0, 0));
        if (wither1 == null || wither2 == null) {
            ExpandingRing.play(session, visuals, "SOUL", "WITCH", center, Color.BLACK, 50, 0.1, 0.02, 2L);
            session.runLater(
                    100L, () -> WitherParticles.spawnFinalImplosion(session, visuals, center, killer, config));
            return;
        }

        double[] angle = {0};
        int[] ticks = {0};

        session.runTimer(0L, 2L, () -> {
            if (ticks[0] > 100) {
                wither1.remove();
                wither2.remove();
                WitherParticles.spawnFinalImplosion(session, visuals, center, killer, config);
                visuals.sound("AMBIENT_CAVE", center, 3.0f, 0.4f);
                return false;
            }

            angle[0] += Math.PI / 30;

            double radius = 5;
            double x1 = Math.cos(angle[0]) * radius;
            double z1 = Math.sin(angle[0]) * radius;
            double y1 = Math.sin(angle[0] / 2) * 2;

            double x2 = Math.cos(angle[0] + Math.PI) * radius;
            double z2 = Math.sin(angle[0] + Math.PI) * radius;
            double y2 = Math.cos(angle[0] / 2) * 2;

            Location pos1 = center.clone().add(x1, y1, z1);
            Location pos2 = center.clone().add(x2, y2, z2);

            orientLocationTowardsPlayer(pos1, killer);
            orientLocationTowardsPlayer(pos2, killer);

            wither1.teleport(pos1);
            wither2.teleport(pos2);

            if (ticks[0] % 10 == 0) {
                EntityCompat.tryHideWitherBossBar(wither1);
                EntityCompat.tryHideWitherBossBar(wither2);
                launchSkull(world, pos1, center);
                launchSkull(world, pos2, center);
            }

            visuals.particle(
                    "SOUL",
                    pos1,
                    ParticleScale.scale(5),
                    0.2,
                    0.2,
                    0.2,
                    0.01,
                    Color.BLACK);
            visuals.particle(
                    "ASH",
                    pos2,
                    ParticleScale.scale(5),
                    0.2,
                    0.2,
                    0.2,
                    0.01,
                    Color.BLACK);

            if (ticks[0] % 20 == 0) {
                world.strikeLightningEffect(center.clone().add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2));
            }

            ticks[0]++;
            return true;
        });
    }

    private Entity spawnControlledWither(String entityType, Location loc) {
        return session.spawnEntity(
                entityType,
                loc,
                spawned -> {
                    if (spawned instanceof LivingEntity living) {
                        EntityCompat.trySetHealth(living, 300.0D);
                        EntityCompat.trySetCollidable(living, false);
                    }
                    EntityCompat.trySetSilent(spawned, true);
                    EntityCompat.tryHideWitherBossBar(spawned);
                    EntityCompat.trySetCustomNameVisible(spawned, false);
                    session.entityRegistry().registerOwnedDamager(spawned.getUniqueId(), killer.getUniqueId());
                    try {
                        Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                        Object name = componentClass.getMethod("text", String.class).invoke(null, "Spectral Wither");
                        spawned.getClass().getMethod("customName", componentClass).invoke(spawned, name);
                    } catch (ReflectiveOperationException ignored) {
                    }
                },
                250L);
    }

    private void launchSkull(World world, Location from, Location to) {
        EntityType skullType = SupportedEntities.resolve(visuals.entity("WITHER_SKULL"));
        if (skullType == null || world == null) {
            return;
        }

        Vector direction = to.clone().subtract(from).toVector().normalize();
        if (direction.getY() >= 0) {
            return;
        }

        Location spawn = from.clone().add(0, 1, 0);
        if (!session.allowsWorldMutation(killer, spawn)) {
            return;
        }

        WitherSkull skull = (WitherSkull) world.spawnEntity(spawn, skullType);
        skull.setDirection(direction);
        skull.setVelocity(direction.multiply(0.7));
        EntityCompat.trySetInvulnerable(skull, true);
        skull.setYield(0);
        skull.setCharged(true);
        EntityCompat.trySetGravity(skull, true);
        EntityCompat.trySetSilent(skull, true);
        session.entityRegistry().registerOwnedDamager(skull.getUniqueId(), killer.getUniqueId());
        session.trackEntity(skull);
        session.runLater(200L, skull::remove);

        session.runTimer(0L, 1L, () -> {
            if (!skull.isValid() || skull.isDead()) {
                return false;
            }
            visuals.particle(
                    "SOUL_FIRE_FLAME",
                    skull.getLocation(),
                    ParticleScale.scale(2),
                    0,
                    0,
                    0,
                    0.01,
                    Color.BLACK);
            visuals.particle(
                    "LARGE_SMOKE",
                    skull.getLocation(),
                    ParticleScale.scale(1),
                    0.05,
                    0.05,
                    0.05,
                    0.01,
                    Color.BLACK);
            return true;
        });
    }

    private void orientLocationTowardsPlayer(Location from, Player player) {
        Location to = player.getEyeLocation();

        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

        from.setYaw(yaw);
        from.setPitch(pitch);
    }
}
