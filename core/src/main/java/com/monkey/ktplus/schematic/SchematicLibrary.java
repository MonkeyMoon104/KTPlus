package com.monkey.ktplus.schematic;

import com.monkey.ktplus.schematic.parse.SchematicParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class SchematicLibrary {
    private static final String ROOT_FOLDER = "schematics";
    private static final String MODERN_FOLDER = "modern";
    private static final String README_RESOURCE = "schematics/README.txt";

    private final File pluginDataFolder;
    private final Logger logger;
    private final Function<String, InputStream> resourceLoader;
    private final Map<SchematicKey, SchematicModel> cache = new ConcurrentHashMap<>();

    public SchematicLibrary(JavaPlugin plugin) {
        this(
                Objects.requireNonNull(plugin, "plugin").getDataFolder(),
                plugin.getLogger(),
                plugin::getResource);
    }

    SchematicLibrary(File pluginDataFolder, Logger logger, Function<String, InputStream> resourceLoader) {
        this.pluginDataFolder = Objects.requireNonNull(pluginDataFolder, "pluginDataFolder");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
    }

    public void ensureFolders() {
        File root = rootFolder();
        File modern = modernFolder();
        if (!root.exists() && !root.mkdirs()) {
            logger.warning("[Schematic] Could not create folder: " + root.getAbsolutePath());
        }
        if (!modern.exists() && !modern.mkdirs()) {
            logger.warning("[Schematic] Could not create folder: " + modern.getAbsolutePath());
        }
        copyReadmeIfMissing(new File(root, "README.txt"));
    }

    public LoadSummary load() {
        ensureFolders();
        cache.clear();
        int modernLoaded = scanFolder(SchematicFormat.MODERN, modernFolder(), ".schem");
        logger.info("[Schematic] Loaded " + modernLoaded + " modern schematics");
        return new LoadSummary(modernLoaded, cache.size());
    }

    public LoadSummary reload() {
        return load();
    }

    public Optional<SchematicModel> find(SchematicFormat format, String id) {
        return Optional.ofNullable(cache.get(new SchematicKey(format, id)));
    }

    public Optional<SchematicModel> modern(String id) {
        return find(SchematicFormat.MODERN, id);
    }

    public Map<SchematicKey, SchematicModel> snapshot() {
        return Collections.unmodifiableMap(cache);
    }

    public File rootFolder() {
        return new File(pluginDataFolder, ROOT_FOLDER);
    }

    public File modernFolder() {
        return new File(rootFolder(), MODERN_FOLDER);
    }

    private int scanFolder(SchematicFormat format, File folder, String extension) {
        File[] files = folder.listFiles((dir, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(extension));
        if (files == null || files.length == 0) {
            return 0;
        }
        int loaded = 0;
        for (File file : files) {
            String id = fileNameWithoutExtension(file.getName());
            SchematicLoadResult result = SchematicParser.parse(file.toPath(), format, id);
            if (!result.success() || result.model() == null) {
                logger.warning("[Schematic] failed " + file.getAbsolutePath() + ": " + result.error());
                continue;
            }
            SchematicKey key = new SchematicKey(format, id);
            if (cache.containsKey(key)) {
                logger.warning("[Schematic] duplicate id '" + id + "' in " + format.name().toLowerCase(Locale.ROOT)
                        + ", replacing previous entry");
            }
            cache.put(key, result.model());
            loaded++;
        }
        return loaded;
    }

    private void copyReadmeIfMissing(File target) {
        if (target.exists()) {
            return;
        }
        try (InputStream input = resourceLoader.apply(README_RESOURCE)) {
            if (input == null) {
                logger.warning("[Schematic] bundled README missing: " + README_RESOURCE);
                return;
            }
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            logger.log(Level.WARNING, "[Schematic] failed to copy README", error);
        }
    }

    private static String fileNameWithoutExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return name.toLowerCase(Locale.ROOT);
        }
        return name.substring(0, dot).toLowerCase(Locale.ROOT);
    }

    public static final class LoadSummary {
        private final int modernLoaded;
        private final int totalCached;

        public LoadSummary(int modernLoaded, int totalCached) {
            this.modernLoaded = modernLoaded;
            this.totalCached = totalCached;
        }

        public int modernLoaded() {
            return modernLoaded;
        }

        public int totalCached() {
            return totalCached;
        }
    }

    public static SchematicLibrary createAndLoad(JavaPlugin plugin) {
        SchematicLibrary library = new SchematicLibrary(plugin);
        library.load();
        return library;
    }
}
