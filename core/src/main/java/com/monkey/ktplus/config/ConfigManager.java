package com.monkey.ktplus.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {
    private static final String[] FILES = {
        "config.yml",
        "messages.yml",
        "effects.yml",
        "economy.yml",
        "gui.yml",
        "database.yml",
        "events.yml",
        "resource-pack.yml",
        "performance.yml"
    };

    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public ConfigSnapshot load() {
        YamlConfiguration[] loaded = new YamlConfiguration[FILES.length];
        for (int i = 0; i < FILES.length; i++) {
            loaded[i] = loadFile(FILES[i]);
        }
        return new ConfigSnapshot(
                loaded[0],
                loaded[1],
                loaded[2],
                loaded[3],
                loaded[4],
                loaded[5],
                loaded[6],
                loaded[7],
                loaded[8]);
    }

    private YamlConfiguration loadFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        InputStream resource = plugin.getResource(name);
        if (resource != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            yaml.setDefaults(defaults);
            yaml.options().copyDefaults(true);
            try {
                yaml.save(file);
            } catch (IOException ignored) {
                
            }
        }
        return yaml;
    }

    public void saveDatabaseType(String type) throws IOException {
        Objects.requireNonNull(type, "type");
        File file = new File(plugin.getDataFolder(), "database.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("database.type", type);
        yaml.save(file);
    }
}
