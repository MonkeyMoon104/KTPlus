package com.monkey.ktplus.effects.list.headcollector;

import com.monkey.ktplus.effects.support.particle.BukkitParticles;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.hook.HookManager;
import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.scheduler.ScheduledHandle;
import com.monkey.ktplus.user.UserService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public final class HeadCollectorService {
    private static final String EFFECT_ID = "headcollector";
    private static final NamespacedKey HEALTH_BONUS_KEY = new NamespacedKey("ktplus", "headcollector_health");
    private static final NamespacedKey ABSORPTION_CAP_KEY = new NamespacedKey("ktplus", "headcollector_absorption");
    private static final double MISSILE_RISE_HEIGHT = 2.8;

    private final PlatformScheduler scheduler;
    private final UserService users;
    private final HookManager hooks;
    private final VisualEffectService visuals;
    private final Map<UUID, CollectorState> states = new ConcurrentHashMap<>();

    public HeadCollectorService(
            PlatformScheduler scheduler,
            UserService users,
            HookManager hooks,
            VisualEffectService visuals) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.users = Objects.requireNonNull(users, "users");
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    public void adoptHead(Player killer, ItemDisplay display, HeadCollectorSettings settings) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(display, "display");
        Objects.requireNonNull(settings, "settings");
        if (!hasEffectSelected(killer.getUniqueId())) {
            HeadDisplayUtil.remove(display);
            return;
        }
        CollectorState state = states.computeIfAbsent(killer.getUniqueId(), ignored -> new CollectorState(settings));
        state.settings = settings;
        if (state.activeHeadCount() >= settings.maxHeads()) {
            HeadDisplayUtil.remove(display);
            return;
        }
        HeadCollectorHead head = new HeadCollectorHead(
                display, HeadCollectorTrail.particleForIndex(state.heads.size()), state.heads.size());
        head.phase = HeadCollectorHead.Phase.APPROACH;
        state.heads.add(head);
        ensureOrbitTask(killer, state);
    }

    public int inheritHeads(Player killer, Player victim, HeadCollectorSettings settings) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(settings, "settings");
        if (!settings.inheritHeads() || killer.getUniqueId().equals(victim.getUniqueId())) {
            return 0;
        }
        if (!hasEffectSelected(killer.getUniqueId())) {
            return 0;
        }
        CollectorState victimState = states.get(victim.getUniqueId());
        if (victimState == null || victimState.heads.isEmpty()) {
            return 0;
        }

        victimState.forming = false;
        victimState.launching = false;
        victimState.pendingTarget = null;
        victimState.formationQueue.clear();

        List<HeadCollectorHead> stolen = new ArrayList<>(victimState.heads);
        victimState.heads.clear();
        resetKillerBonuses(victim);
        if (victimState.orbitTask != null) {
            victimState.orbitTask.cancel();
            victimState.orbitTask = null;
        }
        states.remove(victim.getUniqueId());

        CollectorState killerState =
                states.computeIfAbsent(killer.getUniqueId(), ignored -> new CollectorState(settings));
        killerState.settings = settings;
        ensureOrbitTask(killer, killerState);

        int inheritBudget = Math.max(0, settings.maxHeads() - killerState.activeHeadCount() - 1);
        int inherited = 0;

        for (HeadCollectorHead stolenHead : stolen) {
            if (!stolenHead.display.isValid() || stolenHead.display.isDead()) {
                continue;
            }
            if (inheritBudget <= 0) {
                HeadDisplayUtil.remove(stolenHead.display);
                continue;
            }
            HeadCollectorHead head = new HeadCollectorHead(
                    stolenHead.display,
                    HeadCollectorTrail.particleForIndex(killerState.heads.size()),
                    killerState.heads.size());
            head.phase = HeadCollectorHead.Phase.APPROACH;
            head.selfRotation = stolenHead.selfRotation;
            HeadCollectorTrail.reset(head);
            killerState.heads.add(head);
            inheritBudget--;
            inherited++;
        }
        return inherited;
    }

    public void stripPersistedBonuses(Player player) {
        if (player == null) {
            return;
        }
        resetKillerBonuses(player);
    }

    public void stripPersistedBonusesForOnlinePlayers() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!states.containsKey(player.getUniqueId())) {
                stripPersistedBonuses(player);
            }
        }
    }

    public void tryLaunchHeads(Player killer, LivingEntity target) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(target, "target");
        if (target.equals(killer)) {
            return;
        }
        CollectorState state = states.get(killer.getUniqueId());
        if (state == null || state.forming || state.launching) {
            return;
        }
        List<HeadCollectorHead> launchable = state.orbitHeads();
        if (launchable.size() < state.settings.maxHeads()) {
            return;
        }
        beginFormation(killer, target, state, launchable);
    }

    public void clear(Player killer) {
        if (killer != null) {
            clear(killer.getUniqueId());
        }
    }

    public void clear(UUID killerId) {
        CollectorState state = states.remove(killerId);
        if (state != null) {
            state.attackEpoch++;
            state.forming = false;
            state.launching = false;
            state.pendingTarget = null;
            Player killer = org.bukkit.Bukkit.getPlayer(killerId);
            if (killer != null && killer.isOnline()) {
                resetKillerBonuses(killer);
            }
            state.dispose();
        }
    }

    public void clearAll() {
        for (UUID killerId : new ArrayList<>(states.keySet())) {
            clear(killerId);
        }
    }

    public void shutdown() {
        clearAll();
    }

    private void beginFormation(
            Player killer, LivingEntity target, CollectorState state, List<HeadCollectorHead> launchable) {
        state.forming = true;
        state.pendingTarget = target;
        state.formationQueue.clear();
        state.formationQueue.addAll(launchable);
        state.formationQueue.sort(Comparator.comparingInt(head -> head.slotIndex));
        for (HeadCollectorHead head : state.formationQueue) {
            head.phase = HeadCollectorHead.Phase.FORMATION;
            head.formationArrived = false;
            HeadCollectorTrail.reset(head);
        }
        visuals.sound("BLOCK_BEACON_POWER_SELECT", killer.getLocation(), 0.55f, 1.35f);
    }

    private void launchHeadAt(
            Player killer,
            LivingEntity target,
            CollectorState state,
            List<HeadCollectorHead> launchable,
            int index) {
        if (!isAttackActive(killer, state)) {
            return;
        }
        if (!isTargetValid(target)) {
            finishAttack(killer, state);
            return;
        }
        if (index >= launchable.size()) {
            finishAttack(killer, state);
            return;
        }
        HeadCollectorHead head = launchable.get(index);
        if (!head.display.isValid() || head.display.isDead()) {
            scheduleNextMissile(killer, target, state, launchable, index + 1, state.settings);
            return;
        }
        head.phase = HeadCollectorHead.Phase.LAUNCH;
        head.launchPhase = HeadCollectorHead.LaunchPhase.RISE;
        head.launchTicks = 0;
        head.launchOrigin = head.display.getLocation().clone();
        head.launchPeak = head.launchOrigin.clone().add(0.0, MISSILE_RISE_HEIGHT, 0.0);
        head.launchTarget = target;
        HeadCollectorTrail.reset(head);
        visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", head.display.getLocation(), 0.45f, 1.35f + (index * 0.05f));
        animateMissile(killer, target, state, head, launchable, index);
    }

    private void animateMissile(
            Player killer,
            LivingEntity target,
            CollectorState state,
            HeadCollectorHead head,
            List<HeadCollectorHead> launchable,
            int index) {
        if (!isAttackActive(killer, state)) {
            return;
        }
        if (!head.display.isValid() || head.display.isDead()) {
            scheduleNextMissile(killer, target, state, launchable, index + 1, state.settings);
            return;
        }
        LivingEntity launchTarget = head.launchTarget;
        if (!isTargetValid(launchTarget)) {
            finishAttack(killer, state);
            return;
        }

        Location next = missileLocation(head, launchTarget, state.settings);
        head.display.teleport(next);
        head.selfRotation += state.settings.headSpinSpeed() * 1.6;
        HeadDisplayUtil.setLaunchPose(head.display, head.selfRotation, (float) (-Math.PI / 3.0));
        HeadCollectorTrail.emitDense(head, next);

        if (shouldImpact(head, next, launchTarget, state.settings)) {
            applyLaunchHit(killer, launchTarget, state, head);
            scheduleNextMissile(killer, target, state, launchable, index + 1, state.settings);
            return;
        }

        head.launchTicks++;
        if (head.launchPhase == HeadCollectorHead.LaunchPhase.RISE
                && head.launchTicks >= state.settings.missileRiseTicks()) {
            head.launchPhase = HeadCollectorHead.LaunchPhase.STRIKE;
            head.launchTicks = 0;
        }
        scheduler.runLater(killer, () -> animateMissile(killer, target, state, head, launchable, index), 1L);
    }

    private Location missileLocation(HeadCollectorHead head, LivingEntity target, HeadCollectorSettings settings) {
        if (head.launchPhase == HeadCollectorHead.LaunchPhase.RISE) {
            double progress = easeOut(head.launchTicks / (double) settings.missileRiseTicks());
            return interpolate(head.launchOrigin, head.launchPeak, progress);
        }
        Location goal = target.getLocation().add(0.0, target.getHeight() * 0.62, 0.0);
        Location peak = head.launchPeak != null ? head.launchPeak : head.display.getLocation();
        Location control = peak.clone().add(
                (goal.getX() - peak.getX()) * 0.35,
                1.35,
                (goal.getZ() - peak.getZ()) * 0.35);
        double progress = easeIn(Math.min(1.0, head.launchTicks / (double) settings.missileStrikeTicks()));
        return quadraticBezier(peak, control, goal, progress);
    }

    private boolean shouldImpact(
            HeadCollectorHead head, Location current, LivingEntity target, HeadCollectorSettings settings) {
        if (head.launchPhase != HeadCollectorHead.LaunchPhase.STRIKE) {
            return false;
        }
        Location goal = target.getLocation().add(0.0, target.getHeight() * 0.62, 0.0);
        if (current.distanceSquared(goal) <= 0.55) {
            return true;
        }
        return head.launchTicks >= settings.missileStrikeTicks();
    }

    private void scheduleNextMissile(
            Player killer,
            LivingEntity target,
            CollectorState state,
            List<HeadCollectorHead> launchable,
            int index,
            HeadCollectorSettings settings) {
        if (!isAttackActive(killer, state)) {
            return;
        }
        scheduler.runLater(
                killer,
                () -> launchHeadAt(killer, target, state, launchable, index),
                settings.missileGapTicks());
    }

    private void applyLaunchHit(Player killer, LivingEntity target, CollectorState state, HeadCollectorHead head) {
        Location impact = head.display.getLocation();
        if (target.isValid()
                && !target.isDead()
                && state.activeLaunchDamage > 0.0D
                && hooks.worldGuard().allowsProtectedAction(killer, target.getLocation())) {
            target.damage(state.activeLaunchDamage, killer);
        }
        BukkitParticles.spawn("CRIT", impact, 10, 0.08, 0.08, 0.08, 0.02);
        BukkitParticles.spawn("SWEEP_ATTACK", impact, 2, 0.0, 0.0, 0.0, 0.0);
        BukkitParticles.spawn(head.trailParticle, impact, 6, 0.12, 0.12, 0.12, 0.01);
        visuals.sound("ENTITY_GENERIC_EXPLODE", impact, 0.85f, 1.15f);
        HeadDisplayUtil.remove(head.display);
        state.heads.remove(head);
    }

    private void ensureOrbitTask(Player killer, CollectorState state) {
        if (state.orbitTask != null) {
            return;
        }
        state.orbitTask = scheduler.runTimer(killer, () -> tickOrbit(killer.getUniqueId()), 1L, 1L);
    }

    private void tickOrbit(UUID killerId) {
        Player killer = org.bukkit.Bukkit.getPlayer(killerId);
        if (killer == null || !killer.isOnline() || killer.isDead() || !hasEffectSelected(killerId)) {
            clear(killerId);
            return;
        }
        CollectorState state = states.get(killerId);
        if (state == null || state.heads.isEmpty()) {
            clear(killerId);
            return;
        }

        if (!state.forming && !state.launching) {
            state.spin += state.settings.orbitSpeed();
        }

        if ((state.forming || state.launching) && !isTargetValid(state.pendingTarget)) {
            abortAttack(killer, state);
        }

        Iterator<HeadCollectorHead> iterator = state.heads.iterator();
        while (iterator.hasNext()) {
            HeadCollectorHead head = iterator.next();
            if (!head.display.isValid() || head.display.isDead()) {
                iterator.remove();
                continue;
            }
            if (head.phase == HeadCollectorHead.Phase.LAUNCH) {
                continue;
            }
            if (head.phase == HeadCollectorHead.Phase.APPROACH) {
                tickApproach(killer, state, head);
                continue;
            }
            if (head.phase == HeadCollectorHead.Phase.FORMATION) {
                tickFormation(killer, state, head);
                continue;
            }
            tickOrbitHead(killer, state, head);
        }

        if (state.forming) {
            checkFormationReady(killer, state);
        }

        if (state.heads.isEmpty() && !state.forming && !state.launching) {
            clear(killerId);
            return;
        }

        refreshKillerBonuses(killer, state);
    }

    private void tickFormation(Player killer, CollectorState state, HeadCollectorHead head) {
        LivingEntity target = state.pendingTarget;
        if (!isTargetValid(target)) {
            abortAttack(killer, state);
            return;
        }
        List<HeadCollectorHead> queue = state.formationQueue;
        if (queue.isEmpty()) {
            return;
        }
        int index = queue.indexOf(head);
        if (index < 0) {
            return;
        }
        Location slot = formationSlot(killer, index, queue.size(), state.settings);
        Location current = head.display.getLocation();
        Vector delta = slot.toVector().subtract(current.toVector());
        Location next;
        if (delta.lengthSquared() <= 0.18) {
            next = slot;
            head.formationArrived = true;
        } else {
            next = current.clone().add(delta.normalize().multiply(0.38));
            head.formationArrived = false;
        }
        head.display.teleport(next);
        head.selfRotation += state.settings.headSpinSpeed();
        HeadDisplayUtil.setFormationPose(head.display, killer, target, head.selfRotation);
        if (!head.formationArrived) {
            HeadCollectorTrail.emitOrbit(head, next, state.settings.orbitTrailInterval());
        }
    }

    private void checkFormationReady(Player killer, CollectorState state) {
        List<HeadCollectorHead> queue = state.formationQueue;
        if (queue.isEmpty()) {
            return;
        }
        for (HeadCollectorHead head : queue) {
            if (head.phase != HeadCollectorHead.Phase.FORMATION || !head.formationArrived) {
                return;
            }
        }
        state.forming = false;
        state.launching = true;
        state.activeLaunchDamage = state.settings.launchDamageForHeads(queue.size());
        LivingEntity target = state.pendingTarget;
        visuals.sound("ENTITY_BLAZE_SHOOT", killer.getLocation(), 0.45f, 0.75f);
        scheduler.runLater(
                killer,
                () -> launchHeadAt(killer, target, state, queue, 0),
                state.settings.formationHoldTicks());
    }

    private void abortAttack(Player killer, CollectorState state) {
        state.attackEpoch++;
        returnAttackHeadsToOrbit(state);
        state.forming = false;
        state.launching = false;
        state.pendingTarget = null;
        state.formationQueue.clear();
    }

    private void finishAttack(Player killer, CollectorState state) {
        state.attackEpoch++;
        returnAttackHeadsToOrbit(state);
        state.forming = false;
        state.launching = false;
        state.pendingTarget = null;
        state.formationQueue.clear();
    }

    private void returnAttackHeadsToOrbit(CollectorState state) {
        for (HeadCollectorHead head : state.heads) {
            if (head.phase == HeadCollectorHead.Phase.FORMATION
                    || head.phase == HeadCollectorHead.Phase.LAUNCH) {
                head.phase = HeadCollectorHead.Phase.ORBIT;
                head.formationArrived = false;
                head.launchTicks = 0;
                head.launchOrigin = null;
                head.launchPeak = null;
                head.launchTarget = null;
                HeadCollectorTrail.reset(head);
            }
        }
    }

    private void returnFormationHeadsToOrbit(CollectorState state) {
        returnAttackHeadsToOrbit(state);
    }

    private Location formationSlot(Player killer, int index, int total, HeadCollectorSettings settings) {
        Location base = killer.getLocation();
        Vector back = base.getDirection().clone().setY(0);
        if (back.lengthSquared() < 0.0001) {
            back = new Vector(0, 0, 1);
        } else {
            back.normalize().multiply(-1);
        }
        Vector right = new Vector(-back.getZ(), 0.0, back.getX());
        double lineOffset = (index - ((total - 1) / 2.0)) * settings.formationSpacing();
        return base.clone()
                .add(back.clone().multiply(settings.formationDistance()))
                .add(right.multiply(lineOffset))
                .add(0.0, settings.formationHeight(), 0.0);
    }

    private void refreshKillerBonuses(Player killer, CollectorState state) {
        int heads = state.activeHeadCount();
        removeHealthBonus(killer);
        removeAbsorptionCap(killer);
        if (heads <= 0) {
            killer.setAbsorptionAmount(0.0f);
            HeadCollectorStatusBar.clear(killer);
            return;
        }

        double bonusHealth = state.settings.healthPerHead() * heads;
        AttributeInstance maxHealth = killer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && bonusHealth > 0.0D) {
            maxHealth.addModifier(new AttributeModifier(
                    HEALTH_BONUS_KEY, bonusHealth, AttributeModifier.Operation.ADD_NUMBER));
        }

        float absorption = state.settings.absorptionForHeads(heads);
        AttributeInstance maxAbsorption = killer.getAttribute(Attribute.MAX_ABSORPTION);
        if (maxAbsorption != null && absorption > 0.0f) {
            maxAbsorption.addModifier(new AttributeModifier(
                    ABSORPTION_CAP_KEY, absorption, AttributeModifier.Operation.ADD_NUMBER));
        }
        killer.setAbsorptionAmount(absorption);
        HeadCollectorStatusBar.update(killer, state.settings, heads);
    }

    private void resetKillerBonuses(Player killer) {
        removeHealthBonus(killer);
        removeAbsorptionCap(killer);
        killer.setAbsorptionAmount(0.0f);
        HeadCollectorStatusBar.clear(killer);
    }

    private static void removeHealthBonus(Player killer) {
        AttributeInstance maxHealth = killer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.getModifiers().stream()
                .filter(modifier -> HEALTH_BONUS_KEY.equals(modifier.getKey()))
                .forEach(maxHealth::removeModifier);
    }

    private static void removeAbsorptionCap(Player killer) {
        AttributeInstance maxAbsorption = killer.getAttribute(Attribute.MAX_ABSORPTION);
        if (maxAbsorption == null) {
            return;
        }
        maxAbsorption.getModifiers().stream()
                .filter(modifier -> ABSORPTION_CAP_KEY.equals(modifier.getKey()))
                .forEach(maxAbsorption::removeModifier);
    }

    private void tickApproach(Player killer, CollectorState state, HeadCollectorHead head) {
        Location current = head.display.getLocation();
        Location goal = killer.getLocation().clone().add(0.0, 1.15, 0.0);
        Vector delta = goal.toVector().subtract(current.toVector());
        if (delta.lengthSquared() <= 0.45) {
            head.phase = HeadCollectorHead.Phase.ORBIT;
            HeadCollectorTrail.reset(head);
            return;
        }
        Vector step = delta.normalize().multiply(0.32);
        Location next = current.clone().add(step);
        head.display.teleport(next);
        head.selfRotation += state.settings.headSpinSpeed();
        HeadDisplayUtil.setSlowSpin(head.display, Math.toDegrees(head.selfRotation));
        HeadCollectorTrail.emitDense(head, next);
    }

    private void tickOrbitHead(Player killer, CollectorState state, HeadCollectorHead head) {
        List<HeadCollectorHead> orbitHeads = state.orbitHeads();
        int index = Math.max(0, orbitHeads.indexOf(head));
        int orbitCount = Math.max(1, orbitHeads.size());
        double angle = state.spin + ((Math.PI * 2.0 * index) / orbitCount);
        double radius = state.settings.orbitRadius() * (0.94 + (Math.sin(state.spin * 1.6 + index) * 0.06));
        double spiralLift = Math.sin(angle * 1.8 + index) * 0.28;
        double layer = index * 0.14;
        Location center = killer.getLocation();
        Location orbit = new Location(
                center.getWorld(),
                center.getX() + (Math.cos(angle) * radius),
                center.getY() + state.settings.orbitHeight() + layer + spiralLift,
                center.getZ() + (Math.sin(angle) * radius));
        head.display.teleport(orbit);
        head.selfRotation += state.settings.headSpinSpeed();
        HeadDisplayUtil.setTornadoPose(head.display, head.selfRotation, angle);
        HeadCollectorTrail.emitOrbit(head, orbit, state.settings.orbitTrailInterval());
    }

    private boolean isAttackActive(Player killer, CollectorState state) {
        if (killer == null || !killer.isOnline() || killer.isDead()) {
            return false;
        }
        CollectorState current = states.get(killer.getUniqueId());
        return current == state && state.launching;
    }

    private static boolean isTargetValid(@Nullable LivingEntity target) {
        return target != null && target.isValid() && !target.isDead();
    }

    private boolean hasEffectSelected(UUID killerId) {
        return users.selectedEffect(killerId).map(id -> id.equalsIgnoreCase(EFFECT_ID)).orElse(false);
    }

    private static Location interpolate(@Nullable Location from, @Nullable Location to, double t) {
        if (from == null || to == null || from.getWorld() == null) {
            return to != null ? to : from;
        }
        double x = from.getX() + ((to.getX() - from.getX()) * t);
        double y = from.getY() + ((to.getY() - from.getY()) * t);
        double z = from.getZ() + ((to.getZ() - from.getZ()) * t);
        return new Location(from.getWorld(), x, y, z);
    }

    private static Location quadraticBezier(Location start, Location control, Location end, double t) {
        double u = 1.0 - t;
        double x = (u * u * start.getX()) + (2.0 * u * t * control.getX()) + (t * t * end.getX());
        double y = (u * u * start.getY()) + (2.0 * u * t * control.getY()) + (t * t * end.getY());
        double z = (u * u * start.getZ()) + (2.0 * u * t * control.getZ()) + (t * t * end.getZ());
        return new Location(start.getWorld(), x, y, z);
    }

    private static double easeOut(double t) {
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    private static double easeIn(double t) {
        return t * t * t;
    }

    private static final class CollectorState {
        private HeadCollectorSettings settings;
        private final List<HeadCollectorHead> heads = new ArrayList<>();
        private final List<HeadCollectorHead> formationQueue = new ArrayList<>();
        private @Nullable ScheduledHandle orbitTask;
        private @Nullable LivingEntity pendingTarget;
        private double spin;
        private boolean forming;
        private boolean launching;
        private double activeLaunchDamage;
        private int attackEpoch;

        private CollectorState(HeadCollectorSettings settings) {
            this.settings = settings;
        }

        private int activeHeadCount() {
            int count = 0;
            for (HeadCollectorHead head : heads) {
                if (head.phase == HeadCollectorHead.Phase.ORBIT
                        || head.phase == HeadCollectorHead.Phase.APPROACH
                        || head.phase == HeadCollectorHead.Phase.FORMATION) {
                    count++;
                }
            }
            return count;
        }

        private List<HeadCollectorHead> orbitHeads() {
            List<HeadCollectorHead> orbitHeads = new ArrayList<>();
            for (HeadCollectorHead head : heads) {
                if (head.phase == HeadCollectorHead.Phase.ORBIT) {
                    orbitHeads.add(head);
                }
            }
            return orbitHeads;
        }

        private void dispose() {
            if (orbitTask != null) {
                orbitTask.cancel();
                orbitTask = null;
            }
            for (HeadCollectorHead head : heads) {
                HeadDisplayUtil.remove(head.display);
            }
            heads.clear();
            formationQueue.clear();
        }
    }
}
