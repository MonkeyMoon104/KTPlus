package com.monkey.ktplus.effects.list.fireworks.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.list.fireworks.animation.util.FireworksGround;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class FireworksLauncher {
    private FireworksLauncher() {}

    public static void launch(EffectSession session, EffectContext context, VisualEffectService visuals) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(visuals, "visuals");

        Location target = context.location();
        World world = target.getWorld();
        if (world == null) {
            return;
        }

        Player killer = context.killer();
        ConfigurationSection effectSection = context.config().effectSection("fireworks");
        FireworksSettings settings = FireworksSettings.from(effectSection);
        boolean allowStructure = context.config().effectStructure("fireworks", true);
        Location groundCenter = FireworksGround.resolveGroundCenter(target);
        FireworksMarkedTracker tracker =
                new FireworksMarkedTracker(session.temporaryBlocks(), session::allowsWorldMutation);
        FireworksScheduler radarScheduler = FireworksSchedulers.fromSession(session);
        session.onCleanup(() -> PerkActionBar.clear(killer));
        session.runTimer(0L, 5L, () -> {
            if (!session.active() || !killer.isOnline()) {
                PerkActionBar.clear(killer);
                return false;
            }
            PerkActionBar.show(
                    killer,
                    String.format(
                            "&d✦ FIREWORKS &8| &eMARKED &f%d &8| &cHUNTING &f%d",
                            tracker.markedCount(),
                            tracker.huntingCount()));
            return true;
        });

        FireworksRadarWave.start(
                session,
                radarScheduler,
                visuals,
                killer,
                groundCenter,
                settings,
                allowStructure,
                tracker,
                () -> session.handoffFireworksFinale(visuals, killer, settings, tracker, allowStructure));
    }
}
