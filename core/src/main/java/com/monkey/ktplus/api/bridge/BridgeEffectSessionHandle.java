package com.monkey.ktplus.api.bridge;

import com.monkey.ktplus.api.session.EffectSessionHandle;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.task.CancellationReason;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.bukkit.Location;

public final class BridgeEffectSessionHandle implements EffectSessionHandle {
    private final EffectSession session;

    public BridgeEffectSessionHandle(EffectSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    public UUID id() {
        return session.id();
    }

    @Override
    public UUID playerId() {
        return session.playerId();
    }

    @Override
    public String effectId() {
        return session.effectId();
    }

    @Override
    public boolean heavy() {
        return session.heavy();
    }

    @Override
    public boolean cosmeticMode() {
        return session.cosmeticMode();
    }

    @Override
    public boolean active() {
        return session.active();
    }

    @Override
    public Location origin() {
        return session.origin();
    }

    @Override
    public void onCleanup(Runnable hook) {
        session.onCleanup(hook);
    }

    @Override
    public void runLater(long delayTicks, Runnable action) {
        session.runLater(delayTicks, action);
    }

    @Override
    public void runTimer(long delayTicks, long periodTicks, BooleanSupplier action) {
        session.runTimer(delayTicks, periodTicks, action);
    }

    @Override
    public void cancel() {
        session.cancel(CancellationReason.PLUGIN_DISABLE);
    }
}
