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

/**
 * End-to-end lifecycle tests for the StandardHillEngine covering every outpost
 * stage as experienced by two competing players/teams.
 *
 * Engine speed: 10% per tick, uncapture: 10% per tick, decay: 2% per tick.
 * Default lose_control_threshold: 100.0 (engine drives progress to 0% first).
 */
public class OutpostLifecycleTest {

    private Player alice; // team "Vikings"
    private Player bob;   // team "Spartans"
    private DummyTeamProvider teams;
    private ArenaMechanicsConfig cfg;

    @BeforeEach
    public void setUp() {
        teams = new DummyTeamProvider();

        alice = mockPlayer(UUID.randomUUID(), "Alice");
        bob   = mockPlayer(UUID.randomUUID(), "Bob");

        teams.register(alice, "Vikings",  "Viking Clan");
        teams.register(bob,   "Spartans", "Spartan Clan");

        cfg = new ArenaMechanicsConfig(
                true,                        // enabled
                CaptureModeType.STANDARD_HILL,
                10.0,                        // percent per tick
                0.0,                         // no per-member scaling
                4,                           // max cappers counted
                true,                        // freeze when contested
                100.0,                       // lose_control_threshold (standard)
                0,                           // lockout seconds
                0,                           // knock delay seconds
                true,                        // passive decay enabled
                2.0,                         // 2% passive decay per tick
                2.0,                         // hysteresis buffer
                1                            // state change cooldown ticks
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. NEUTRAL → CAPTURING: progress advances each tick
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("1. Alice begins capturing a neutral pad — progress advances each tick")
    public void neutralPad_AliceCaptures() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getCappingTeamId());
        assertNull(arena.getControllerTeamId());
        assertEquals(ArenaState.CAPTURING, arena.getState());

        // 4 more ticks → 50%
        for (int i = 0; i < 4; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(50.0, arena.getProgress(), 0.001);
        assertEquals(ArenaState.CAPTURING, arena.getState());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. CONTESTED: freeze_when_contested halts progress
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("2. Bob joins mid-cap — contest freezes progress while both are present")
    public void contested_ProgressFreezes() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        for (int i = 0; i < 3; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(30.0, arena.getProgress(), 0.001);

        // Both on pad → freeze
        engine.evaluateCapture(arena, List.of(alice, bob), true);
        assertEquals(30.0, arena.getProgress(), 0.001, "must freeze while contested");

        // Bob leaves → Alice resumes
        engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(40.0, arena.getProgress(), 0.001);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. CAPTURING → CONTROLLED: reach 100%
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("3. Alice captures to 100% — outpost becomes CONTROLLED, capping team cleared")
    public void capture_AliceReachesHundredPercent() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        for (int i = 0; i < 10; i++) engine.evaluateCapture(arena, List.of(alice), false);

        assertEquals(100.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());
        assertEquals(ArenaState.CONTROLLED, arena.getState());
        assertNull(arena.getCappingTeamId(),   "capping team id must be null post-capture");
        assertNull(arena.getCappingTeamName(), "capping team name must be null post-capture");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. CONTROLLED: defender ally re-enters and heals progress
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("4. Bob chips away, Alice returns and heals progress back to 100%")
    public void controlled_DefenderRecovery() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        arena.setController("Vikings", "Viking Clan", UUID.randomUUID());

        // Bob knocks 100 → 70
        for (int i = 0; i < 3; i++) engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(70.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());

        // Alice heals 70 → 100 (3 ticks)
        for (int i = 0; i < 3; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(100.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. CONTROLLED → NEUTRAL: full knockdown to 0%, pad handed to invader
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("5. Bob fully knocks Vikings to 0% — outpost neutralizes, Bob starts capping next tick")
    public void controlled_FullKnockdownToNeutral() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        arena.setController("Vikings", "Viking Clan", UUID.randomUUID());

        // 10 ticks: 100 → 0%
        for (int i = 0; i < 10; i++) engine.evaluateCapture(arena, List.of(bob), false);

        assertEquals(0.0, arena.getProgress(), 0.001);
        assertNull(arena.getControllerTeamId());
        assertEquals(ArenaState.NEUTRAL, arena.getState());
        // Note: engine sets capper via DefaultOutpostArena cast not available in TestArena stub.
        // Verify Bob immediately starts capturing on the next tick.
        engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertEquals("Spartans", arena.getCappingTeamId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. RIVAL MID-CAP KNOCKDOWN: Bob must wipe existing partial progress first
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("6. Bob arrives at 50% Viking progress — wipes it before starting own capture")
    public void neutral_RivalKnockdownBeforeCapture() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        for (int i = 0; i < 5; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(50.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getCappingTeamId());

        // Bob knocks 50 → 0 (5 ticks)
        for (int i = 0; i < 5; i++) engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(0.0, arena.getProgress(), 0.001);

        // Next tick: Bob starts own capture
        engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertEquals("Spartans", arena.getCappingTeamId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. LOCKOUT: engine refuses capture while locked; resumes after lockout drains
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("7. Pad is locked post-capture — Bob cannot knock it until lockout expires")
    public void controlled_LockoutBlocksCapture() {
        ArenaMechanicsConfig lockedCfg = new ArenaMechanicsConfig(
                true, CaptureModeType.STANDARD_HILL,
                10.0, 0.0, 4, true, 100.0,
                5, 0, true, 2.0, 2.0, 1
        );
        StandardHillEngine engine = new StandardHillEngine(teams, lockedCfg);
        TestArena arena = arena();

        arena.setController("Vikings", "Viking Clan", UUID.randomUUID());
        arena.setLockout(5);

        // Bob can't break through lockout
        engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(100.0, arena.getProgress(), 0.001, "locked — progress must be unchanged");

        // Drain lockout
        for (int i = 0; i < 5; i++) arena.tickLockout();
        assertFalse(arena.isLocked());

        // Now Bob can knock down
        engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(90.0, arena.getProgress(), 0.001);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. PASSIVE DECAY — abandoned neutral pad bleeds back to 0%
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("8. Alice leaves at 30% — passive decay drains pad to 0%, ownership cleared")
    public void neutral_PassiveDecayResetsProgress() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        for (int i = 0; i < 3; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(30.0, arena.getProgress(), 0.001);

        // 15 abandonment ticks × 2% = 30% drained
        for (int i = 0; i < 15; i++) engine.handleAbandonment(arena);

        assertEquals(0.0, arena.getProgress(), 0.001);
        // No controller and no progress — pad is effectively reset.
        // (Capper name clearing only fires inside DefaultOutpostArena cast; not tested on stub.)
        assertNull(arena.getControllerTeamId());
        assertEquals(ArenaState.NEUTRAL, arena.getState());

        // Verify the next capper can immediately claim again
        engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. PASSIVE DECAY (controlled) — abandoned controlled pad loses ownership at 0%
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("9. Vikings control but abandon pad — 50 decay ticks strips ownership at 0%")
    public void controlled_AbandonmentDecayStripsOwnership() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        arena.setController("Vikings", "Viking Clan", UUID.randomUUID());
        assertEquals(100.0, arena.getProgress(), 0.001);

        // 50 ticks × 2% = 100% drained
        for (int i = 0; i < 50; i++) engine.handleAbandonment(arena);

        assertEquals(0.0, arena.getProgress(), 0.001);
        assertNull(arena.getControllerTeamId());
        assertEquals(ArenaState.NEUTRAL, arena.getState());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. EARLY LOSE_CONTROL_THRESHOLD — defender loses control above 0%
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("10. lose_control_threshold=30.0 — defender loses control when knocked below 28%")
    public void earlyThreshold_DefenderLosesControlAboveZero() {
        // threshold=30.0, buffer=2.0 → effective=28.0
        ArenaMechanicsConfig threshCfg = new ArenaMechanicsConfig(
                true, CaptureModeType.STANDARD_HILL,
                10.0, 0.0, 4, true, 30.0,
                0, 0, true, 2.0, 2.0, 1
        );
        StandardHillEngine engine = new StandardHillEngine(teams, threshCfg);
        TestArena arena = new TestArena("thresh", threshCfg);

        arena.setController("Vikings", "Viking Clan", UUID.randomUUID());

        // Knock 100 → 30 (7 ticks)
        for (int i = 0; i < 7; i++) engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(30.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId(), "still in control at exactly 30%");

        // Set Bob as capper so threshold can identify invader
        arena.setCappingTeam("Spartans", "Spartan Clan");

        // Knock one more tick → 20, below effective threshold 28
        engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(20.0, arena.getProgress(), 0.001);

        // Simulate tickGameLoop section 8
        arena.applyEarlyThresholdCheck();

        assertNull(arena.getControllerTeamId(), "control must be stripped below threshold");
        assertEquals("Spartans", arena.getCappingTeamId(), "pad handed to invader");
        assertEquals(20.0, arena.getProgress(), 0.001, "progress preserved at point of threshold breach");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. MILESTONE REPLAY PREVENTION post-capture
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("11. lastAnnouncedMilestone is 100 post-capture, milestones don't replay during knockdown")
    public void milestones_NoReplayDuringKnockdown() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        for (int i = 0; i < 10; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(100.0, arena.getProgress(), 0.001);

        assertEquals(100, arena.getLastAnnouncedMilestone(),
                "milestone tracker must be 100 after capture to block knockdown replay");
        assertNull(arena.getCappingTeamName(),
                "capping team name must be null after capture");

        // Bob knocks to 50%
        for (int i = 0; i < 5; i++) engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(50.0, arena.getProgress(), 0.001);

        // Milestone stays at 100 during controlled knockdown
        assertEquals(100, arena.getLastAnnouncedMilestone(),
                "milestone must not reset while pad is still controlled");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 12. SOLO MODE — Bob displaces Alice's partial capture
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("12. Solo mode: Bob knocks Alice's partial progress to 0% and begins his own capture")
    public void solo_BobDisplacesAlice() {
        TestArena arena = arena();
        arena.setOccupancyMode(OccupancyMode.SOLO);
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        Player soloAlice = mockPlayer(UUID.randomUUID(), "Alice");
        Player soloBob   = mockPlayer(UUID.randomUUID(), "Bob");

        // Alice: 4 ticks → 40%
        for (int i = 0; i < 4; i++) engine.evaluateCapture(arena, List.of(soloAlice), false);
        assertEquals(40.0, arena.getProgress(), 0.001);
        assertEquals(soloAlice.getUniqueId().toString(), arena.getCappingTeamId());

        // Bob wipes 40 → 0 (4 ticks)
        for (int i = 0; i < 4; i++) engine.evaluateCapture(arena, List.of(soloBob), false);
        assertEquals(0.0, arena.getProgress(), 0.001);

        // Bob's first positive tick
        engine.evaluateCapture(arena, List.of(soloBob), false);
        assertEquals(10.0, arena.getProgress(), 0.001);
        assertEquals(soloBob.getUniqueId().toString(), arena.getCappingTeamId());
        assertEquals("Bob", arena.getCappingTeamName());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 13. DEFENDER OUTPACES INVADER — net positive, progress heals
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("13. Bob chips away while Alice defends — defender heals faster, reaches 100%")
    public void controlled_DefenderHealsToFull() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        arena.setController("Vikings", "Viking Clan", UUID.randomUUID());

        // Bob knocks 100 → 70
        for (int i = 0; i < 3; i++) engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals(70.0, arena.getProgress(), 0.001);

        // Alice heals 70 → 100
        for (int i = 0; i < 3; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(100.0, arena.getProgress(), 0.001);
        assertEquals("Vikings", arena.getControllerTeamId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 14. CONTESTED THEN UNCONTESTED — contest clears after rival leaves
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("14. Contest clears after Bob leaves — Alice resumes capture from frozen point")
    public void contested_ClearsAfterRivalLeaves() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        // Alice to 40%
        for (int i = 0; i < 4; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(40.0, arena.getProgress(), 0.001);

        // Contested: frozen
        engine.evaluateCapture(arena, List.of(alice, bob), true);
        assertEquals(40.0, arena.getProgress(), 0.001, "must be frozen while contested");

        // Bob leaves
        engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals(50.0, arena.getProgress(), 0.001, "must resume after contest clears");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 15. FULL ROUND-TRIP — Alice captures, Bob fully steals it
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("15. Full round-trip: Alice captures → Bob knocks to 0% → Bob recaptures")
    public void fullRoundTrip_AliceCapturesBobSteals() {
        TestArena arena = arena();
        StandardHillEngine engine = new StandardHillEngine(teams, cfg);

        // Alice captures
        for (int i = 0; i < 10; i++) engine.evaluateCapture(arena, List.of(alice), false);
        assertEquals("Vikings", arena.getControllerTeamId());
        assertEquals(ArenaState.CONTROLLED, arena.getState());

        // Bob knocks to 0%
        for (int i = 0; i < 10; i++) engine.evaluateCapture(arena, List.of(bob), false);
        assertNull(arena.getControllerTeamId());
        assertEquals(0.0, arena.getProgress(), 0.001);

        // Bob captures from neutral
        for (int i = 0; i < 10; i++) engine.evaluateCapture(arena, List.of(bob), false);
        assertEquals("Spartans", arena.getControllerTeamId());
        assertEquals(100.0, arena.getProgress(), 0.001);
        assertEquals(ArenaState.CONTROLLED, arena.getState());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private TestArena arena() {
        return new TestArena("test_outpost", cfg);
    }

    private static Player mockPlayer(UUID uuid, String name) {
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

    // ── Dummy team provider ────────────────────────────────────────────────────

    static class DummyTeamProvider implements TeamRosterProvider {
        private final Map<UUID, String> ids = new HashMap<>();
        private final Map<String, String> names = new HashMap<>();

        public void register(Player p, String id, String name) {
            ids.put(p.getUniqueId(), id);
            names.put(id, name);
        }

        @Override public String getProviderName() { return "Dummy"; }
        @Override public boolean hasTeam(Player p) { return ids.containsKey(p.getUniqueId()); }
        @Override public String getTeamId(Player p) { return ids.get(p.getUniqueId()); }
        @Override public String getTeamName(Player p) { return names.get(ids.get(p.getUniqueId())); }
        @Override public UUID getTeamLeader(Player p) { return null; }
        @Override public List<Player> getOnlineMembers(Player p) { return Collections.singletonList(p); }
        @Override public boolean areAllies(String a, String b) { return false; }
    }

    // ── Minimal TestArena stub ─────────────────────────────────────────────────

    static class TestArena implements OutpostArena {
        private final String id;
        private final ArenaMechanicsConfig cfg;
        private OccupancyMode mode = OccupancyMode.TEAM;
        private double progress = 0.0;
        private String controllerId, controllerName;
        private String cappingId, cappingName;
        private long lockout = 0;
        private boolean active = true;
        private int lastAnnouncedMilestone = 0;

        TestArena(String id, ArenaMechanicsConfig cfg) {
            this.id  = id;
            this.cfg = cfg;
        }

        public void setOccupancyMode(OccupancyMode m) { this.mode = m; }
        public void setLockout(long ticks) { this.lockout = ticks; }
        public void tickLockout() { if (lockout > 0) lockout--; }
        public int getLastAnnouncedMilestone() { return lastAnnouncedMilestone; }

        /** Mirrors DefaultOutpostArena tickGameLoop section 8 early-loss check. */
        public void applyEarlyThresholdCheck() {
            if (controllerId == null || cfg == null) return;
            double threshold = cfg.getLoseControlThreshold();
            if (threshold < 100.0 - 1e-4) {
                double effective = threshold - cfg.getHysteresisBufferPercent();
                if (cappingId != null && !cappingId.equalsIgnoreCase(controllerId) && progress < effective) {
                    String invId = cappingId;
                    String invName = cappingName;
                    double saved = Math.max(0.0, progress);
                    resetToNeutral();
                    this.progress    = saved;
                    this.cappingId   = invId;
                    this.cappingName = invName;
                }
            }
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
        @Override public CaptureModeType getCaptureModeType() { return CaptureModeType.STANDARD_HILL; }
        @Override public OccupancyMode getOccupancyMode() { return mode; }
        @Override public double getMultiplier(String key) { return 1.0; }
        @Override public ArenaView createSnapshot() { return null; }
        @Override public void tickGameLoop() {}
        @Override public void setProgress(double p) {
            this.progress = Math.max(0.0, Math.min(100.0, p));
        }
        @Override public void setController(String teamId, String teamName, UUID capturer) {
            this.controllerId   = teamId;
            this.controllerName = teamName;
            this.cappingId      = null;
            this.cappingName    = null;
            this.progress       = 100.0;
            // mirrors DefaultOutpostArena: set to 100 so milestones don't replay during knockdown
            this.lastAnnouncedMilestone = 100;
        }
        @Override public void resetToNeutral() {
            this.controllerId   = null;
            this.controllerName = null;
            this.cappingId      = null;
            this.cappingName    = null;
            this.progress       = 0.0;
            this.lastAnnouncedMilestone = 0;
        }
        @Override public boolean isWithinBounds(int x, int y, int z) { return true; }
        @Override public ArenaState getState() {
            if (lockout > 0)           return ArenaState.LOCKED;
            if (controllerId != null)  return ArenaState.CONTROLLED;
            if (cappingId != null && progress > 0) return ArenaState.CAPTURING;
            return ArenaState.NEUTRAL;
        }
        @Override public double getProgress() { return progress; }
        @Override public String getControllerTeamId() { return controllerId; }
        @Override public String getControllerTeamName() { return controllerName; }
        @Override public String getCappingTeamId() { return cappingId; }
        @Override public String getCappingTeamName() { return cappingName; }
        @Override public void setCappingTeam(String id, String name) { cappingId = id; cappingName = name; }
        @Override public int getCapperCount() { return 0; }
        @Override public boolean isContested() { return false; }
        @Override public boolean isLocked() { return lockout > 0; }
        @Override public long getLockoutRemainingSeconds() { return lockout; }
        @Override public Location getCenterLocation() { return null; }
        @Override public Location getWarpLocation() { return null; }
        @Override public void setWarpLocation(Location l) {}
        @Override public boolean isActive() { return active; }
        @Override public void setActive(boolean a) { this.active = a; }
        private final DynamicLocationConfig dlc = DynamicLocationConfig.createDisabled();
        @Override public DynamicLocationConfig getDynamicLocationConfig() { return dlc; }
        @Override public ArenaRegion getCurrentRegion() { return null; }
        @Override public void shiftToRegion(ArenaRegion t) {}
        @Override public long getNextShiftSeconds() { return -1; }
        @Override public int getActivationGraceRemainingSeconds() { return 0; }
        @Override public boolean isWarmingUp() { return false; }
        @Override public Map<String, String> getCustomLangOverrides() { return Collections.emptyMap(); }
        @Override public void setCustomLangOverrides(Map<String, String> o) {}
        @Override public String getCustomLang(String path) { return null; }
        @Override public boolean isBoundingParticlesEnabled() { return false; }
        @Override public void setBoundingParticlesEnabled(boolean e) {}
    }
}
