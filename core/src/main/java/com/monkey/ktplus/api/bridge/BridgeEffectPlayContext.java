package com.monkey.ktplus.api.bridge;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.session.EffectSessionHandle;
import com.monkey.ktplus.api.spi.EffectPlayContext;
import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.runtime.EffectSession;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class BridgeEffectPlayContext implements EffectPlayContext {
    private final EffectContext context;
    private final Effect effect;
    private final EffectSessionHandle session;

    public BridgeEffectPlayContext(EffectContext context, EffectSession session, Effect effect) {
        this.context = Objects.requireNonNull(context, "context");
        this.effect = Objects.requireNonNull(effect, "effect");
        this.session = new BridgeEffectSessionHandle(Objects.requireNonNull(session, "session"));
    }

    @Override
    public Player killer() {
        return context.killer();
    }

    @Override
    public @Nullable Entity victim() {
        return context.victim();
    }

    @Override
    public Location location() {
        return context.location();
    }

    @Override
    public Effect effect() {
        return effect;
    }

    @Override
    public EffectSessionHandle session() {
        return session;
    }

    @Override
    public boolean cosmeticMode() {
        return session.cosmeticMode();
    }
}
