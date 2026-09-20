package me.goosbanny.outposts.core.schedule;

import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DynamicLocationConfig;
import me.goosbanny.outposts.core.manager.ArenaManager;
import me.goosbanny.outposts.core.manager.SpatialGridManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class ScheduleManagerSyncTest {

    static class DummyArena implements OutpostArena {
        private final String id;
        private boolean active = false;

        public DummyArena(String id) { this.id = id; }
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
        @Override public ArenaState getState() { return active ? ArenaState.NEUTRAL : ArenaState.LOCKED; }
        @Override public double getProgress() { return 0; }
        @Override public void setProgress(double progress) {}
        @Override public String getControllerTeamId() { return null; }
        @Override public String getControllerTeamName() { return null; }
        @Override public void setController(String teamId, String teamName, UUID capturingPlayer) {}
        @Override public void resetToNeutral() {}
        @Override public String getCappingTeamId() { return null; }
        @Override public int getCapperCount() { return 0; }
        @Override public boolean isContested() { return false; }
        @Override public boolean isLocked() { return !active; }
        @Override public long getLockoutRemainingSeconds() { return 0; }
        @Override public Location getCenterLocation() { return null; }
        @Override public Location getWarpLocation() { return null; }
        @Override public void setWarpLocation(Location warpLocation) {}
        @Override public CaptureModeType getCaptureModeType() { return CaptureModeType.STANDARD_HILL; }
        @Override public ArenaView createSnapshot() { return null; }
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
    }

    @Test
    @DisplayName("Test ScheduleManager: Syncs dormant state on inactive schedules")
    public void testSyncArenaInactive() throws Exception {
        SpatialGridManager spatial = new SpatialGridManager();
        ArenaManager arenaManager = new ArenaManager(spatial, null);
        DummyArena arena = new DummyArena("south");
        arena.setActive(false);
        arenaManager.registerArena(arena);

        ScheduleManager scheduleManager = new ScheduleManager(arenaManager, new LangManager(null), Logger.getAnonymousLogger());

        String yamlStr = """
        schedules:
          weekend_war:
            arena: "south"
            cron: "0 0 1 1 *"
            duration_minutes: 60
            overtime:
              enabled: false
        """;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yamlStr));
        scheduleManager.loadSchedules(config);

        scheduleManager.syncArenaState(arena);
        assertFalse(arena.isActive(), "Arena governed by inactive schedule should remain inactive / locked");
    }
}
