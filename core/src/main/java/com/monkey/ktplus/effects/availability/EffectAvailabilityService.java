package com.monkey.ktplus.effects.availability;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class EffectAvailabilityService {
    private final Logger logger;
    private final Set<String> disabled = ConcurrentHashMap.newKeySet();
    private final File file;

    public EffectAvailabilityService(JavaPlugin plugin) {
        this(Objects.requireNonNull(plugin, "plugin").getDataFolder(), plugin.getLogger());
    }

    public EffectAvailabilityService(File dataFolder, Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataFolder, "dataFolder");
        this.file = new File(dataFolder, "disabled-effects.yml");
        reload();
    }

    public void reload() {
        disabled.clear();
        if (!file.exists()) {
            save();
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getStringList("disabled")) {
            if (id == null || id.isBlank()) {
                continue;
            }
            disabled.add(normalize(id));
        }
    }

    public boolean isEnabled(String effectId) {
        return !disabled.contains(normalize(effectId));
    }

    public boolean isDisabled(String effectId) {
        return disabled.contains(normalize(effectId));
    }

    public Set<String> disabledIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(disabled));
    }

    public Collection<String> filterEnabled(Collection<String> ids) {
        ArrayList<String> result = new ArrayList<>();
        for (String id : ids) {
            if (isEnabled(id)) {
                result.add(id);
            }
        }
        return result;
    }

    public Collection<String> filterDisabled(Collection<String> knownIds) {
        ArrayList<String> result = new ArrayList<>();
        for (String id : knownIds) {
            if (isDisabled(id)) {
                result.add(id);
            }
        }
        return result;
    }

    public DisableResult disable(String effectId, boolean known) {
        String id = normalize(effectId);
        if (!known) {
            return DisableResult.UNKNOWN;
        }
        if (!disabled.add(id)) {
            return DisableResult.ALREADY_DISABLED;
        }
        save();
        return DisableResult.DISABLED;
    }

    public EnableResult enable(String effectId, boolean known) {
        String id = normalize(effectId);
        if (!known) {
            return EnableResult.UNKNOWN;
        }
        if (!disabled.remove(id)) {
            return EnableResult.ALREADY_ENABLED;
        }
        save();
        return EnableResult.ENABLED;
    }

    private void save() {
        FileConfiguration yaml = new YamlConfiguration();
        ArrayList<String> list = new ArrayList<>(disabled);
        Collections.sort(list);
        yaml.set("disabled", list);
        try {
            File folder = file.getParentFile();
            if (folder != null && !folder.exists() && !folder.mkdirs()) {
                logger.warning("[Availability] Could not create data folder for disabled-effects.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            logger.warning("[Availability] Failed to save disabled-effects.yml: " + ex.getMessage());
        }
    }

    private static String normalize(@Nullable String effectId) {
        return effectId == null ? "" : effectId.trim().toLowerCase(Locale.ROOT);
    }

    public enum DisableResult {
        DISABLED,
        ALREADY_DISABLED,
        UNKNOWN
    }

    public enum EnableResult {
        ENABLED,
        ALREADY_ENABLED,
        UNKNOWN
    }
}
