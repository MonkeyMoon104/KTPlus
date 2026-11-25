package com.monkey.ktplus.effects.runtime;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksFinaleService;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksMarkedTracker;
import com.monkey.ktplus.effects.list.fireworks.animation.FireworksSettings;
import com.monkey.ktplus.effects.runtime.block.BlockChangeGuard;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import com.monkey.ktplus.hook.particleplus.ParticlePlusHook;
import com.monkey.ktplus.lang.LangService;
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
import java.util.logging.Logger;
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
    private final ParticlePlusHook particlePlusHook;
    private ConfigSnapshot config;
    private LangService lang;

    public EffectRuntime(
            ConfigSnapshot config,
            LangService lang,
            PlatformScheduler scheduler,
            TaskRegistry taskRegistry,
            TemporaryBlockService temporaryBlocks,
            BlockChangeGuard blockChangeGuard) {
        this.config = Objects.requireNonNull(config, "config");
        this.lang = Objects.requireNonNull(lang, "lang");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.taskRegistry = Objects.requireNonNull(taskRegistry, "taskRegistry");
        this.temporaryBlocks = Objects.requireNonNull(temporaryBlocks, "temporaryBlocks");
        this.blockChangeGuard = Objects.requireNonNull(blockChangeGuard, "blockChangeGuard");
        this.fireworksFinale = new FireworksFinaleService(scheduler, entityRegistry, blockChangeGuard);
        this.particlePlusHook = ParticlePlusHook.create(Logger.getLogger("KTPlus-Hooks"));
    }

    public void reload(ConfigSnapshot config, LangService lang, BlockChangeGuard blockChangeGuard) {
        this.config = Objects.requireNonNull(config, "config");
        this.lang = Objects.requireNonNull(lang, "lang");
        this.blockChangeGuard = Objects.requireNonNull(blockChangeGuard, "blockChangeGuard");
    }

    public EffectEntityRegistry entityRegistry() {
        return entityRegistry;
    }

    public void onFinalePlayerChangedWorld(Player player, org.bukkit.World fromWorld) {
        fireworksFinale.onPlayerChangedWorld(player, fromWorld);
    }

    public boolean start(Player killer, Entity victim, Location location, KillEffect effect) {
        return start(killer, victim, location, effect, false);
    }

    /**
     * Private preview for GUI / {@code /kt test}: visible only to {@code killer}, cosmetic-only,
     * and cancels any previous sessions for that player first.
     */
    public boolean startPreview(Player player, KillEffect effect) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(effect, "effect");
        cancelPlayer(player.getUniqueId(), CancellationReason.PREVIEW_REPLACED);
        return start(player, player, player.getLocation(), effect, true);
    }

    private boolean start(
            Player killer, Entity victim, Location location, KillEffect effect, boolean privatePreview) {
        Set<UUID> playerSessions =
                sessionsByPlayer.computeIfAbsent(killer.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
        if (!privatePreview && playerSessions.size() >= config.maxSessionsPerPlayer()) {
            return false;
        }
        boolean heavy = effect.definition().heavy();
        if (!privatePreview && heavy && heavySessions.get() >= heavyCap()) {
            return false;
        }
        UUID sessionId = UUID.randomUUID();
        EffectSession session = new EffectSession(
                sessionId,
                killer.getUniqueId(),
                effect.definition().id(),
                heavy && !privatePreview,
                config.cosmeticMode() || privatePreview,
                privatePreview,
                location,
                scheduler,
                taskRegistry,
                temporaryBlocks,
                blockChangeGuard,
                this);
        sessions.put(sessionId, session);
        playerSessions.add(sessionId);
        if (heavy && !privatePreview) {
            heavySessions.incrementAndGet();
        }

        ParticlePlusHook.Suspension particleSuspension = privatePreview
                ? null
                : particlePlusHook.suspendNearby(
                        location,
                        config.particlePlusTurnOffRadius(),
                        config.turnOffPlayerParticles(),
                        config.turnOffMobParticles(),
                        config.turnOffBlockParticles(),
                        config.turnOffItemParticles());
        if (particleSuspension != null) {
            session.onCleanup(() -> particlePlusHook.resume(particleSuspension));
        }

        try {
            VisualEffectService.pushAudience(privatePreview ? killer : null);
            try {
                effect.execute(new EffectContext(killer, victim, location.clone(), config, lang), session);
            } finally {
                VisualEffectService.clearAudience();
            }
            if (!session.active()) {
                finish(session, CancellationReason.ERROR);
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

    public int sessionCount(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Set<UUID> ids = sessionsByPlayer.get(playerId);
        return ids == null ? 0 : ids.size();
    }

    public int heavySessionCount() {
        return heavySessions.get();
    }

    public java.util.Optional<EffectSession> session(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        return java.util.Optional.ofNullable(sessions.get(sessionId));
    }

    public java.util.Collection<EffectSession> sessionsFor(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Set<UUID> ids = sessionsByPlayer.get(playerId);
        if (ids == null || ids.isEmpty()) {
            return java.util.List.of();
        }
        java.util.ArrayList<EffectSession> result = new java.util.ArrayList<>();
        for (UUID id : ids) {
            EffectSession session = sessions.get(id);
            if (session != null) {
                result.add(session);
            }
        }
        return result;
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