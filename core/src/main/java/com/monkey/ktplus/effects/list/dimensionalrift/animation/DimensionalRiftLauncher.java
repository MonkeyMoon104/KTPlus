package com.monkey.ktplus.effects.list.dimensionalrift.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;

public final class DimensionalRiftLauncher {
    private DimensionalRiftLauncher() {}

    public static void launch(EffectSession session, VisualEffectService visuals, EffectContext context) {
        DimensionalRiftAnimation.start(session, visuals, context);
    }
}
