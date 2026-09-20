package me.goosbanny.outposts.core.mechanics;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeEngine;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.arena.ArenaMechanicsConfig;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tug-of-War capture engine:
 * Symmetrical contestation between opposing factions.
 * Strictly adheres to the Neutralization-First lifecycle:
 * - Neutral progress held by a rival is knocked down to 0% before positive capture begins.
 * - Invaders knock defending controller down to 0% to force neutralization before capping.
 * - Abandoned outposts drift toward 50% neutral midpoint or bleed down if held.
 */
public class TugOfWarEngine implements CaptureModeEngine {

    private static final double EPSILON = 1e-4;

    private final TeamRosterProvider teamProvider;
    private final ArenaMechanicsConfig config;

    public TugOfWarEngine(@NotNull TeamRosterProvider teamProvider, @NotNull ArenaMechanicsConfig config) {
        this.teamProvider = teamProvider;
        this.config = config;
    }

    @Override
    public @NotNull CaptureModeType getType() {
        return CaptureModeType.TUG_OF_WAR;
    }

    @Override
    public void evaluateCapture(@NotNull OutpostArena arena, @NotNull List<Player> validCappers, boolean isContested) {
        if (validCappers.isEmpty() || arena.isLocked()) {
            return;
        }

        // Tally cappers per team
        Map<String, Integer> teamCounts = new HashMap<>();
        Map<String, String> teamNames = new HashMap<>();
        Map<String, Player> firstPlayers = new HashMap<>();

        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;
        for (Player p : validCappers) {
            String teamId = isSolo
                    ? p.getUniqueId().toString()
                    : (teamProvider.getTeamId(p) != null ? teamProvider.getTeamId(p) : p.getUniqueId().toString());
            String teamName = isSolo
                    ? p.getName()
                    : (teamProvider.getTeamName(p) != null ? teamProvider.getTeamName(p) : p.getName());
            if (teamId != null) {
                teamCounts.put(teamId, teamCounts.getOrDefault(teamId, 0) + 1);
                teamNames.putIfAbsent(teamId, teamName);
                firstPlayers.putIfAbsent(teamId, p);
            }
        }

        if (teamCounts.isEmpty()) {
            return;
        }

        // Merge allied team counts if not solo
        if (!isSolo && teamCounts.size() > 1) {
            List<String> teamList = new ArrayList<>(teamCounts.keySet());
            for (int i = 0; i < teamList.size(); i++) {
                String a = teamList.get(i);
                if (!teamCounts.containsKey(a)) continue;
                for (int j = i + 1; j < teamList.size(); j++) {
                    String b = teamList.get(j);
                    if (!teamCounts.containsKey(b)) continue;
                    if (teamProvider.areAllies(a, b)) {
                        // Merge b into a
                        teamCounts.put(a, teamCounts.get(a) + teamCounts.get(b));
                        teamCounts.remove(b);
                    }
                }
            }
        }

        // If only one team (or allied faction block) is present, pull toward 100%
        if (teamCounts.size() == 1) {
            String teamId = teamCounts.keySet().iterator().next();
            int count = Math.min(teamCounts.get(teamId), config.getMaxCappersCounted());
            double step = config.getPercentPerSecond() + Math.max(0, count - 1) * config.getScalingPerMember();
            double uncaptureStep = config.getUncapturePercentPerSecond() + Math.max(0, count - 1) * config.getScalingPerMember();

            String controllerId = arena.getControllerTeamId();
            double progress = arena.getProgress();
            boolean isControllerOrAlly = controllerId != null && (controllerId.equalsIgnoreCase(teamId) || (!isSolo && teamProvider.areAllies(controllerId, teamId)));

            if (controllerId == null) {
                String currentCappingId = arena.getCappingTeamId();
                boolean isSameOrAlly = currentCappingId == null || currentCappingId.equalsIgnoreCase(teamId)
                        || (!isSolo && teamProvider.areAllies(currentCappingId, teamId));

                if (isSameOrAlly) {
                    if (currentCappingId == null) {
                        arena.setCappingTeam(teamId, teamNames.get(teamId));
                    }
                    if (arena instanceof DefaultOutpostArena defArena) {
                        defArena.setClearingTeamName(null);
                    }

                    double newProgress = Math.min(100.0, progress + step);
                    arena.setProgress(newProgress);
                    if (newProgress >= 100.0 - EPSILON) {
                        String finalId = currentCappingId != null ? currentCappingId : teamId;
                        String finalName = currentCappingId != null && arena.getControllerTeamName() != null ? arena.getControllerTeamName() : teamNames.get(teamId);
                        arena.setController(finalId, finalName, firstPlayers.get(teamId) != null ? firstPlayers.get(teamId).getUniqueId() : null);
                    }
                } else {
                    // Rival team knocking down existing progress to 0% first
                    double newProgress = Math.max(0.0, progress - uncaptureStep);
                    arena.setProgress(newProgress);

                    if (arena instanceof DefaultOutpostArena defArena) {
                        defArena.setClearingTeamName(teamNames.get(teamId));
                    }

                    if (newProgress <= EPSILON) {
                        arena.setCappingTeam(teamId, teamNames.get(teamId));
                        if (arena instanceof DefaultOutpostArena defArena) {
                            defArena.setClearingTeamName(null);
                        }
                    }
                }
            } else if (isControllerOrAlly) {
                if (progress < 100.0) {
                    arena.setProgress(Math.min(100.0, progress + step));
                }
            } else {
                // Rival knocking controller down to 0%
                if (arena instanceof DefaultOutpostArena defArena && defArena.isKnockDelayActive()) {
                    return; // Grace delay active before knockdown begins
                }
                double newProgress = Math.max(0.0, progress - uncaptureStep);
                arena.setProgress(newProgress);
                if (newProgress <= EPSILON) {
                    arena.resetToNeutral();
                    arena.setCappingTeam(teamId, teamNames.get(teamId));
                    if (arena instanceof DefaultOutpostArena defArena) {
                        defArena.setClearingTeamName(null);
                    }
                }
            }
        } else {
            // Contested tug of war: pull according to dominant advantage if freeze disabled
            if (!config.isFreezeWhenContested()) {
                String dominantId = null;
                int maxCount = -1;
                int totalCount = 0;
                for (Map.Entry<String, Integer> entry : teamCounts.entrySet()) {
                    int count = entry.getValue();
                    totalCount += count;
                    if (count > maxCount) {
                        maxCount = count;
                        dominantId = entry.getKey();
                    }
                }

                if (dominantId != null) {
                    int totalOthers = totalCount - maxCount;
                    int diff = maxCount - totalOthers;
                    if (diff > 0) {
                        double delta = config.getPercentPerTick() * 0.5;
                        double current = arena.getProgress();
                        String controllerId = arena.getControllerTeamId();
                        String cappingId = arena.getCappingTeamId();

                        boolean pullUp = (controllerId != null && controllerId.equalsIgnoreCase(dominantId))
                                || (controllerId == null && (cappingId == null || cappingId.equalsIgnoreCase(dominantId)));

                        double newProgress = pullUp
                                ? Math.min(100.0, current + delta)
                                : Math.max(0.0, current - delta);
                        arena.setProgress(newProgress);
                    }
                }
            }
        }
    }

    @Override
    public void handleAbandonment(@NotNull OutpostArena arena) {
        if (!config.isPassiveDecayEnabled() || arena.isLocked()) {
            return;
        }

        double decayRate = config.getPassiveDecayRate();
        double current = arena.getProgress();

        // If controlled and abandoned, bleed progress to 0% and neutralize
        if (arena.getControllerTeamId() != null && current > EPSILON) {
            double newProgress = Math.max(0.0, current - decayRate);
            arena.setProgress(newProgress);
            if (newProgress <= EPSILON) {
                arena.resetToNeutral();
            }
        }
        // When abandoned and neutral, drift back toward 50% neutral midpoint
        else if (arena.getControllerTeamId() == null) {
            if (Math.abs(current - 50.0) > 0.1) {
                if (current > 50.0) {
                    arena.setProgress(Math.max(50.0, current - decayRate));
                } else {
                    arena.setProgress(Math.min(50.0, current + decayRate));
                }
            }
        }
    }
}
