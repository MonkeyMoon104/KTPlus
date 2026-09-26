package com.monkey.ktplus.effects.runtime;

import com.monkey.ktplus.api.bridge.ApiEvents;
import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.event.KillEffectPreTriggerEvent;
import com.monkey.ktplus.api.event.KillEffectTriggerEvent;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.cooldown.CooldownService;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.availability.EffectAvailabilityService;
import com.monkey.ktplus.effects.condition.EffectConditionService;
import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.event.random.RandomEventService;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class EffectPlayPipeline {
    private final ConfigSnapshot config;
    private final EffectConditionService conditions;
    private final EffectAvailabilityService availability;
    private final EffectAccessService access;
    private final CooldownService cooldowns;
    private final EffectRuntime runtime;
    private final RandomEventService randomEvents;

    public EffectPlayPipeline(
            ConfigSnapshot config,
            EffectConditionService conditions,
            EffectAvailabilityService availability,
            EffectAccessService access,
            CooldownService cooldowns,
            EffectRuntime runtime,
            RandomEventService randomEvents) {
        this.config = Objects.requireNonNull(config, "config");
        this.conditions = Objects.requireNonNull(conditions, "conditions");
        this.availability = Objects.requireNonNull(availability, "availability");
        this.access = Objects.requireNonNull(access, "access");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.randomEvents = Objects.requireNonNull(randomEvents, "randomEvents");
    }

    public EffectPlayPipeline withConfig(ConfigSnapshot config) {
        return new EffectPlayPipeline(
                config, conditions, availability, access, cooldowns, runtime, randomEvents);
    }

    public boolean playGated(Player killer, Entity victim, Location location, KillEffect effect) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(effect, "effect");
        if (!conditions.allowsWorld(killer) || !conditions.allowsGameMode(killer)) {
            return false;
        }
        boolean playerKill = victim instanceof Player;
        if (!playerKill && !config.effectsOnMobs()) {
            return false;
        }
        if (!availability.isEnabled(effect.definition().id())) {
            return false;
        }
        if (!conditions.allowsTrigger(killer, effect.definition())) {
            return false;
        }
        if (!access.canActivate(killer, effect.definition())) {
            return false;
        }
        if (!cooldowns.ready(killer.getUniqueId(), CooldownService.EFFECT_KEY)) {
            return false;
        }
        Effect apiEffect = EffectMappings.toApi(effect.definition());
        KillEffectPreTriggerEvent pre = new KillEffectPreTriggerEvent(killer, victim, location, apiEffect);
        ApiEvents.call(pre);
        if (pre.isCancelled()) {
            return false;
        }
        cooldowns.set(
                killer.getUniqueId(),
                CooldownService.EFFECT_KEY,
                Duration.ofMillis(config.effectCooldownMillis()));
        if (!runtime.start(killer, victim, location, effect)) {
            cooldowns.clearKey(killer.getUniqueId(), CooldownService.EFFECT_KEY);
            return false;
        }
        ApiEvents.call(new KillEffectTriggerEvent(killer, victim, location, apiEffect));
        randomEvents.tryTrigger(killer, location);
        return true;
    }

    public boolean playForced(Player killer, Entity victim, Location location, KillEffect effect) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(effect, "effect");
        return runtime.start(killer, victim, location, effect);
    }
}
