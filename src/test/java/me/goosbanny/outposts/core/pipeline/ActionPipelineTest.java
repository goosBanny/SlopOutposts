package me.goosbanny.outposts.core.pipeline;

import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DynamicLocationConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ActionPipelineTest {

    static class DummyArena implements OutpostArena {
        private final String id;
        private final OccupancyMode occupancyMode;

        public DummyArena(String id, OccupancyMode occupancyMode) {
            this.id = id;
            this.occupancyMode = occupancyMode;
        }

        @Override public String getId() { return id; }
        @Override public Component getDisplayName() { return Component.text("South Outpost"); }
        @Override public String getWorldName() { return "world"; }
        @Override public int getMinX() { return 0; }
        @Override public int getMinY() { return 0; }
        @Override public int getMinZ() { return 0; }
        @Override public int getMaxX() { return 10; }
        @Override public int getMaxY() { return 10; }
        @Override public int getMaxZ() { return 10; }
        @Override public boolean isWithinBounds(int x, int y, int z) { return true; }
        @Override public ArenaState getState() { return ArenaState.CONTROLLED; }
        @Override public double getProgress() { return 100.0; }
        @Override public void setProgress(double progress) {}
        @Override public String getControllerTeamId() { return "vikings"; }
        @Override public String getControllerTeamName() { return "Vikings"; }
        @Override public void setController(String teamId, String teamName, UUID capturingPlayer) {}
        @Override public void resetToNeutral() {}
        @Override public String getCappingTeamId() { return null; }
        @Override public int getCapperCount() { return 1; }
        @Override public boolean isContested() { return false; }
        @Override public boolean isLocked() { return false; }
        @Override public long getLockoutRemainingSeconds() { return 0; }
        @Override public Location getCenterLocation() { return null; }
        @Override public Location getWarpLocation() { return null; }
        @Override public void setWarpLocation(Location warpLocation) {}
        @Override public CaptureModeType getCaptureModeType() { return CaptureModeType.STANDARD_HILL; }
        @Override public OccupancyMode getOccupancyMode() { return occupancyMode; }
        @Override public void tickGameLoop() {}
        @Override public ArenaView createSnapshot() { return null; }
        @Override public boolean isActive() { return true; }
        @Override public void setActive(boolean active) {}
        @Override public boolean isBoundingParticlesEnabled() { return false; }
        @Override public void setBoundingParticlesEnabled(boolean enabled) {}
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
        @Override public boolean hasTeam(@NotNull Player player) { return true; }
        @Override public @Nullable String getTeamId(@NotNull Player player) { return "vikings"; }
        @Override public @Nullable String getTeamName(@NotNull Player player) { return "Vikings"; }
        @Override public @Nullable UUID getTeamLeader(@NotNull Player player) { return null; }
        @Override public @NotNull List<Player> getOnlineMembers(@NotNull Player player) { return Collections.emptyList(); }
        @Override public @NotNull List<Player> getOnlineMembersById(@NotNull String teamId) { return Collections.emptyList(); }
        @Override public boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB) { return false; }
    }

    @Test
    @DisplayName("IntervalFilteredAction respects every_seconds condition")
    void testIntervalFilteredAction() {
        AtomicInteger executedCount = new AtomicInteger(0);
        ArenaAction mockAction = new ArenaAction() {
            @Override public @NotNull String getType() { return "MOCK"; }
            @Override public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
                executedCount.incrementAndGet();
            }
        };

        IntervalFilteredAction filtered = new IntervalFilteredAction(mockAction, 300);
        DummyArena arena = new DummyArena("south", OccupancyMode.TEAM);

        Map<String, Object> context30s = new HashMap<>();
        context30s.put("seconds_held", 30);
        filtered.execute(arena, context30s);
        assertEquals(0, executedCount.get(), "Action should NOT execute at 30 seconds");

        Map<String, Object> context300s = new HashMap<>();
        context300s.put("seconds_held", 300);
        filtered.execute(arena, context300s);
        assertEquals(1, executedCount.get(), "Action SHOULD execute at 300 seconds (5m)");

        Map<String, Object> context450s = new HashMap<>();
        context450s.put("seconds_held", 450);
        filtered.execute(arena, context450s);
        assertEquals(1, executedCount.get(), "Action should NOT execute at 450 seconds");

        Map<String, Object> context600s = new HashMap<>();
        context600s.put("seconds_held", 600);
        filtered.execute(arena, context600s);
        assertEquals(2, executedCount.get(), "Action SHOULD execute at 600 seconds (10m)");
    }

    @Test
    @DisplayName("ActionTargetResolver returns non-null player list for unknown players safely")
    void testActionTargetResolverSafety() {
        DummyArena arena = new DummyArena("south", OccupancyMode.TEAM);
        DummyTeamProvider teamProvider = new DummyTeamProvider();
        Map<String, Object> context = new HashMap<>();
        context.put("team_id", "vikings");
        context.put("previous_team_id", "spartans");

        List<Player> controllerPlayers = ActionTargetResolver.resolvePlayers("CONTROLLER", "CONTROLLER", arena, context, teamProvider);
        assertNotNull(controllerPlayers);

        List<Player> prevPlayers = ActionTargetResolver.resolvePlayers("PREVIOUS_CONTROLLER", "CONTROLLER", arena, context, teamProvider);
        assertNotNull(prevPlayers);

        List<Player> zonePlayers = ActionTargetResolver.resolvePlayers("ZONE", "CONTROLLER", arena, context, teamProvider);
        assertNotNull(zonePlayers);
    }
}
