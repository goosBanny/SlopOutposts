package me.goosbanny.outposts.core.mechanics;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeEngine;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.mechanics.TugOfWarTeamAssignment;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.arena.ArenaMechanicsConfig;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forced 2-Team Symmetrical Tug-of-War capture engine:
 * - 50.0% is neutral midpoint anchor.
 * - Side A (Red / Left) pulls toward 0.0% (Wins at <= 0.0%).
 * - Side B (Blue / Right) pulls toward 100.0% (Wins at >= 100.0%).
 * - Deadzone buffer around 50.0% retains deadlock/neutral state.
 * - Supports AUTO_RED_BLUE (auto-balancing players) and FIRST_TWO_FACTIONS (symmetrical duel).
 */
public class TugOfWarEngine implements CaptureModeEngine {

    private static final double EPSILON = 1e-4;

    public enum TugSide {
        SIDE_A,
        SIDE_B,
        NONE
    }

    private final TeamRosterProvider teamProvider;
    private final ArenaMechanicsConfig config;

    private final Map<UUID, String> autoPlayerTeamMap = new ConcurrentHashMap<>();
    private volatile String sideAId = null;
    private volatile String sideAName = null;
    private volatile String sideBId = null;
    private volatile String sideBName = null;

    public TugOfWarEngine(@NotNull TeamRosterProvider teamProvider, @NotNull ArenaMechanicsConfig config) {
        this.teamProvider = teamProvider;
        this.config = config;
    }

    @Override
    public @NotNull CaptureModeType getType() {
        return CaptureModeType.TUG_OF_WAR;
    }

    public @Nullable String getSideAId() {
        return sideAId;
    }

    public @Nullable String getSideAName() {
        return sideAName;
    }

    public @Nullable String getSideBId() {
        return sideBId;
    }

    public @Nullable String getSideBName() {
        return sideBName;
    }

    public @NotNull TugSide getPlayerSide(@NotNull Player player) {
        TugOfWarTeamAssignment assignment = config.getTugOfWarTeamAssignment();
        if (assignment == TugOfWarTeamAssignment.AUTO_RED_BLUE) {
            String team = autoPlayerTeamMap.get(player.getUniqueId());
            if ("RED".equalsIgnoreCase(team)) return TugSide.SIDE_A;
            if ("BLUE".equalsIgnoreCase(team)) return TugSide.SIDE_B;
            return TugSide.NONE;
        } else {
            String teamId = teamProvider.getTeamId(player);
            if (teamId == null) {
                teamId = player.getUniqueId().toString();
            }
            if (sideAId != null && (sideAId.equalsIgnoreCase(teamId) || teamProvider.areAllies(sideAId, teamId))) {
                return TugSide.SIDE_A;
            }
            if (sideBId != null && (sideBId.equalsIgnoreCase(teamId) || teamProvider.areAllies(sideBId, teamId))) {
                return TugSide.SIDE_B;
            }
            return TugSide.NONE;
        }
    }

    @Override
    public void evaluateCapture(@NotNull OutpostArena arena, @NotNull List<Player> validCappers, boolean isContested) {
        if (validCappers.isEmpty() || arena.isLocked()) {
            return;
        }

        TugOfWarTeamAssignment assignment = config.getTugOfWarTeamAssignment();
        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;
        double currentProgress = arena.getProgress();
        double deadzone = config.getDeadzoneBufferPercent();

        List<Player> sideAPlayers = new ArrayList<>();
        List<Player> sideBPlayers = new ArrayList<>();

        if (assignment == TugOfWarTeamAssignment.AUTO_RED_BLUE) {
            sideAId = "RED";
            sideAName = "RED";
            sideBId = "BLUE";
            sideBName = "BLUE";

            int redCount = 0;
            int blueCount = 0;
            for (Player p : validCappers) {
                String assigned = autoPlayerTeamMap.get(p.getUniqueId());
                if ("RED".equalsIgnoreCase(assigned)) redCount++;
                else if ("BLUE".equalsIgnoreCase(assigned)) blueCount++;
            }

            for (Player p : validCappers) {
                String assigned = autoPlayerTeamMap.get(p.getUniqueId());
                if (assigned == null) {
                    if (redCount <= blueCount) {
                        assigned = "RED";
                        redCount++;
                    } else {
                        assigned = "BLUE";
                        blueCount++;
                    }
                    autoPlayerTeamMap.put(p.getUniqueId(), assigned);
                }
                if ("RED".equalsIgnoreCase(assigned)) {
                    sideAPlayers.add(p);
                } else if ("BLUE".equalsIgnoreCase(assigned)) {
                    sideBPlayers.add(p);
                }
            }
        } else {
            // FIRST_TWO_FACTIONS
            String controllerId = arena.getControllerTeamId();
            if (controllerId != null && sideAId == null && sideBId == null) {
                if (currentProgress <= 50.0) {
                    sideAId = controllerId;
                    sideAName = arena.getControllerTeamName() != null ? arena.getControllerTeamName() : controllerId;
                } else {
                    sideBId = controllerId;
                    sideBName = arena.getControllerTeamName() != null ? arena.getControllerTeamName() : controllerId;
                }
            }

            Map<String, List<Player>> teamsOnPad = new java.util.LinkedHashMap<>();
            Map<String, String> teamNames = new java.util.LinkedHashMap<>();

            for (Player p : validCappers) {
                String teamId = isSolo
                        ? p.getUniqueId().toString()
                        : (teamProvider.getTeamId(p) != null ? teamProvider.getTeamId(p) : p.getUniqueId().toString());
                String teamName = isSolo
                        ? p.getName()
                        : (teamProvider.getTeamName(p) != null ? teamProvider.getTeamName(p) : p.getName());

                teamsOnPad.computeIfAbsent(teamId, k -> new ArrayList<>()).add(p);
                teamNames.putIfAbsent(teamId, teamName);
            }

            if (!isSolo && teamsOnPad.size() > 1) {
                List<String> keys = new ArrayList<>(teamsOnPad.keySet());
                for (int i = 0; i < keys.size(); i++) {
                    String a = keys.get(i);
                    if (!teamsOnPad.containsKey(a)) continue;
                    for (int j = i + 1; j < keys.size(); j++) {
                        String b = keys.get(j);
                        if (!teamsOnPad.containsKey(b)) continue;
                        if (teamProvider.areAllies(a, b)) {
                            teamsOnPad.get(a).addAll(teamsOnPad.remove(b));
                        }
                    }
                }
            }

            for (Map.Entry<String, List<Player>> entry : teamsOnPad.entrySet()) {
                String tid = entry.getKey();
                if (sideAId == null) {
                    sideAId = tid;
                    sideAName = teamNames.get(tid);
                } else if (sideBId == null && !tid.equalsIgnoreCase(sideAId) && (isSolo || !teamProvider.areAllies(sideAId, tid))) {
                    sideBId = tid;
                    sideBName = teamNames.get(tid);
                }
            }

            for (Map.Entry<String, List<Player>> entry : teamsOnPad.entrySet()) {
                String tid = entry.getKey();
                if (sideAId != null && (tid.equalsIgnoreCase(sideAId) || (!isSolo && teamProvider.areAllies(sideAId, tid)))) {
                    sideAPlayers.addAll(entry.getValue());
                } else if (sideBId != null && (tid.equalsIgnoreCase(sideBId) || (!isSolo && teamProvider.areAllies(sideBId, tid)))) {
                    sideBPlayers.addAll(entry.getValue());
                }
            }
        }

        int countA = sideAPlayers.size();
        int countB = sideBPlayers.size();

        if (countA == 0 && countB == 0) {
            return;
        }

        double newProgress = currentProgress;

        if (countA > 0 && countB > 0) {
            int diff = countA - countB;
            if (diff > 0) {
                double delta = config.getPercentPerSecond() * config.getContestedAdvantageScaling() * diff;
                newProgress = Math.max(0.0, currentProgress - delta);
            } else if (diff < 0) {
                double delta = config.getPercentPerSecond() * config.getContestedAdvantageScaling() * (-diff);
                newProgress = Math.min(100.0, currentProgress + delta);
            }
        } else if (countA > 0) {
            int cappedCount = Math.min(countA, config.getMaxCappersCounted());
            double step = config.getPercentPerSecond() + Math.max(0, cappedCount - 1) * config.getScalingPerMember();
            newProgress = Math.max(0.0, currentProgress - step);
        } else {
            int cappedCount = Math.min(countB, config.getMaxCappersCounted());
            double step = config.getPercentPerSecond() + Math.max(0, cappedCount - 1) * config.getScalingPerMember();
            newProgress = Math.min(100.0, currentProgress + step);
        }

        arena.setProgress(newProgress);

        if (newProgress < 50.0 - deadzone) {
            arena.setCappingTeam(sideAId, sideAName);
        } else if (newProgress > 50.0 + deadzone) {
            arena.setCappingTeam(sideBId, sideBName);
        } else {
            arena.setCappingTeam(null, null);
        }

        String currentController = arena.getControllerTeamId();
        if (currentController != null) {
            if (sideAId != null && currentController.equalsIgnoreCase(sideAId) && newProgress > 50.0 + deadzone) {
                double saved = newProgress;
                arena.resetToNeutral();
                arena.setProgress(saved);
            } else if (sideBId != null && currentController.equalsIgnoreCase(sideBId) && newProgress < 50.0 - deadzone) {
                double saved = newProgress;
                arena.resetToNeutral();
                arena.setProgress(saved);
            }
        }

        if (newProgress <= EPSILON) {
            Player capturer = sideAPlayers.isEmpty() ? null : sideAPlayers.get(0);
            arena.setController(sideAId, sideAName, capturer != null ? capturer.getUniqueId() : null);
            arena.setProgress(0.0);
            arena.setCappingTeam(null, null);
        } else if (newProgress >= 100.0 - EPSILON) {
            Player capturer = sideBPlayers.isEmpty() ? null : sideBPlayers.get(0);
            arena.setController(sideBId, sideBName, capturer != null ? capturer.getUniqueId() : null);
            arena.setProgress(100.0);
            arena.setCappingTeam(null, null);
        }
    }

    @Override
    public void handleAbandonment(@NotNull OutpostArena arena) {
        if (!config.isPassiveDecayEnabled() || arena.isLocked()) {
            return;
        }

        double decayRate = config.getPassiveDecayRate();
        double current = arena.getProgress();
        double anchor = config.getNeutralAnchorPercent();
        double deadzone = config.getDeadzoneBufferPercent();

        if (Math.abs(current - anchor) > EPSILON) {
            double newProgress;
            if (current > anchor) {
                newProgress = Math.max(anchor, current - decayRate);
            } else {
                newProgress = Math.min(anchor, current + decayRate);
            }
            arena.setProgress(newProgress);

            if (Math.abs(newProgress - anchor) <= deadzone) {
                arena.setCappingTeam(null, null);
                if (arena.getControllerTeamId() != null) {
                    double saved = newProgress;
                    arena.resetToNeutral();
                    arena.setProgress(saved);
                }
                if (Math.abs(newProgress - anchor) <= EPSILON) {
                    resetSides();
                }
            }
        }
    }

    public void resetSides() {
        this.sideAId = null;
        this.sideAName = null;
        this.sideBId = null;
        this.sideBName = null;
        this.autoPlayerTeamMap.clear();
    }
}
