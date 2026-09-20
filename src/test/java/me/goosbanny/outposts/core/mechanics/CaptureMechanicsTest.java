package me.goosbanny.outposts.core.mechanics;

import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.arena.ArenaMechanicsConfig;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DynamicLocationConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CaptureMechanicsTest {

    private DummyTeamProvider teamProvider;
    private ArenaMechanicsConfig config;

    @BeforeEach
    public void setUp() {
        teamProvider = new DummyTeamProvider();
        config = new ArenaMechanicsConfig(
                true,
                CaptureModeType.STANDARD_HILL,
                10.0, // 10% per tick for testing
                5.0,  // 5% scaling per additional member
                4,
                true,
                100.0,
                0,
                0,
                true,
                2.0,  // 2% passive decay
                2.0,
                20
        );
    }

    private Player createMockPlayer(UUID uuid, String name) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("getName")) return name;
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Test StandardHillEngine: Neutral hill progresses to 100% and captures")
    public void testStandardHillCapture() {
        StandardHillEngine engine = new StandardHillEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("outpost_1");

        Player player = createMockPlayer(UUID.randomUUID(), "Alice");
        teamProvider.registerPlayerTeam(player, "Vikings", "Vikings Clan");

        List<Player> cappers = List.of(player);

        // Tick 1: 0 + 10 = 10%
        engine.evaluateCapture(arena, cappers, false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertNull(arena.getControllerTeamId());

        // Tick to 100%
        for (int i = 0; i < 9; i++) {
            engine.evaluateCapture(arena, cappers, false);
        }

        assertEquals(100.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());
    }

    @Test
    @DisplayName("Test StandardHillEngine: Invading team knocks down defender to 0%")
    public void testStandardHillInvadeAndKnockdown() {
        StandardHillEngine engine = new StandardHillEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("outpost_1");

        // Set Vikings as controller at 100%
        arena.setController("Vikings", "Vikings Clan", UUID.randomUUID());
        assertEquals(100.0, arena.getProgress(), 0.001);

        // Spartan invader arrives
        Player spartan = createMockPlayer(UUID.randomUUID(), "Leonidas");
        teamProvider.registerPlayerTeam(spartan, "Spartans", "Spartans Clan");

        List<Player> cappers = List.of(spartan);

        // Invader knocks down 10%
        engine.evaluateCapture(arena, cappers, false);
        assertEquals(90.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());

        // Knock down remaining 90%
        for (int i = 0; i < 9; i++) {
            engine.evaluateCapture(arena, cappers, false);
        }

        // Once hits 0%, outpost is neutralized
        assertEquals(0.0, arena.getProgress(), 0.001);
        assertNull(arena.getControllerTeamId());
    }

    @Test
    @DisplayName("Test PassiveDecayEngine: Abandoned pad bleeds progress back to 0%")
    public void testPassiveDecayAbandonment() {
        PassiveDecayEngine engine = new PassiveDecayEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("outpost_1");

        arena.setProgress(50.0);
        assertNull(arena.getControllerTeamId());

        // Empty pad ticks abandonment
        engine.handleAbandonment(arena);
        assertEquals(48.0, arena.getProgress(), 0.001);

        engine.handleAbandonment(arena);
        assertEquals(46.0, arena.getProgress(), 0.001);
    }

    private static class DummyTeamProvider implements TeamRosterProvider {
        private final Map<UUID, String> playerTeams = new HashMap<>();
        private final Map<String, String> teamNames = new HashMap<>();

        public void registerPlayerTeam(Player player, String teamId, String teamName) {
            playerTeams.put(player.getUniqueId(), teamId);
            teamNames.put(teamId, teamName);
        }

        @Override public String getProviderName() { return "Dummy"; }
        @Override public boolean hasTeam(Player player) { return playerTeams.containsKey(player.getUniqueId()); }
        @Override public String getTeamId(Player player) { return playerTeams.get(player.getUniqueId()); }
        @Override public String getTeamName(Player player) { return teamNames.get(getTeamId(player)); }
        @Override public UUID getTeamLeader(Player player) { return null; }
        @Override public List<Player> getOnlineMembers(Player player) { return Collections.singletonList(player); }
        @Override public boolean areAllies(String a, String b) { return false; }
    }

    @Test
    @DisplayName("Test StandardHillEngine in SOLO occupancy mode")
    public void testSoloOccupancyCapture() {
        TestOutpostArena arena = new TestOutpostArena("solo_hill");
        arena.setOccupancyMode(OccupancyMode.SOLO);

        StandardHillEngine engine = new StandardHillEngine(teamProvider, config);

        UUID soloPlayerUuid = UUID.randomUUID();
        Player soloPlayer = createMockPlayer(soloPlayerUuid, "SoloKing");

        // Player has NO team in teamProvider
        assertNull(teamProvider.getTeamId(soloPlayer));

        // Evaluate capture in SOLO mode
        engine.evaluateCapture(arena, List.of(soloPlayer), false);
        assertEquals(10.0, arena.getProgress());

        // Advance to 100%
        for (int i = 0; i < 10; i++) {
            engine.evaluateCapture(arena, List.of(soloPlayer), false);
        }
        assertEquals(100.0, arena.getProgress());
        assertEquals(soloPlayerUuid.toString(), arena.getControllerTeamId());
        assertEquals("SoloKing", arena.getControllerTeamName());
    }

    @Test
    @DisplayName("Test StandardHillEngine in TEAM mode with unaffiliated player (fallback to UUID)")
    public void testTeamModeUnaffiliatedPlayerCapture() {
        TestOutpostArena arena = new TestOutpostArena("team_hill");
        arena.setOccupancyMode(OccupancyMode.TEAM);

        StandardHillEngine engine = new StandardHillEngine(teamProvider, config);

        UUID unaffiliatedUuid = UUID.randomUUID();
        Player unaffiliatedPlayer = createMockPlayer(unaffiliatedUuid, "LoneWolf");

        assertNull(teamProvider.getTeamId(unaffiliatedPlayer));

        // In TEAM mode, unaffiliated player falls back to UUID as participant ID
        engine.evaluateCapture(arena, List.of(unaffiliatedPlayer), false);
        assertEquals(10.0, arena.getProgress());
        assertEquals(unaffiliatedUuid.toString(), arena.getCappingTeamId());
        assertEquals("LoneWolf", arena.getCappingTeamName());
    }

    private static class TestOutpostArena implements OutpostArena {
        private final String id;
        private OccupancyMode occupancyMode = OccupancyMode.TEAM;
        private double progress = 0.0;
        private String controllerId = null;
        private String controllerName = null;
        private String cappingId = null;

        public TestOutpostArena(String id) { this.id = id; }
        public void setOccupancyMode(OccupancyMode mode) { this.occupancyMode = mode; }

        @Override public String getId() { return id; }
        @Override public Component getDisplayName() { return Component.text(id); }
        @Override public String getWorldName() { return "world"; }
        @Override public int getMinX() { return 0; }
        @Override public int getMinY() { return 0; }
        @Override public int getMinZ() { return 0; }
        @Override public int getMaxX() { return 10; }
        @Override public int getMaxY() { return 10; }
        @Override public int getMaxZ() { return 10; }
        @Override public CaptureModeType getCaptureModeType() { return CaptureModeType.STANDARD_HILL; }
        @Override public OccupancyMode getOccupancyMode() { return occupancyMode; }
        @Override public double getMultiplier(String key) { return 1.0; }
        @Override public ArenaView createSnapshot() { return null; }
        @Override public void tickGameLoop() {}
        @Override public void setProgress(double p) { this.progress = Math.max(0.0, Math.min(100.0, p)); }
        @Override public void setController(String teamId, String teamName, UUID capturer) {
            this.controllerId = teamId;
            this.controllerName = teamName;
            this.progress = 100.0;
        }
        @Override public void resetToNeutral() {
            this.controllerId = null;
            this.controllerName = null;
            this.progress = 0.0;
        }
        @Override public boolean isWithinBounds(int x, int y, int z) { return true; }
        @Override public ArenaState getState() { return controllerId != null ? ArenaState.CONTROLLED : ArenaState.NEUTRAL; }
        @Override public double getProgress() { return progress; }
        @Override public String getControllerTeamId() { return controllerId; }
        @Override public String getControllerTeamName() { return controllerName; }
        @Override public String getCappingTeamId() { return cappingId; }
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
        private String cappingName = null;
        @Override public String getCappingTeamName() { return cappingName; }
        @Override public void setCappingTeam(String teamId, String teamName) {
            this.cappingId = teamId;
            this.cappingName = teamName;
        }

        private boolean particles = false;
        @Override public boolean isBoundingParticlesEnabled() { return particles; }
        @Override public void setBoundingParticlesEnabled(boolean enabled) { this.particles = enabled; }

        @Override public Map<String, String> getCustomLangOverrides() { return Collections.emptyMap(); }
        @Override public void setCustomLangOverrides(Map<String, String> overrides) {}
        @Override public String getCustomLang(String path) { return null; }
    }

    @Test
    @DisplayName("Test StandardHillEngine: Rival team knocks down existing neutral progress to 0% before claiming")
    public void testNeutralHillKnockdownByRivalTeam() {
        StandardHillEngine engine = new StandardHillEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("neutral_contest");

        Player playerA = createMockPlayer(UUID.randomUUID(), "Alice");
        teamProvider.registerPlayerTeam(playerA, "Vikings", "Vikings Clan");

        Player playerB = createMockPlayer(UUID.randomUUID(), "Bob");
        teamProvider.registerPlayerTeam(playerB, "Spartans", "Spartans Clan");

        // Step 1: Vikings capture up to 30%
        engine.evaluateCapture(arena, List.of(playerA), false);
        engine.evaluateCapture(arena, List.of(playerA), false);
        engine.evaluateCapture(arena, List.of(playerA), false);
        assertEquals(30.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getCappingTeamId());
        assertNull(arena.getControllerTeamId());

        // Step 2: Spartans arrive while Vikings are gone.
        // First tick: knocks down 10% (30 -> 20%)
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(20.0, arena.getProgress(), 0.001);

        // Second tick: knocks down 10% (20 -> 10%)
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(10.0, arena.getProgress(), 0.001);

        // Third tick: knocks down to 0%
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(0.0, arena.getProgress(), 0.001);

        // Fourth tick: Spartans now claim from 0% -> 10%
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertEquals("Spartans", arena.getCappingTeamId());
    }

    @Test
    @DisplayName("Test StandardHillEngine: Captured hill decays down to 0% and resets to neutral when abandoned")
    public void testPostCaptureAbandonmentDecay() {
        StandardHillEngine engine = new StandardHillEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("captured_decay");

        // Vikings control at 100%
        arena.setController("Vikings", "Vikings Clan", UUID.randomUUID());
        assertEquals(100.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());

        // Pad abandoned: decay starts ticking down (passiveDecayRate = 2.0%)
        engine.handleAbandonment(arena);
        assertEquals(98.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());

        // Bleed down all the way to 0% (49 more ticks)
        for (int i = 0; i < 49; i++) {
            engine.handleAbandonment(arena);
        }

        // At 0%, ownership is stripped and outpost resets to neutral
        assertEquals(0.0, arena.getProgress(), 0.001);
        assertNull(arena.getControllerTeamId());
        assertEquals(ArenaState.NEUTRAL, arena.getState());
    }

    @Test
    @DisplayName("Test Bounding Particles default to false and can be toggled")
    public void testBoundingParticlesToggle() {
        TestOutpostArena arena = new TestOutpostArena("particles_test");
        assertFalse(arena.isBoundingParticlesEnabled());
        arena.setBoundingParticlesEnabled(true);
        assertTrue(arena.isBoundingParticlesEnabled());
        arena.setBoundingParticlesEnabled(false);
        assertFalse(arena.isBoundingParticlesEnabled());
    }

    @Test
    @DisplayName("Test TicketAccumulationEngine: Neutral knockdown and post-capture abandonment decay")
    public void testTicketAccumulationNeutralKnockdownAndDecay() {
        TicketAccumulationEngine engine = new TicketAccumulationEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("tickets_test");

        Player playerA = createMockPlayer(UUID.randomUUID(), "Alice");
        teamProvider.registerPlayerTeam(playerA, "Vikings", "Vikings Clan");
        Player playerB = createMockPlayer(UUID.randomUUID(), "Bob");
        teamProvider.registerPlayerTeam(playerB, "Spartans", "Spartans Clan");

        // Vikings get 20% tickets
        engine.evaluateCapture(arena, List.of(playerA), false);
        engine.evaluateCapture(arena, List.of(playerA), false);
        assertEquals(20.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getCappingTeamId());

        // Spartans enter alone: knocks down 20 -> 10 -> 0
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(0.0, arena.getProgress(), 0.001);

        // Next tick: Spartans now accumulate their own tickets
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertEquals("Spartans", arena.getCappingTeamId());

        // Fill to 100% and test abandonment decay
        arena.setController("Spartans", "Spartans Clan", UUID.randomUUID());
        assertEquals(100.0, arena.getProgress(), 0.001);

        engine.handleAbandonment(arena);
        assertEquals(98.0, arena.getProgress(), 0.001);
    }

    @Test
    @DisplayName("Test PassiveDecayEngine: Solo mode support and neutralization knockdown")
    public void testPassiveDecaySoloAndKnockdown() {
        PassiveDecayEngine engine = new PassiveDecayEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("passive_decay_solo");
        arena.setOccupancyMode(OccupancyMode.SOLO);

        UUID uuidA = UUID.randomUUID();
        Player playerA = createMockPlayer(uuidA, "SoloOne");
        UUID uuidB = UUID.randomUUID();
        Player playerB = createMockPlayer(uuidB, "SoloTwo");

        // Player A gets 20%
        engine.evaluateCapture(arena, List.of(playerA), false);
        engine.evaluateCapture(arena, List.of(playerA), false);
        assertEquals(20.0, arena.getProgress(), 0.001);
        assertEquals(uuidA.toString(), arena.getCappingTeamId());

        // Player B enters: burns down 20 -> 10 -> 0
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(0.0, arena.getProgress(), 0.001);

        // Player B accumulates positive progress
        engine.evaluateCapture(arena, List.of(playerB), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertEquals(uuidB.toString(), arena.getCappingTeamId());
    }

    @Test
    @DisplayName("Test TugOfWarEngine: Knocking defender to 0% resets to neutral without instant controller grant")
    public void testTugOfWarKnockdownAndNeutralReset() {
        TugOfWarEngine engine = new TugOfWarEngine(teamProvider, config);
        TestOutpostArena arena = new TestOutpostArena("tow_test");

        Player defender = createMockPlayer(UUID.randomUUID(), "Defender");
        teamProvider.registerPlayerTeam(defender, "TeamDef", "Defenders");
        Player attacker = createMockPlayer(UUID.randomUUID(), "Attacker");
        teamProvider.registerPlayerTeam(attacker, "TeamAtk", "Attackers");

        // Defender starts with 100% control
        arena.setController("TeamDef", "Defenders", UUID.randomUUID());
        assertEquals(100.0, arena.getProgress(), 0.001);

        // Attacker arrives alone and knocks down 100% -> 0%
        for (int i = 0; i < 10; i++) {
            engine.evaluateCapture(arena, List.of(attacker), false);
        }

        // Must reset to neutral (controller is null, not automatically assigned to attacker at 0%)
        assertEquals(0.0, arena.getProgress(), 0.001);
        assertNull(arena.getControllerTeamId());
        assertEquals("TeamAtk", arena.getCappingTeamId());
    }

    @Test
    @DisplayName("Test TugOfWarEngine: Contested multi-team (3+ teams) pulls toward dominant team")
    public void testMultiTeamContestedTugOfWar() {
        ArenaMechanicsConfig noFreezeConfig = new ArenaMechanicsConfig(
                true,
                CaptureModeType.TUG_OF_WAR,
                10.0,
                0.0,
                1,
                false, // freezeWhenContested = false
                100.0,
                0,
                0,
                false,
                0.0,
                2.0,
                20
        );

        TugOfWarEngine engine = new TugOfWarEngine(teamProvider, noFreezeConfig);
        TestOutpostArena arena = new TestOutpostArena("multi_tow");
        arena.setProgress(50.0);

        Player playerA = createMockPlayer(UUID.randomUUID(), "PlayerA");
        teamProvider.registerPlayerTeam(playerA, "TeamA", "Team A");

        Player playerB = createMockPlayer(UUID.randomUUID(), "PlayerB");
        teamProvider.registerPlayerTeam(playerB, "TeamB", "Team B");

        Player playerC1 = createMockPlayer(UUID.randomUUID(), "PlayerC1");
        Player playerC2 = createMockPlayer(UUID.randomUUID(), "PlayerC2");
        Player playerC3 = createMockPlayer(UUID.randomUUID(), "PlayerC3");
        teamProvider.registerPlayerTeam(playerC1, "TeamC", "Team C");
        teamProvider.registerPlayerTeam(playerC2, "TeamC", "Team C");
        teamProvider.registerPlayerTeam(playerC3, "TeamC", "Team C");

        arena.setCappingTeam("TeamC", "Team C");

        // Team A (1), Team B (1), Team C (3) => Team C dominant: 3 - (1 + 1) = 1 > 0
        engine.evaluateCapture(arena, List.of(playerA, playerB, playerC1, playerC2, playerC3), true);
        // delta = 10.0 * 0.5 = 5.0 -> 50.0 + 5.0 = 55.0
        assertEquals(55.0, arena.getProgress(), 0.001);
    }
}
