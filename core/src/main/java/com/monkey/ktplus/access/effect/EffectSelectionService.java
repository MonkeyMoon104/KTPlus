package com.monkey.ktplus.access.effect;

import com.monkey.ktplus.economy.PurchaseResult;
import com.monkey.ktplus.effects.api.EffectDefinition;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class EffectSelectionService {
    public enum Outcome {
        SELECTED,
        PURCHASED_AND_SELECTED,
        DENIED_PERMISSION,
        DENIED_FUNDS
    }

    private final EffectAccessService access;
    private final EconomyEnabledGate economy;
    private final UserSelectionWriter users;

    public EffectSelectionService(
            EffectAccessService access,
            EconomyEnabledGate economy,
            UserSelectionWriter users) {
        this.access = Objects.requireNonNull(access, "access");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.users = Objects.requireNonNull(users, "users");
    }

    public Outcome select(Player player, EffectDefinition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        if (access.canActivate(player, definition)) {
            users.selectEffect(player, definition.id());
            return Outcome.SELECTED;
        }
        PurchaseResult result = access.unlockForSelection(player, definition);
        if (!result.successful()) {
            return result.price() > 0 ? Outcome.DENIED_FUNDS : Outcome.DENIED_PERMISSION;
        }
        users.selectEffect(player, definition.id());
        if (definition.price() > 0 && economy.enabled()) {
            return Outcome.PURCHASED_AND_SELECTED;
        }
        return Outcome.SELECTED;
    }
}
