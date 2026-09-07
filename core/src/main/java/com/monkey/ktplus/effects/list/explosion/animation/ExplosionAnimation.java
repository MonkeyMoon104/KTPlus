package com.monkey.ktplus.effects.list.explosion.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.list.explosion.animation.util.ExplosionUtils;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import org.bukkit.Location;

public final class ExplosionAnimation {
    private ExplosionAnimation() {}

    public static void launch(EffectSession session, VisualEffectService visuals, EffectContext context, Location center) {
        for (int i = 0; i < 6; i++) {
            ExplosionUtils.launchCosmeticTnt(session, visuals, context, center);
        }
    }
}
