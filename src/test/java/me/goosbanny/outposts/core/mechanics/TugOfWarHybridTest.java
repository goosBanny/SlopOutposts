package me.goosbanny.outposts.core.mechanics;

import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.mechanics.TugOfWarTeamAssignment;
import me.goosbanny.outposts.core.arena.ArenaMechanicsConfig;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TugOfWarHybridTest {

    private CaptureMechanicsTest.DummyTeamProvider teamProvider;

    @BeforeEach
    public void setup() {
        this.teamProvider = new CaptureMechanicsTest.DummyTeamProvider();
    }

    private Player createMockPlayer(UUID uuid, String name) {
        return CaptureMechanicsTest.createMockPlayer(uuid, name);
    }

    @Test
    @DisplayName("Test AUTO_RED_BLUE: Automatically assigns players to Red and Blue, pulling towards respective victory lines")
    public void testAutoRedBlueAssignmentAndPull() {
        ArenaMechanicsConfig autoConfig = new ArenaMechanicsConfig(
                true,
                true,
                CaptureModeType.TUG_OF_WAR,
                5.0,
                5.0,
                1,
                4,
                false,
                100.0,
                0,
                0,
                true,
                2.5,
                2.0,
                1.0,
                1,
                50.0,
                1.0, // 100% speed per player diff
                2.5,
                1000,
                10.0,
                TugOfWarTeamAssignment.AUTO_RED_BLUE,
                2.5
        );

        TugOfWarEngine engine = new TugOfWarEngine(teamProvider, autoConfig);
        CaptureMechanicsTest.TestOutpostArena arena = new CaptureMechanicsTest.TestOutpostArena("tow_auto");
        arena.setProgress(50.0);

        Player p1 = createMockPlayer(UUID.randomUUID(), "Player1");
        Player p2 = createMockPlayer(UUID.randomUUID(), "Player2");
        Player p3 = createMockPlayer(UUID.randomUUID(), "Player3");

        // First evaluation: p1 -> RED, p2 -> BLUE, p3 -> RED (balances: 2 RED, 1 BLUE)
        engine.evaluateCapture(arena, List.of(p1, p2, p3), true);

        assertEquals(TugOfWarEngine.TugSide.SIDE_A, engine.getPlayerSide(p1));
        assertEquals(TugOfWarEngine.TugSide.SIDE_B, engine.getPlayerSide(p2));
        assertEquals(TugOfWarEngine.TugSide.SIDE_A, engine.getPlayerSide(p3));

        // Diff: 2 Red - 1 Blue = 1 Red advantage -> pulls towards 0.0%
        // delta = 5.0 * 1.0 * 1 = 5.0 -> 50.0 - 5.0 = 45.0%
        assertEquals(45.0, arena.getProgress(), 0.001);
        assertEquals("RED", arena.getCappingTeamId());
    }

    @Test
    @DisplayName("Test FIRST_TWO_FACTIONS: First 2 factions duel, 3rd party is excluded")
    public void testFirstTwoFactionsExcludesThirdParty() {
        ArenaMechanicsConfig factionsConfig = new ArenaMechanicsConfig(
                true,
                true,
                CaptureModeType.TUG_OF_WAR,
                10.0,
                10.0,
                1,
                4,
                false,
                100.0,
                0,
                0,
                true,
                2.5,
                2.0,
                1.0,
                1,
                50.0,
                0.5,
                2.5,
                1000,
                10.0,
                TugOfWarTeamAssignment.FIRST_TWO_FACTIONS,
                2.5
        );

        TugOfWarEngine engine = new TugOfWarEngine(teamProvider, factionsConfig);
        CaptureMechanicsTest.TestOutpostArena arena = new CaptureMechanicsTest.TestOutpostArena("tow_factions");
        arena.setProgress(50.0);

        Player a1 = createMockPlayer(UUID.randomUUID(), "Alpha1");
        teamProvider.registerPlayerTeam(a1, "FactionA", "Alpha");

        Player b1 = createMockPlayer(UUID.randomUUID(), "Bravo1");
        Player b2 = createMockPlayer(UUID.randomUUID(), "Bravo2");
        teamProvider.registerPlayerTeam(b1, "FactionB", "Bravo");
        teamProvider.registerPlayerTeam(b2, "FactionB", "Bravo");

        Player c1 = createMockPlayer(UUID.randomUUID(), "Charlie1");
        Player c2 = createMockPlayer(UUID.randomUUID(), "Charlie2");
        teamProvider.registerPlayerTeam(c1, "FactionC", "Charlie");
        teamProvider.registerPlayerTeam(c2, "FactionC", "Charlie");

        // FactionA (Side A) has 1, FactionB (Side B) has 2, FactionC (3rd party) has 2
        engine.evaluateCapture(arena, List.of(a1, b1, b2, c1, c2), true);

        assertEquals("FactionA", engine.getSideAId());
        assertEquals("FactionB", engine.getSideBId());
        assertEquals(TugOfWarEngine.TugSide.NONE, engine.getPlayerSide(c1));

        // Contest is only between Side A (1) and Side B (2) -> diff = 2 - 1 = 1 towards Side B (100.0%)
        // delta = 10.0 * 0.5 * 1 = 5.0 -> 50.0 + 5.0 = 55.0%
        assertEquals(55.0, arena.getProgress(), 0.001);
        assertEquals("FactionB", arena.getCappingTeamId());
    }

    @Test
    @DisplayName("Test TugOfWar abandonment drifts back to 50% neutral midpoint")
    public void testTugOfWarAbandonmentDrift() {
        ArenaMechanicsConfig driftConfig = new ArenaMechanicsConfig(
                true,
                true,
                CaptureModeType.TUG_OF_WAR,
                10.0,
                10.0,
                1,
                4,
                false,
                100.0,
                0,
                0,
                true,
                5.0, // 5% per second decay
                2.0,
                1.0,
                1,
                50.0,
                0.5,
                5.0,
                1000,
                10.0,
                TugOfWarTeamAssignment.FIRST_TWO_FACTIONS,
                2.5
        );

        TugOfWarEngine engine = new TugOfWarEngine(teamProvider, driftConfig);
        CaptureMechanicsTest.TestOutpostArena arena = new CaptureMechanicsTest.TestOutpostArena("tow_drift");

        // Starts at 25.0% (Side A advantage), abandoned
        arena.setProgress(25.0);
        engine.handleAbandonment(arena);
        // Drifts +5% toward 50.0%
        assertEquals(30.0, arena.getProgress(), 0.001);

        // Starts at 80.0% (Side B advantage), abandoned
        arena.setProgress(80.0);
        engine.handleAbandonment(arena);
        // Drifts -5% toward 50.0%
        assertEquals(75.0, arena.getProgress(), 0.001);
    }
}
