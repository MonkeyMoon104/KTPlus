package com.monkey.ktplus.access.effect;

import com.monkey.ktplus.api.bridge.ApiEvents;
import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.event.EffectSelectEvent;
import com.monkey.ktplus.api.model.SelectionResult;
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
        Outcome outcome;
        if (access.canActivate(player, definition)) {
            users.selectEffect(player, definition.id());
            outcome = Outcome.SELECTED;
        } else {
            PurchaseResult result = access.unlockForSelection(player, definition);
            if (!result.successful()) {
                outcome = result.price() > 0 ? Outcome.DENIED_FUNDS : Outcome.DENIED_PERMISSION;
            } else {
                users.selectEffect(player, definition.id());
                if (definition.price() > 0 && economy.enabled()) {
                    outcome = Outcome.PURCHASED_AND_SELECTED;
                } else {
                    outcome = Outcome.SELECTED;
                }
            }
        }
        ApiEvents.call(
                new EffectSelectEvent(
                        player.getUniqueId(), player, EffectMappings.toApi(definition), toApi(outcome)));
        return outcome;
    }

    private static SelectionResult toApi(Outcome outcome) {
        return switch (outcome) {
            case SELECTED -> SelectionResult.SELECTED;
            case PURCHASED_AND_SELECTED -> SelectionResult.PURCHASED_AND_SELECTED;
            case DENIED_PERMISSION -> SelectionResult.DENIED_PERMISSION;
            case DENIED_FUNDS -> SelectionResult.DENIED_FUNDS;
        };
    }
}
