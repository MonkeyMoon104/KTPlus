package com.monkey.ktplus.effects.runtime;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksFinaleService;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksMarkedTracker;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksSettings;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.runtime.block.BlockChangeGuard;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import com.monkey.ktplus.platform.ServerLoadProbe;
import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.task.CancellationReason;
import com.monkey.ktplus.task.TaskRegistry;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class EffectRuntime {
    private final PlatformScheduler scheduler;
    private final TaskRegistry taskRegistry;
    private final TemporaryBlockService temporaryBlocks;
    private final ServerLoadProbe loadProbe = new ServerLoadProbe();
    private BlockChangeGuard blockChangeGuard;
    private final Map<UUID, EffectSession> sessions = new ConcurrentHashMap<UUID, EffectSession>();
    private final Map<UUID, Set<UUID>> sessionsByPlayer = new ConcurrentHashMap<UUID, Set<UUID>>();
    private final AtomicInteger heavySessions = new AtomicInteger();
    private final EffectEntityRegistry entityRegistry = new EffectEntityRegistry();
    private final FireworksFinaleService fireworksFinale;
    private ConfigSnapshot config;

    public EffectRuntime(
            ConfigSnapshot config,
            PlatformScheduler scheduler,
            TaskRegistry taskRegistry,
            TemporaryBlockService temporaryBlocks,
            BlockChangeGuard blockChangeGuard) {
        this.config = Objects.requireNonNull(config, "config");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.taskRegistry = Objects.requireNonNull(taskRegistry, "taskRegistry");
        this.temporaryBlocks = Objects.requireNonNull(temporaryBlocks, "temporaryBlocks");
        this.blockChangeGuard = Objects.requireNonNull(blockChangeGuard, "blockChangeGuard");
        this.fireworksFinale = new FireworksFinaleService(scheduler, entityRegistry, blockChangeGuard);
    }

    public void reload(ConfigSnapshot config, BlockChangeGuard blockChangeGuard) {
        this.config = Objects.requireNonNull(config, "config");
        this.blockChangeGuard = Objects.requireNonNull(blockChangeGuard, "blockChangeGuard");
    }

    public EffectEntityRegistry entityRegistry() {
        return entityRegistry;
    }

    public void onFinalePlayerChangedWorld(Player player, org.bukkit.World fromWorld) {
        fireworksFinale.onPlayerChangedWorld(player, fromWorld);
    }

    public boolean start(Player killer, Entity victim, Location location, KillEffect effect) {
        Set<UUID> playerSessions =
                sessionsByPlayer.computeIfAbsent(killer.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
        if (playerSessions.size() >= config.maxSessionsPerPlayer()) {
            return false;
        }
        boolean heavy = effect.definition().heavy();
        if (heavy && heavySessions.get() >= heavyCap()) {
            return false;
        }
        UUID sessionId = UUID.randomUUID();
        EffectSession session = new EffectSession(
                sessionId,
                killer.getUniqueId(),
                effect.definition().id(),
                heavy,
                location,
                scheduler,
                taskRegistry,
                temporaryBlocks,
                blockChangeGuard,
                this);
        sessions.put(sessionId, session);
        playerSessions.add(sessionId);
        if (heavy) {
            heavySessions.incrementAndGet();
        }
        try {
            effect.execute(new EffectContext(killer, victim, location.clone(), config), session);
            if (!session.active()) {
                return false;
            }
            session.resetDeadline(effect.definition().maxDurationTicks());
            return true;
        } catch (RuntimeException ex) {
            finish(session, CancellationReason.ERROR);
            throw ex;
        }
    }

    public void cancelPlayer(UUID playerId, CancellationReason reason) {
        fireworksFinale.onPlayerQuit(playerId);
        Set<UUID> ids = sessionsByPlayer.get(playerId);
        if (ids == null) {
            return;
        }
        for (UUID id : new ArrayList<UUID>(ids)) {
            EffectSession session = sessions.get(id);
            if (session != null) {
                finish(session, reason);
            }
        }
    }

    public void cancelWorld(String worldName, CancellationReason reason) {
        fireworksFinale.abortForWorld(worldName);
        for (EffectSession session : new ArrayList<EffectSession>(sessions.values())) {
            if (session.origin().getWorld().getName().equals(worldName)) {
                finish(session, reason);
            }
        }
    }

    public void cancelAll(CancellationReason reason) {
        for (EffectSession session : new ArrayList<EffectSession>(sessions.values())) {
            finish(session, reason);
        }
        fireworksFinale.shutdown();
        temporaryBlocks.restoreAll();
        heavySessions.set(0);
    }

    void handoffFireworksFinale(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            FireworksSettings settings,
            FireworksMarkedTracker tracker,
            boolean allowStructure) {
        tracker.stopFootTracking();
        session.detachBlockChanges(tracker.pinnedBlockChanges());
        fireworksFinale.launch(visuals, session.origin(), killer, settings, tracker, allowStructure);
        finish(session, CancellationReason.COMPLETED);
    }

    void finish(EffectSession session, CancellationReason reason) {
        if (!session.close()) {
            return;
        }
        taskRegistry.cancelSession(session.id());
        session.cleanup();
        sessions.remove(session.id());
        if (session.heavy()) {
            heavySessions.updateAndGet(value -> Math.max(0, value - 1));
        }
        Set<UUID> playerSessions = sessionsByPlayer.get(session.playerId());
        if (playerSessions != null) {
            playerSessions.remove(session.id());
            if (playerSessions.isEmpty()) {
                sessionsByPlayer.remove(session.playerId());
            }
        }
    }

    private int heavyCap() {
        return loadProbe.adaptedHeavyCap(
                config.maxHeavySessionsGlobal(),
                config.adaptiveTpsEnabled(),
                config.adaptiveLowTpsThreshold(),
                config.adaptiveCriticalTpsThreshold());
    }
}
