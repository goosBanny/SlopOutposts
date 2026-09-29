package me.goosbanny.outposts.hook.placeholder;

import dev.dejvokep.boostedyaml.YamlDocument;
import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.ArenaViewSnapshot;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DynamicLocationConfig;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.manager.ArenaManager;
import me.goosbanny.outposts.core.manager.SpatialGridManager;
import me.goosbanny.outposts.core.schedule.ScheduleManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class OutpostsPlaceholderExpansionTest {

    static class TestArena implements OutpostArena {
        private final String id;
        private boolean active;
        private ArenaState state = ArenaState.LOCKED;

        public TestArena(String id, boolean active) {
            this.id = id;
            this.active = active;
        }

        @Override public String getId() { return id; }
        @Override public Component getDisplayName() { return Component.text(id); }
        @Override public String getWorldName() { return "world"; }
        @Override public int getMinX() { return 0; }
        @Override public int getMinY() { return 0; }
        @Override public int getMinZ() { return 0; }
        @Override public int getMaxX() { return 10; }
        @Override public int getMaxY() { return 10; }
        @Override public int getMaxZ() { return 10; }
        @Override public boolean isWithinBounds(int x, int y, int z) { return false; }
        @Override public ArenaState getState() { return state; }
        public void setState(ArenaState state) { this.state = state; }
        @Override public double getProgress() { return 0; }
        @Override public void setProgress(double progress) {}
        @Override public String getControllerTeamId() { return null; }
        @Override public String getControllerTeamName() { return null; }
        @Override public void setController(String teamId, String teamName, UUID capturingPlayer) {}
        @Override public void resetToNeutral() {}
        @Override public String getCappingTeamId() { return null; }
        @Override public int getCapperCount() { return 0; }
        @Override public boolean isContested() { return false; }
        @Override public boolean isLocked() { return state == ArenaState.LOCKED; }
        @Override public long getLockoutRemainingSeconds() { return 0; }
        @Override public Location getCenterLocation() { return null; }
        @Override public Location getWarpLocation() { return null; }
        @Override public void setWarpLocation(Location warpLocation) {}
        @Override public CaptureModeType getCaptureModeType() { return CaptureModeType.STANDARD_HILL; }
        @Override public ArenaView createSnapshot() {
            return new ArenaViewSnapshot(
                    id,
                    Component.text(id),
                    state,
                    0.0,
                    null,
                    null,
                    0,
                    false,
                    0,
                    OccupancyMode.TEAM,
                    -1L,
                    active
            );
        }
        @Override public void tickGameLoop() {}
        @Override public boolean isActive() { return active; }
        @Override public void setActive(boolean active) { this.active = active; }
        @Override public boolean isBoundingParticlesEnabled() { return false; }
        @Override public void setBoundingParticlesEnabled(boolean enabled) {}
        @Override public OccupancyMode getOccupancyMode() { return OccupancyMode.TEAM; }
        @Override public double getMultiplier(String key) { return 1.0; }
        @Override public DynamicLocationConfig getDynamicLocationConfig() { return DynamicLocationConfig.createDisabled(); }
        @Override public ArenaRegion getCurrentRegion() { return null; }
        @Override public void shiftToRegion(ArenaRegion target) {}
        @Override public long getNextShiftSeconds() { return -1; }
        @Override public int getActivationGraceRemainingSeconds() { return 0; }
        @Override public boolean isWarmingUp() { return false; }
        @Override public String getCappingTeamName() { return null; }
        @Override public void setCappingTeam(String teamId, String teamName) {}
        @Override public Map<String, String> getCustomLangOverrides() { return Collections.emptyMap(); }
        @Override public void setCustomLangOverrides(Map<String, String> overrides) {}
        @Override public String getCustomLang(String path) { return null; }
        @Override public boolean isAutoStart() { return false; }
    }

    static class DummyTeamProvider implements TeamRosterProvider {
        @Override public @NotNull String getProviderName() { return "Dummy"; }
        @Override public boolean hasTeam(@NotNull Player player) { return false; }
        @Override public String getTeamId(@NotNull Player player) { return null; }
        @Override public String getTeamName(@NotNull Player player) { return null; }
        @Override public UUID getTeamLeader(@NotNull Player player) { return null; }
        @Override public @NotNull List<Player> getOnlineMembers(@NotNull Player player) { return Collections.emptyList(); }
        @Override public boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB) { return false; }
    }

    @Test
    @DisplayName("Test is_active placeholders reflect arena operational state, even during lockout")
    public void testIsActivePlaceholders() {
        SpatialGridManager spatial = new SpatialGridManager();
        ArenaManager arenaManager = new ArenaManager(spatial, null);
        TestArena activeArena = new TestArena("south", true);
        activeArena.setState(ArenaState.LOCKED); // locked state during an active match
        TestArena dormantArena = new TestArena("north", false);

        arenaManager.registerArena(activeArena);
        arenaManager.registerArena(dormantArena);

        OutpostsPlaceholderExpansion expansion = new OutpostsPlaceholderExpansion(
                arenaManager,
                spatial,
                new DummyTeamProvider(),
                new LangManager(null),
                null,
                "1.1"
        );

        // Active arena returns true even when state == LOCKED
        assertEquals("true", expansion.onPlaceholderRequest(null, "is_active_south"));
        assertEquals("true", expansion.onPlaceholderRequest(null, "south_is_active"));
        assertEquals("true", expansion.onPlaceholderRequest(null, "arena_south_is_active"));

        // Inactive arena returns false
        assertEquals("false", expansion.onPlaceholderRequest(null, "is_active_north"));
        assertEquals("false", expansion.onPlaceholderRequest(null, "north_is_active"));
    }

    @Test
    @DisplayName("Test scheduler next formatted placeholders point to upcoming event")
    public void testSchedulerPlaceholders() throws Exception {
        SpatialGridManager spatial = new SpatialGridManager();
        ArenaManager arenaManager = new ArenaManager(spatial, null);
        TestArena arena = new TestArena("south", false);
        arenaManager.registerArena(arena);

        ScheduleManager scheduleManager = new ScheduleManager(arenaManager, new LangManager(null), Logger.getAnonymousLogger());
        String yamlStr = """
        schedules:
          weekend_war:
            arena: "south"
            cron: "0 18 * * 6,7"
            duration_minutes: 120
            overtime:
              enabled: true
              max_overtime_minutes: 15
        """;
        YamlDocument config = YamlDocument.create(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
        scheduleManager.loadSchedules(config);

        OutpostsPlaceholderExpansion expansion = new OutpostsPlaceholderExpansion(
                arenaManager,
                spatial,
                new DummyTeamProvider(),
                new LangManager(null),
                scheduleManager,
                "1.1"
        );

        // %outpost_scheduler_next_weekend_war_formatted%
        String nextFormatted = expansion.onPlaceholderRequest(null, "scheduler_next_weekend_war_formatted");
        assertNotNull(nextFormatted);
        assertNotEquals("None", nextFormatted);
        assertNotEquals("Active", nextFormatted);

        // Also query via arena id %outpost_scheduler_next_south_formatted%
        assertEquals(nextFormatted, expansion.onPlaceholderRequest(null, "scheduler_next_south_formatted"));

        // %outpost_schedule_next_weekend_war_formatted% (supports schedule_)
        assertEquals(nextFormatted, expansion.onPlaceholderRequest(null, "schedule_next_weekend_war_formatted"));

        // %outpost_scheduler_next_weekend_war_seconds%
        String nextSeconds = expansion.onPlaceholderRequest(null, "scheduler_next_weekend_war_seconds");
        assertNotNull(nextSeconds);
        long sec = Long.parseLong(nextSeconds);
        assertTrue(sec > 0);

        // %outpost_scheduler_is_active_weekend_war%
        assertEquals("false", expansion.onPlaceholderRequest(null, "scheduler_is_active_weekend_war"));
    }
}
