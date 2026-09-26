package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.bridge.BridgeEffectSessionHandle;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.service.RuntimeService;
import com.monkey.ktplus.api.session.EffectSessionHandle;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.runtime.EffectPlayPipeline;
import com.monkey.ktplus.effects.runtime.EffectRuntime;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.task.CancellationReason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class BridgeRuntimeService implements RuntimeService {
    private final EffectRuntime runtime;
    private final EffectRegistry registry;
    private volatile EffectPlayPipeline pipeline;

    public BridgeRuntimeService(
            EffectRuntime runtime, EffectRegistry registry, EffectPlayPipeline pipeline) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    public void reload(EffectPlayPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    @Override
    public boolean play(Player killer, Entity victim, Location location, Effect effect) {
        KillEffect killEffect = resolve(effect == null ? null : effect.id());
        return killEffect != null && pipeline.playGated(killer, victim, location, killEffect);
    }

    @Override
    public boolean play(Player killer, Entity victim, Location location, String effectId) {
        KillEffect killEffect = resolve(effectId);
        return killEffect != null && pipeline.playGated(killer, victim, location, killEffect);
    }

    @Override
    public boolean playForced(Player killer, Entity victim, Location location, Effect effect) {
        KillEffect killEffect = resolve(effect == null ? null : effect.id());
        return killEffect != null && pipeline.playForced(killer, victim, location, killEffect);
    }

    @Override
    public boolean playForced(Player killer, Entity victim, Location location, String effectId) {
        KillEffect killEffect = resolve(effectId);
        return killEffect != null && pipeline.playForced(killer, victim, location, killEffect);
    }

    @Override
    public void cancel(Player player) {
        cancel(player.getUniqueId());
    }

    @Override
    public void cancel(UUID playerId) {
        runtime.cancelPlayer(playerId, CancellationReason.PLUGIN_DISABLE);
    }

    @Override
    public void cancelAll() {
        runtime.cancelAll(CancellationReason.PLUGIN_DISABLE);
    }

    @Override
    public int activeSessions(Player player) {
        return activeSessions(player.getUniqueId());
    }

    @Override
    public int activeSessions(UUID playerId) {
        return runtime.sessionCount(playerId);
    }

    @Override
    public int heavySessions() {
        return runtime.heavySessionCount();
    }

    @Override
    public Collection<EffectSessionHandle> sessions(Player player) {
        return sessions(player.getUniqueId());
    }

    @Override
    public Collection<EffectSessionHandle> sessions(UUID playerId) {
        Collection<EffectSession> sessions = runtime.sessionsFor(playerId);
        ArrayList<EffectSessionHandle> handles = new ArrayList<>(sessions.size());
        for (EffectSession session : sessions) {
            handles.add(new BridgeEffectSessionHandle(session));
        }
        return handles;
    }

    @Override
    public Optional<EffectSessionHandle> session(UUID sessionId) {
        return runtime.session(sessionId).map(BridgeEffectSessionHandle::new);
    }

    private @org.jspecify.annotations.Nullable KillEffect resolve(
            @org.jspecify.annotations.Nullable String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return null;
        }
        return registry.find(effectId).orElse(null);
    }
}
