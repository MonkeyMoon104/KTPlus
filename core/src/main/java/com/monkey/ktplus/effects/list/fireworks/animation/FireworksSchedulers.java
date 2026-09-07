package com.monkey.ktplus.effects.list.fireworks.animation;

import com.monkey.ktplus.effects.runtime.EffectSession;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class FireworksSchedulers {
    private FireworksSchedulers() {}

    public static FireworksScheduler fromSession(EffectSession session) {
        Objects.requireNonNull(session, "session");
        return new FireworksScheduler() {
            @Override
            public void runLater(long delayTicks, Runnable action) {
                session.runLater(delayTicks, action);
            }

            @Override
            public void runTimer(long delayTicks, long periodTicks, BooleanSupplier action) {
                session.runTimer(delayTicks, periodTicks, action);
            }
        };
    }
}
