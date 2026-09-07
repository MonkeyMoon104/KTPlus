package com.monkey.ktplus.effects.list.fireworks.animation;

import java.util.function.BooleanSupplier;

public interface FireworksScheduler {
    void runLater(long delayTicks, Runnable action);

    void runTimer(long delayTicks, long periodTicks, BooleanSupplier action);
}
