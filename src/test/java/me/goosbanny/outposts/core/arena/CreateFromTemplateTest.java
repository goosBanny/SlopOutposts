package me.goosbanny.outposts.core.arena;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.config.ArenaSerializer;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateFromTemplateTest {

    @Test
    public void testCreateFromTemplateWithModeAndOccupancy(@TempDir Path tempDir) throws IOException {
        File targetFile = tempDir.resolve("citadel.yml").toFile();
        File dummyTemplate = new File("src/main/resources/outposts/default.yml");

        // Use mock/dummy dependencies for serializer
        ArenaSerializer serializer = new ArenaSerializer(
                null,
                null,
                new FoliaCompatScheduler(null),
                new LangManager(null)
        );

        serializer.createFromTemplate(
                dummyTemplate,
                targetFile,
                "citadel",
                "custom_world",
                -10, 60, -10,
                10, 80, 10,
                new Location(null, 0.5, 65.0, 0.5, 90.0f, 0.0f),
                OccupancyMode.SOLO,
                CaptureModeType.TUG_OF_WAR
        );

        assertTrue(targetFile.exists());
        String content = Files.readString(targetFile.toPath());

        assertTrue(content.contains("id: citadel"));
        assertTrue(content.contains("world: custom_world"));
        assertTrue(content.contains("occupancy_mode: SOLO"));
        assertTrue(content.contains("mode: TUG_OF_WAR"));
        assertTrue(content.contains("mode_settings:"));
        assertTrue(content.contains("standard_hill:"));
        assertTrue(content.contains("tug_of_war:"));
        assertTrue(content.contains("ticket_accumulation:"));
        assertTrue(content.contains("name: <#E13148><bold>Citadel Outpost</bold></#E13148>"));
        assertTrue(content.contains("neutral_anchor_percent: 50.0"));
        assertTrue(content.contains("contested_advantage_scaling: 0.5"));

        File outpostTarget = tempDir.resolve("outpost.yml").toFile();
        serializer.createFromTemplate(
                dummyTemplate,
                outpostTarget,
                "outpost",
                "custom_world",
                0, 60, 0,
                10, 80, 10,
                new Location(null, 0.5, 65.0, 0.5, 0.0f, 0.0f),
                OccupancyMode.SOLO,
                CaptureModeType.STANDARD_HILL
        );
        String outpostContent = Files.readString(outpostTarget.toPath());
        assertTrue(outpostContent.contains("name: <#E13148><bold>Outpost</bold></#E13148>"));
        assertFalse(outpostContent.contains("Outpost Outpost"));
    }
}
