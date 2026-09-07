package com.monkey.ktplus.config.validate;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigIntegrityChecker {
    private final JavaPlugin plugin;
    private final Logger logger;

    public ConfigIntegrityChecker(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
    }

    public void validate() {
        List<String> issues = new ArrayList<String>();
        issues.addAll(checkEconomy());
        issues.addAll(checkDatabase());
        issues.addAll(checkWorldGuard());
        for (String issue : issues) {
            logger.warning("[Config] " + issue);
        }
        if (issues.isEmpty()) {
            logger.info("[Config] Configurate integrity check passed");
        }
    }

    private List<String> checkEconomy() {
        List<String> issues = new ArrayList<String>();
        ConfigurationNode root = load("economy.yml");
        if (root == null) {
            return issues;
        }
        String provider = root.node("provider").getString("");
        if (provider == null || provider.trim().isEmpty()) {
            provider = root.node("use-internal").getBoolean(true) ? "KILLCOINS" : "VAULT";
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        if (!"KILLCOINS".equals(normalized) && !"VAULT".equals(normalized)) {
            issues.add("economy.provider must be KILLCOINS or VAULT (got '" + provider + "')");
        }
        return issues;
    }

    private List<String> checkDatabase() {
        List<String> issues = new ArrayList<String>();
        ConfigurationNode root = load("database.yml");
        if (root == null) {
            return issues;
        }
        String type = root.node("database", "type").getString("sqlite");
        if (type == null) {
            type = "sqlite";
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if (!"sqlite".equals(normalized)
                && !"mysql".equals(normalized)
                && !"mariadb".equals(normalized)
                && !"postgresql".equals(normalized)
                && !"postgres".equals(normalized)) {
            issues.add("database.type unsupported: '" + type + "'");
        }
        return issues;
    }

    private List<String> checkWorldGuard() {
        List<String> issues = new ArrayList<String>();
        ConfigurationNode root = load("config.yml");
        if (root == null) {
            return issues;
        }
        String mode = root.node("worldguard", "block-changes").getString("RESPECT");
        if (mode == null) {
            mode = "RESPECT";
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if (!"RESPECT".equals(normalized) && !"BYPASS".equals(normalized) && !"IGNORE".equals(normalized)) {
            issues.add("worldguard.block-changes should be RESPECT, BYPASS, or IGNORE (got '" + mode + "')");
        }
        return issues;
    }

    private ConfigurationNode load(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.isFile()) {
            return null;
        }
        try {
            return YamlConfigurationLoader.builder().path(file.toPath()).build().load();
        } catch (Exception error) {
            logger.warning("[Config] Unable to parse " + fileName + " with Configurate: " + error.getMessage());
            return null;
        }
    }
}
