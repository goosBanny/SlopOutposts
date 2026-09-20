package me.goosbanny.outposts.core.arena;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicLocationConfigTest {

    @Test
    public void testWeightedSelectionAndNoImmediateRepeat() {
        ArenaGeometry g1 = new ArenaGeometry("world", 0, 60, 0, 10, 70, 10, null);
        ArenaGeometry g2 = new ArenaGeometry("world", 100, 60, 100, 110, 70, 110, null);
        ArenaGeometry g3 = new ArenaGeometry("world", 200, 60, 200, 210, 70, 210, null);

        ArenaRegion r1 = new ArenaRegion("region1", "Region 1", 50, g1);
        ArenaRegion r2 = new ArenaRegion("region2", "Region 2", 30, g2);
        ArenaRegion r3 = new ArenaRegion("region3", "Region 3", 20, g3);

        DynamicLocationConfig config = new DynamicLocationConfig(
                true,
                DynamicLocationConfig.SwitchMode.INTERVAL,
                300,
                180,
                420,
                List.of(300L, 600L),
                false,
                List.of(60, 30, 10),
                5,
                5,
                DynamicLocationConfig.StateTransition.KEEP_PROGRESS,
                DynamicLocationConfig.PlayerTransition.NONE,
                true, // noImmediateRepeat
                true,
                "ENTITY_ENDERMAN_TELEPORT",
                true,
                List.of(r1, r2, r3)
        );

        assertEquals(3, config.getRegions().size());
        assertEquals("region1", config.getRegion("region1").getId());

        Random rnd = new Random(42);
        for (int i = 0; i < 50; i++) {
            ArenaRegion next = config.selectNextRegion(r1, rnd);
            assertNotNull(next);
            // With noImmediateRepeat enabled and multiple regions, r1 must never be rolled back-to-back
            assertNotEquals("region1", next.getId());
        }
    }

    @Test
    public void testAddAndRemoveRegion() {
        DynamicLocationConfig config = DynamicLocationConfig.createDisabled();
        ArenaGeometry g = new ArenaGeometry("world", 0, 60, 0, 10, 70, 10, null);
        ArenaRegion r = new ArenaRegion("test_region", "Test", 100, g);

        config.addRegion(r);
        assertEquals(1, config.getRegions().size());
        assertNotNull(config.getRegion("test_region"));

        boolean removed = config.removeRegion("test_region");
        assertTrue(removed);
        assertEquals(0, config.getRegions().size());
    }
}
