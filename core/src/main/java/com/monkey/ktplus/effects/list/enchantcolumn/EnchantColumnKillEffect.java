package com.monkey.ktplus.effects.list.enchantcolumn;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.list.enchantcolumn.animation.EnchantAnimation;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

public final class EnchantColumnKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;

    public EnchantColumnKillEffect(EffectDefinition definition, VisualEffectService visuals) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        ConfigurationSection section = context.config().effectSection("enchantcolumn");
        ConfigurationSection potion = null;
        if (section != null) {
            ConfigurationSection perks = section.getConfigurationSection("perks");
            if (perks != null) {
                potion = perks.getConfigurationSection("potion");
            }
            if (potion == null) {
                potion = section.getConfigurationSection("effectexplosion");
            }
        }
        PotionEffectType effectType = PotionEffectType.REGENERATION;
        int amplifier = 1;
        int duration = 10;
        if (potion != null) {
            PotionEffectType resolved = PotionTypes.byKey(potion.getString("type", "REGENERATION"));
            if (resolved != null) {
                effectType = resolved;
            }
            amplifier = potion.getInt("amplifier", 1);
            duration = potion.getInt("duration", 10);
        }
        EnchantAnimation.start(session, visuals, context, effectType, amplifier, duration);
    }
}
