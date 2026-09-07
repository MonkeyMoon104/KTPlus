package com.monkey.ktplus.effects.list.aurafarming.animation.util;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.util.item.PotionTypes;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

public final class AuraBoostApplier {
    private AuraBoostApplier() {}

    public static void apply(EffectContext context, Player killer) {
        ConfigurationSection section = context.config().effectSection("aurafarming");
        ConfigurationSection boost = resolveBoostSection(section);
        if (boost != null) {
            applyConfiguredBoosts(boost, killer);
            return;
        }
        applyDefaultBoosts(killer);
    }

    private static @Nullable ConfigurationSection resolveBoostSection(@Nullable ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ConfigurationSection perks = section.getConfigurationSection("perks");
        if (perks != null) {
            ConfigurationSection boost = perks.getConfigurationSection("boost");
            if (boost != null) {
                return boost;
            }
        }
        ConfigurationSection auraSettings = section.getConfigurationSection("aura-settings");
        if (auraSettings == null) {
            return null;
        }
        return auraSettings.getConfigurationSection("boost-perks");
    }

    private static void applyConfiguredBoosts(ConfigurationSection boost, Player killer) {
        ConfigurationSection potionBoost = boost.getConfigurationSection("potion");
        if (potionBoost != null) {
            applyPotionBoost(
                    potionBoost.getString("type", "REGENERATION"),
                    potionBoost.getInt("amplifier", 1),
                    potionBoost.getInt("duration", 10),
                    killer);
        }
        ConfigurationSection damageBoost = boost.getConfigurationSection("damage");
        if (damageBoost != null) {
            applyStrengthBoost(damageBoost.getInt("amplifier", 1), damageBoost.getInt("duration", 20), killer);
        }
    }

    private static void applyDefaultBoosts(Player killer) {
        applyPotionBoost("REGENERATION", 2, 10, killer);
        applyStrengthBoost(2, 20, killer);
    }

    private static void applyPotionBoost(String typeName, int amplifier, int duration, Player killer) {
        PotionEffectType potionType = PotionTypes.byKey(typeName);
        if (potionType != null) {
            killer.addPotionEffect(new PotionEffect(potionType, duration * 20, amplifier - 1, true, true));
        }
    }

    private static void applyStrengthBoost(int amplifier, int duration, Player killer) {
        PotionEffectType strength = PotionTypes.strength();
        if (strength != null) {
            killer.addPotionEffect(new PotionEffect(strength, duration * 20, amplifier - 1, true, true));
        }
    }
}
