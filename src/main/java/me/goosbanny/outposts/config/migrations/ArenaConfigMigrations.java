package me.goosbanny.outposts.config.migrations;

import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Manages versioned key relocations and path migrations for outpost arena YAML files.
 */
public final class ArenaConfigMigrations {

    public static final String CURRENT_VERSION = "1";

    private ArenaConfigMigrations() {}

    /**
     * Registers declarative key relocations with the BoostedYAML UpdaterSettings builder.
     * When upgrading an arena file from version X to Y, any registered relocations are applied.
     *
     * Example future migration (v1 -> v2):
     * builder.addRelocation("2", "anti_cheese.line_of_sight", "anti_cheese.los", '.');
     * builder.addRelocation("2", "mechanics.speed.percent_per_second", "mechanics.progression.capture_rate", '.');
     */
    public static void applyRelocations(@NotNull UpdaterSettings.Builder builder) {
        // Relocations registered by target version:
        // builder.addRelocation("2", "old.path", "new.path", '.');
    }
}
