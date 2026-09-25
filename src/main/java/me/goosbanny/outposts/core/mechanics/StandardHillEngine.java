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

import java.util.List;

/**
 * Classical King-of-the-Hill capture engine:
 * 0% -> [Neutral Zone] -> 100% [Capped].
 * Strictly adheres to the Neutralization-First lifecycle:
 * - Rival team knocks existing neutral progress from X% to 0% first before gaining progress.
 * - Invading teams must knock defenders down to 0% before advancing their own capture bar.
 * - Abandoned outposts bleed down to 0.0% and reset ownership.
 */
public class StandardHillEngine implements CaptureModeEngine {

    private static final double EPSILON = 1e-4;

    private final TeamRosterProvider teamProvider;
    private final ArenaMechanicsConfig config;

    public StandardHillEngine(@NotNull TeamRosterProvider teamProvider, @NotNull ArenaMechanicsConfig config) {
        this.teamProvider = teamProvider;
        this.config = config;
    }

    @Override
    public @NotNull CaptureModeType getType() {
        return CaptureModeType.STANDARD_HILL;
    }

    @Override
    public void evaluateCapture(@NotNull OutpostArena arena, @NotNull List<Player> validCappers, boolean isContested) {
        if (validCappers.size() < config.getMinCappersRequired() || arena.isLocked()) {
            return;
        }

        if (isContested && config.isFreezeWhenContested()) {
            return;
        }

        Player primaryPlayer = resolvePrimaryCapper(arena, validCappers);
        if (primaryPlayer == null) return;

        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;
        String cappingTeamId = isSolo
                ? primaryPlayer.getUniqueId().toString()
                : (teamProvider.getTeamId(primaryPlayer) != null ? teamProvider.getTeamId(primaryPlayer) : primaryPlayer.getUniqueId().toString());
        String cappingTeamName = isSolo
                ? primaryPlayer.getName()
                : (teamProvider.getTeamName(primaryPlayer) != null ? teamProvider.getTeamName(primaryPlayer) : primaryPlayer.getName());

        if (cappingTeamId == null) {
            return;
        }

        int capperCount = Math.min(validCappers.size(), config.getMaxCappersCounted());
        double step = config.getPercentPerSecond() + Math.max(0, capperCount - 1) * config.getScalingPerMember();
        double uncaptureStep = config.getUncapturePercentPerSecond() + Math.max(0, capperCount - 1) * config.getScalingPerMember();

        String controllerId = arena.getControllerTeamId();
        double currentProgress = arena.getProgress();
        boolean isControllerOrAlly = controllerId != null && (controllerId.equalsIgnoreCase(cappingTeamId) || (!isSolo && teamProvider.areAllies(controllerId, cappingTeamId)));

        if (controllerId == null) {
            // Neutral outpost being capped
            String currentCappingId = arena.getCappingTeamId();
            boolean isSameOrAlly = currentCappingId == null || currentCappingId.equalsIgnoreCase(cappingTeamId)
                    || (!isSolo && teamProvider.areAllies(currentCappingId, cappingTeamId));

            if (isSameOrAlly) {
                // Initial capture or continuing capture by the same team or ally
                if (currentCappingId == null) {
                    arena.setCappingTeam(cappingTeamId, cappingTeamName);
                }
                if (arena instanceof DefaultOutpostArena defArena) {
                    defArena.setClearingTeamName(null);
                }

                double newProgress = Math.min(100.0, currentProgress + step);
                arena.setProgress(newProgress);

                if (newProgress >= 100.0 - EPSILON) {
                    String finalId = currentCappingId != null ? currentCappingId : cappingTeamId;
                    String finalName = currentCappingId != null && arena.getControllerTeamName() != null ? arena.getControllerTeamName() : cappingTeamName;
                    arena.setController(finalId, finalName, primaryPlayer.getUniqueId());
                }
            } else {
                // Rival team is knocking down existing neutral progress to 0% first!
                double newProgress = Math.max(0.0, currentProgress - uncaptureStep);
                arena.setProgress(newProgress);

                if (arena instanceof DefaultOutpostArena defArena) {
                    defArena.setClearingTeamName(cappingTeamName);
                }

                if (newProgress <= EPSILON) {
                    // Previous progress has been fully wiped out!
                    arena.setCappingTeam(cappingTeamId, cappingTeamName);
                    if (arena instanceof DefaultOutpostArena defArena) {
                        defArena.setClearingTeamName(null);
                    }
                }
            }
        } else if (isControllerOrAlly) {
            // Defending team or their ally is recovering damaged progress back to 100%
            if (currentProgress < 100.0) {
                // Clear any tracked attacker — defender is actively healing
                if (arena.getCappingTeamId() != null) {
                    arena.setCappingTeam(null, null);
                }
                double newProgress = Math.min(100.0, currentProgress + step);
                arena.setProgress(newProgress);
            }
        } else {
            // Invading rival team is knocking down defending controller's progress
            if (arena instanceof DefaultOutpostArena defArena && defArena.isKnockDelayActive()) {
                return; // Grace delay active before knockdown begins
            }
            // Track the active attacker so tickGameLoop section 8 can enforce lose_control_threshold
            arena.setCappingTeam(cappingTeamId, cappingTeamName);

            double newProgress = Math.max(0.0, currentProgress - uncaptureStep);
            arena.setProgress(newProgress);

            if (newProgress <= EPSILON) {
                // Controller lost ownership — outpost resets to neutral
                arena.resetToNeutral();
                if (arena instanceof DefaultOutpostArena defArena) {
                    defArena.setCappingTeam(cappingTeamId, cappingTeamName);
                    defArena.setClearingTeamName(null);
                }
            }
        }
    }

    private Player resolvePrimaryCapper(OutpostArena arena, List<Player> validCappers) {
        if (validCappers.isEmpty()) return null;
        if (validCappers.size() == 1) return validCappers.get(0);

        String controllerId = arena.getControllerTeamId();
        String currentCapperId = arena.getCappingTeamId();
        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;

        if (controllerId != null && !isSolo) {
            for (Player p : validCappers) {
                String tid = teamProvider.getTeamId(p);
                if (tid != null && (tid.equalsIgnoreCase(controllerId) || teamProvider.areAllies(tid, controllerId))) {
                    return p;
                }
            }
        }

        if (currentCapperId != null && !isSolo) {
            for (Player p : validCappers) {
                String tid = teamProvider.getTeamId(p);
                if (tid != null && (tid.equalsIgnoreCase(currentCapperId) || teamProvider.areAllies(tid, currentCapperId))) {
                    return p;
                }
            }
        }

        return validCappers.get(0);
    }

    @Override
    public void handleAbandonment(@NotNull OutpostArena arena) {
        if (!config.isPassiveDecayEnabled() || arena.isLocked()) {
            return;
        }

        double decayRate = config.getPassiveDecayRate();
        double currentProgress = arena.getProgress();

        // 1. If neutral and partially capped, bleed progress to 0
        if (arena.getControllerTeamId() == null && currentProgress > EPSILON) {
            double newProgress = Math.max(0.0, currentProgress - decayRate);
            arena.setProgress(newProgress);
            if (newProgress <= EPSILON && arena instanceof DefaultOutpostArena defArena) {
                defArena.setCappingTeam(null, null);
                defArena.setClearingTeamName(null);
            }
        }
        // 2. If controlled and abandoned, bleed progress down towards 0%
        else if (arena.getControllerTeamId() != null && currentProgress > EPSILON) {
            double newProgress = Math.max(0.0, currentProgress - decayRate);
            arena.setProgress(newProgress);
            if (newProgress <= EPSILON) {
                // Controlling team lost ownership due to abandonment decay!
                arena.resetToNeutral();
            }
        }
    }
}
