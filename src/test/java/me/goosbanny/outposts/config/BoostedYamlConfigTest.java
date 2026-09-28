package me.goosbanny.outposts.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class BoostedYamlConfigTest {

    @AfterEach
    public void cleanup() {
        System.gc();
        System.runFinalization();
    }

    @Test
    @DisplayName("Root configs auto-update missing keys while preserving user values")
    public void testRootConfigAutoUpdatesMissingKeys(@TempDir Path tempDir) throws IOException {
        File configFile = tempDir.resolve("config.yml").toFile();

        // User config with custom value and missing keys
        String userContent = """
                # User customized config
                config-version: 1
                debug: true
                custom_user_key: "hello"
                """;
        Files.writeString(configFile.toPath(), userContent, StandardCharsets.UTF_8);

        // Default template stream with other keys
        String defaultContent = """
                # Default config
                config-version: 1
                debug: false
                storage:
                  type: "H2"
                  flush_interval_seconds: 15
                """;
        ByteArrayInputStream defaults = new ByteArrayInputStream(defaultContent.getBytes(StandardCharsets.UTF_8));

        YamlDocument doc = BoostedYamlFactory.createRootDocument(configFile, defaults);

        // Assert user custom value is preserved
        assertTrue(doc.getBoolean("debug"));
        assertEquals("hello", doc.getString("custom_user_key"));

        // Assert missing defaults were automatically merged
        assertTrue(doc.contains("storage.type"));
        assertEquals("H2", doc.getString("storage.type"));
        assertEquals(15, doc.getInt("storage.flush_interval_seconds"));
    }

    @Test
    @DisplayName("Arena files do not get polluted with unneeded template keys on load")
    public void testArenaConfigSelectiveUpdateDoesNotPollute(@TempDir Path tempDir) throws IOException {
        File arenaFile = tempDir.resolve("custom_outpost.yml").toFile();

        // Minimal user outpost file without dynamic_locations
        String userArenaContent = """
                # Minimal outpost
                config-version: 1
                id: custom_outpost
                meta:
                  name: "<gold>Custom Outpost</gold>"
                geometry:
                  world: "world"
                  min:
                    x: 0
                    y: 60
                    z: 0
                  max:
                    x: 20
                    y: 80
                    z: 20
                mechanics:
                  enabled: true
                  mode: STANDARD_HILL
                  speed:
                    percent_per_second: 5.0
                """;
        Files.writeString(arenaFile.toPath(), userArenaContent, StandardCharsets.UTF_8);

        // Template containing lots of default sections (like dynamic_locations)
        String templateContent = """
                config-version: 1
                id: default
                dynamic_locations:
                  enabled: true
                  switch_mode: INTERVAL
                """;
        ByteArrayInputStream templateIn = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));

        YamlDocument arenaDoc = BoostedYamlFactory.createArenaDocument(arenaFile, templateIn);

        // Verify existing keys are intact
        assertEquals("custom_outpost", arenaDoc.getString("id"));
        assertEquals(5.0, arenaDoc.getDouble("mechanics.speed.percent_per_second"));

        // Verify template section was NOT injected into bespoke arena
        assertFalse(arenaDoc.contains("dynamic_locations"), "Arena config must not be polluted with template keys");
    }

    @Test
    @DisplayName("Arena files execute path relocations across schema versions")
    public void testArenaRelocationsAcrossVersions(@TempDir Path tempDir) throws IOException {
        File arenaFile = tempDir.resolve("old_arena.yml").toFile();

        // Old arena file with config-version: 1 and old path
        String oldArenaContent = """
                config-version: 1
                id: old_arena
                mechanics:
                  anti_cheese:
                    line_of_sight: false
                """;
        Files.writeString(arenaFile.toPath(), oldArenaContent, StandardCharsets.UTF_8);

        // Defaults for version 2 with relocation registered
        String v2Defaults = "config-version: 2\n";
        UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setVersioning(new BasicVersioning("config-version"))
                .setKeepAll(true)
                .addRelocation("2", "mechanics.anti_cheese.line_of_sight", "mechanics.anti_cheese.los", '.')
                .build();

        YamlDocument doc = YamlDocument.create(
                arenaFile,
                new ByteArrayInputStream(v2Defaults.getBytes(StandardCharsets.UTF_8)),
                BoostedYamlFactory.createGeneralSettings(),
                BoostedYamlFactory.createLoaderSettings(),
                BoostedYamlFactory.createDumperSettings(),
                updaterSettings
        );

        // Verify version updated to 2
        assertEquals(2, doc.getInt("config-version"));

        // Verify old path was relocated to new path
        assertFalse(doc.contains("mechanics.anti_cheese.line_of_sight"));
        assertTrue(doc.contains("mechanics.anti_cheese.los"));
        assertFalse(doc.getBoolean("mechanics.anti_cheese.los"));
    }

    @Test
    @DisplayName("ArenaSerializer creates, loads, and saves arena without corruption")
    public void testArenaSerializerCreateLoadSave(@TempDir Path tempDir) throws IOException {
        File templateFile = new File("src/main/resources/outposts/default.yml");
        File targetFile = tempDir.resolve("serialized_arena.yml").toFile();

        ArenaSerializer serializer = new ArenaSerializer(
                null,
                null,
                new FoliaCompatScheduler(null),
                new LangManager(null)
        );

        serializer.createFromTemplate(
                templateFile,
                targetFile,
                "test_arena",
                "world",
                10, 64, 10,
                30, 80, 30,
                new Location(null, 20.0, 65.0, 20.0, 45.0f, 0.0f),
                OccupancyMode.TEAM,
                CaptureModeType.STANDARD_HILL
        );

        assertTrue(targetFile.exists());

        // Load arena
        OutpostArena loaded = serializer.loadFromFile(targetFile);
        assertNotNull(loaded);
        assertEquals("test_arena", loaded.getId());
        assertEquals("world", loaded.getWorldName());
        assertEquals(10, loaded.getMinX());
        assertEquals(30, loaded.getMaxX());
        assertEquals(OccupancyMode.TEAM, loaded.getOccupancyMode());
        assertEquals(CaptureModeType.STANDARD_HILL, loaded.getCaptureModeType());

        // Save arena back to file
        serializer.saveToFile(loaded, targetFile);

        // Reload to verify persistence
        OutpostArena reloaded = serializer.loadFromFile(targetFile);
        assertNotNull(reloaded);
        assertEquals("test_arena", reloaded.getId());
        assertEquals(10, reloaded.getMinX());
        assertEquals(30, reloaded.getMaxX());
    }
}
