package com.monkey.ktplus.lang;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.util.text.TextFormatter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class LangService {
    private static final Pattern PACK_FILE = Pattern.compile("^[A-Z]{2}\\.yml$");
    private static final String SCHEMA_RESOURCE = "lang/EN.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File langFolder;
    private volatile String defaultLanguage = "EN";
    private volatile Map<String, String> schema = Map.of();
    private volatile Map<String, Map<String, String>> packs = Map.of();
    private volatile ConfigSnapshot config;

    public LangService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.langFolder = new File(plugin.getDataFolder(), "lang");
    }

    public void bindConfig(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
        this.defaultLanguage = normalizeCode(config.defaultLanguage());
        reload();
    }

    public void reload() {
        ensureFolderAndDefaults();
        Map<String, String> schemaKeys = loadSchemaFromJar();
        this.schema = schemaKeys;
        Map<String, Map<String, String>> loaded = new ConcurrentHashMap<>();
        File[] files = langFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                String name = file.getName();
                if (!PACK_FILE.matcher(name).matches()) {
                    logger.warning("[Lang] Ignoring invalid lang file name: " + name
                            + " (expected EN.yml, IT.yml, ...)");
                    continue;
                }
                String code = name.substring(0, 2);
                Map<String, String> flat = flattenYaml(YamlConfiguration.loadConfiguration(file));
                if (flat.isEmpty()) {
                    logger.warning("[Lang] " + name + " is empty or invalid; falling back to EN");
                    continue;
                }
                loaded.put(code, mergeWithSchema(code, flat, schemaKeys));
            }
        }
        if (!loaded.containsKey("EN")) {
            loaded.put("EN", new LinkedHashMap<>(schemaKeys));
        }
        this.packs = loaded;
    }

    public String defaultLanguage() {
        return defaultLanguage;
    }

    public String languageCode(@Nullable CommandSender sender) {
        if (sender instanceof Player player) {
            try {
                Locale locale = player.locale();
                if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
                    return normalizeCode(locale.getLanguage());
                }
            } catch (Throwable ignored) {
            }
        }
        return defaultLanguage;
    }

    public String message(@Nullable CommandSender sender, String key) {
        String overlay = overlayMessage(key);
        if (overlay != null) {
            return TextFormatter.color(prefix(sender) + overlay);
        }
        return TextFormatter.color(prefix(sender) + raw(sender, key));
    }

    public String message(String key) {
        return message(null, key);
    }

    public String raw(@Nullable CommandSender sender, String key) {
        return resolve(languageCode(sender), key);
    }

    public String raw(String languageCode, String key) {
        return resolve(normalizeCode(languageCode), key);
    }

    public String effectName(@Nullable CommandSender sender, String effectId, String fallback) {
        String value = resolve(languageCode(sender), "effects." + effectId + ".name");
        if (!value.isBlank()) {
            return value;
        }
        String overlay = overlayEffectName(effectId);
        return overlay == null || overlay.isBlank() ? fallback : overlay;
    }

    public String effectDescription(@Nullable CommandSender sender, String effectId) {
        return resolve(languageCode(sender), "effects." + effectId + ".description");
    }

    public String guiText(@Nullable CommandSender sender, String path, String fallback) {
        String value = resolve(languageCode(sender), "gui." + path);
        if (!value.isBlank()) {
            return value;
        }
        String overlay = overlayGuiText(path);
        return overlay == null || overlay.isBlank() ? fallback : overlay;
    }

    public List<String> guiTextList(@Nullable CommandSender sender, String path, List<String> fallback) {
        String code = languageCode(sender);
        Map<String, String> pack = packs.getOrDefault(code, packs.getOrDefault("EN", Map.of()));
        ArrayList<String> values = new ArrayList<>();
        for (int i = 0; ; i++) {
            String key = "gui." + path + "." + i;
            if (!pack.containsKey(key) && !schema.containsKey(key)) {
                break;
            }
            values.add(resolve(code, key));
        }
        if (!values.isEmpty()) {
            return values;
        }
        ConfigSnapshot snapshot = config;
        if (snapshot != null) {
            List<String> overlay = snapshot.guiTextList(path, List.of());
            if (!overlay.isEmpty()) {
                return overlay;
            }
        }
        return fallback;
    }

    public String categoryDisplayName(@Nullable CommandSender sender, String categoryId, String fallback) {
        String value = resolve(languageCode(sender), "gui.categories." + categoryId + ".display-name");
        if (!value.isBlank()) {
            return value;
        }
        String overlay = overlayCategoryName(categoryId);
        return overlay == null || overlay.isBlank() ? fallback : overlay;
    }

    public static boolean isValidPackFileName(String name) {
        return name != null && PACK_FILE.matcher(name).matches();
    }

    private String resolve(String code, String key) {
        Map<String, String> pack = packs.get(code);
        if (pack != null) {
            String value = pack.get(key);
            if (value != null) {
                return value;
            }
        }
        Map<String, String> en = packs.get("EN");
        if (en != null) {
            String value = en.get(key);
            if (value != null) {
                return value;
            }
        }
        String schemaValue = schema.get(key);
        return schemaValue == null ? "" : schemaValue;
    }

    private String prefix(@Nullable CommandSender sender) {
        String overlay = overlayMessage("prefix");
        if (overlay != null) {
            return overlay;
        }
        return resolve(languageCode(sender), "prefix");
    }

    private @Nullable String overlayMessage(String key) {
        ConfigSnapshot snapshot = config;
        if (snapshot == null) {
            return null;
        }
        return snapshot.messageOverlay(key);
    }

    private @Nullable String overlayEffectName(String effectId) {
        ConfigSnapshot snapshot = config;
        if (snapshot == null) {
            return null;
        }
        return snapshot.effectNameOverlay(effectId);
    }

    private @Nullable String overlayGuiText(String path) {
        ConfigSnapshot snapshot = config;
        if (snapshot == null) {
            return null;
        }
        return snapshot.guiTextOverlay(path);
    }

    private @Nullable String overlayCategoryName(String categoryId) {
        ConfigSnapshot snapshot = config;
        if (snapshot == null) {
            return null;
        }
        return snapshot.categoryDisplayNameOverlay(categoryId);
    }

    private void ensureFolderAndDefaults() {
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            logger.warning("[Lang] Could not create lang folder");
        }
        File enFile = new File(langFolder, "EN.yml");
        if (!enFile.exists()) {
            plugin.saveResource(SCHEMA_RESOURCE, false);
        }
    }

    private Map<String, String> loadSchemaFromJar() {
        try (InputStream stream = plugin.getResource(SCHEMA_RESOURCE)) {
            if (stream == null) {
                logger.warning("[Lang] Missing jar resource " + SCHEMA_RESOURCE);
                File enFile = new File(langFolder, "EN.yml");
                if (enFile.exists()) {
                    return flattenYaml(YamlConfiguration.loadConfiguration(enFile));
                }
                return Map.of();
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            return flattenYaml(yaml);
        } catch (IOException ex) {
            logger.warning("[Lang] Failed to load schema: " + ex.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> mergeWithSchema(String code, Map<String, String> flat, Map<String, String> schemaKeys) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();
        Set<String> missing = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : schemaKeys.entrySet()) {
            String key = entry.getKey();
            if (flat.containsKey(key)) {
                merged.put(key, flat.get(key));
            } else {
                merged.put(key, entry.getValue());
                missing.add(key);
            }
        }
        int unknown = 0;
        for (String key : flat.keySet()) {
            if (!schemaKeys.containsKey(key)) {
                unknown++;
            }
        }
        if (!missing.isEmpty() && !"EN".equals(code)) {
            logger.warning("[Lang] " + code + ".yml missing " + missing.size()
                    + " keys; falling back to EN for those");
        }
        if (unknown > 0) {
            logger.warning("[Lang] " + code + ".yml has " + unknown + " unknown keys (ignored)");
        }
        return merged;
    }

    static Map<String, String> flattenYaml(FileConfiguration yaml) {
        LinkedHashMap<String, String> flat = new LinkedHashMap<>();
        flattenSection(yaml, "", flat);
        return flat;
    }

    private static void flattenSection(ConfigurationSection section, String prefix, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                flattenSection(child, path, out);
            } else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    out.put(path + "." + i, item == null ? "" : String.valueOf(item));
                }
            } else if (value != null) {
                out.put(path, String.valueOf(value));
            }
        }
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "EN";
        }
        String trimmed = code.trim();
        if (trimmed.length() >= 2) {
            return trimmed.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return "EN";
    }
}
