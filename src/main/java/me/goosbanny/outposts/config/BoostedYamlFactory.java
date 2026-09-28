package me.goosbanny.outposts.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.spigot.SpigotSerializer;
import me.goosbanny.outposts.config.migrations.ArenaConfigMigrations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Factory for creating configured BoostedYAML YamlDocument instances.
 * Configures serialization, formatting, and versioned updates.
 */
public final class BoostedYamlFactory {

    private BoostedYamlFactory() {}

    public static GeneralSettings createGeneralSettings() {
        return GeneralSettings.builder()
                .setSerializer(SpigotSerializer.getInstance())
                .build();
    }

    public static LoaderSettings createLoaderSettings() {
        return LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();
    }

    public static DumperSettings createDumperSettings() {
        return DumperSettings.builder()
                .setIndentation(2)
                .build();
    }

    /**
     * Settings for root plugin configs (config.yml, lang.yml, schedules.yml).
     * Fully auto-updates schema: merges missing keys, preserves comments, updates version.
     */
    public static UpdaterSettings createRootUpdaterSettings() {
        return UpdaterSettings.builder()
                .setVersioning(new BasicVersioning("config-version"))
                .setKeepAll(true)
                .build();
    }

    /**
     * Settings for individual arena files (outposts/*.yml).
     * Selective auto-update: performs key relocations without dumping full template options into bespoke outposts.
     */
    public static UpdaterSettings createArenaUpdaterSettings() {
        UpdaterSettings.Builder builder = UpdaterSettings.builder()
                .setVersioning(new BasicVersioning("config-version"))
                .setKeepAll(true);
        ArenaConfigMigrations.applyRelocations(builder);
        return builder.build();
    }

    /**
     * Loads or creates a root plugin YamlDocument with full schema auto-updating.
     */
    public static YamlDocument createRootDocument(
            @NotNull File file,
            @Nullable InputStream defaults
    ) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        return YamlDocument.create(
                file,
                defaults,
                createGeneralSettings(),
                createLoaderSettings(),
                createDumperSettings(),
                createRootUpdaterSettings()
        );
    }

    /**
     * Loads or creates an arena YamlDocument.
     * If the file does not exist on disk, the full template resource is used to generate it with full comments.
     * If the file already exists, a minimal version-defaults stream is used so BoostedYAML executes key relocations
     * and updates version headers without injecting non-critical template options.
     */
    public static YamlDocument createArenaDocument(
            @NotNull File file,
            @Nullable InputStream fullTemplateResource
    ) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            return YamlDocument.create(
                    file,
                    fullTemplateResource,
                    createGeneralSettings(),
                    createLoaderSettings(),
                    createDumperSettings(),
                    createArenaUpdaterSettings()
            );
        }

        // For existing arena files, only pass version tracking defaults to prevent unwanted template key injection
        String arenaDefaults = "config-version: " + ArenaConfigMigrations.CURRENT_VERSION + "\n";
        return YamlDocument.create(
                file,
                new ByteArrayInputStream(arenaDefaults.getBytes(StandardCharsets.UTF_8)),
                createGeneralSettings(),
                createLoaderSettings(),
                createDumperSettings(),
                createArenaUpdaterSettings()
        );
    }
}
