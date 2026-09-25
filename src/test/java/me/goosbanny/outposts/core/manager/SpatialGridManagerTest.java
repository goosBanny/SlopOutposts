package me.goosbanny.outposts.core.manager;

import me.goosbanny.outposts.api.arena.ArenaDescriptor;
import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DynamicLocationConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SpatialGridManagerTest {

    private SpatialGridManager gridManager;

    @BeforeEach
    public void setUp() {
        gridManager = new SpatialGridManager();
    }

    @Test
    @DisplayName("Test bitwise chunk key packing and unpacking")
    public void testChunkKeyPacking() {
        int[] testX = {0, 1, -1, 100, -500, 16384, -16384};
        int[] testZ = {0, 1, -1, 200, -350, 32768, -32768};

        for (int x : testX) {
            for (int z : testZ) {
                long key = SpatialGridManager.getChunkKey(x, z);
                assertEquals(x, SpatialGridManager.getChunkX(key), "Chunk X mismatch for " + x + ", " + z);
                assertEquals(z, SpatialGridManager.getChunkZ(key), "Chunk Z mismatch for " + x + ", " + z);
            }
        }
    }

    @Test
    @DisplayName("Test arena spatial index registration and primitive AABB collision")
    public void testArenaSpatialLookup() {
        OutpostArena mockArena = new DummyArena(
                "south_outpost",
                "world",
                100, 60, 100,
                110, 70, 110
        );

        gridManager.registerArena(mockArena);

        // Point inside bounds
        assertNotNull(gridManager.getArenaAt("world", 105, 65, 105));
        assertEquals("south_outpost", gridManager.getArenaAt("world", 105, 65, 105).getId());

        // Exact boundary corners
        assertNotNull(gridManager.getArenaAt("world", 100, 60, 100));
        assertNotNull(gridManager.getArenaAt("world", 110, 70, 110));

        // Point outside bounds
        assertNull(gridManager.getArenaAt("world", 99, 65, 105));
        assertNull(gridManager.getArenaAt("world", 111, 65, 105));
        assertNull(gridManager.getArenaAt("world", 105, 59, 105));
        assertNull(gridManager.getArenaAt("world", 105, 71, 105));

        // Different world
        assertNull(gridManager.getArenaAt("world_nether", 105, 65, 105));

        // Deregister
        gridManager.unregisterArena(mockArena);
        assertNull(gridManager.getArenaAt("world", 105, 65, 105));
    }

    @Test
    @DisplayName("Test Multi-World chunk indexing and isolation")
    public void testMultiWorldIsolation() {
        OutpostArena overworldArena = new DummyArena("overworld_arena", "world", 0, 60, 0, 15, 70, 15);
        OutpostArena netherArena = new DummyArena("nether_arena", "world_nether", 0, 60, 0, 15, 70, 15);

        gridManager.registerArena(overworldArena);
        gridManager.registerArena(netherArena);

        // Both arenas are in chunk (0, 0), but in different worlds
        var worldArenas = gridManager.getArenasInChunk("world", 0, 0);
        assertEquals(1, worldArenas.size());
        assertEquals("overworld_arena", worldArenas.iterator().next().getId());

        var netherArenas = gridManager.getArenasInChunk("world_nether", 0, 0);
        assertEquals(1, netherArenas.size());
        assertEquals("nether_arena", netherArenas.iterator().next().getId());

    }

    private static class DummyArena implements OutpostArena {
        private final String id;
        private final String world;
        private final int minX, minY, minZ, maxX, maxY, maxZ;

        public DummyArena(String id, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.id = id;
            this.world = world;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        @Override public String getId() { return id; }
        @Override public Component getDisplayName() { return Component.text(id); }
        @Override public String getWorldName() { return world; }
        @Override public int getMinX() { return minX; }
        @Override public int getMinY() { return minY; }
        @Override public int getMinZ() { return minZ; }
        @Override public int getMaxX() { return maxX; }
        @Override public int getMaxY() { return maxY; }
        @Override public int getMaxZ() { return maxZ; }
        @Override public CaptureModeType getCaptureModeType() { return CaptureModeType.STANDARD_HILL; }
        @Override public OccupancyMode getOccupancyMode() { return OccupancyMode.TEAM; }
        @Override public double getMultiplier(String key) { return 1.0; }
        @Override public ArenaView createSnapshot() { return null; }
        @Override public void tickGameLoop() {}
        @Override public void setProgress(double progress) {}
        @Override public void setController(String teamId, String teamName, UUID capturer) {}
        @Override public void resetToNeutral() {}
        @Override public boolean isWithinBounds(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
        @Override public ArenaState getState() { return ArenaState.NEUTRAL; }
        @Override public double getProgress() { return 0; }
        @Override public String getControllerTeamId() { return null; }
        @Override public String getControllerTeamName() { return null; }
        @Override public String getCappingTeamId() { return null; }
        @Override public String getCappingTeamName() { return null; }
        @Override public void setCappingTeam(String teamId, String teamName) {}
        @Override public int getCapperCount() { return 0; }
        @Override public boolean isContested() { return false; }
        @Override public boolean isLocked() { return false; }
        @Override public long getLockoutRemainingSeconds() { return 0; }
        @Override public Location getCenterLocation() { return null; }
        @Override public Location getWarpLocation() { return null; }
        @Override public void setWarpLocation(Location location) {}
        private boolean active = true;
        @Override public boolean isActive() { return active; }
        @Override public void setActive(boolean active) { this.active = active; }

        private final DynamicLocationConfig dlc = DynamicLocationConfig.createDisabled();
        @Override public DynamicLocationConfig getDynamicLocationConfig() { return dlc; }
        @Override public ArenaRegion getCurrentRegion() { return null; }
        @Override public void shiftToRegion(ArenaRegion target) {}
        @Override public long getNextShiftSeconds() { return -1; }
        @Override public int getActivationGraceRemainingSeconds() { return 0; }
        @Override public boolean isWarmingUp() { return false; }
        @Override public Map<String, String> getCustomLangOverrides() { return Collections.emptyMap(); }
        @Override public void setCustomLangOverrides(Map<String, String> overrides) {}
        @Override public String getCustomLang(String path) { return null; }
        private boolean particles = false;
        @Override public boolean isBoundingParticlesEnabled() { return particles; }
        @Override public void setBoundingParticlesEnabled(boolean enabled) { this.particles = enabled; }
    }
}
