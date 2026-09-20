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
 * Passive Decay capture engine:
 * Standard capture curve with aggressive decay bleed whenever unoccupied.
 * Strictly adheres to the Neutralization-First lifecycle:
 * - Neutral progress held by a rival is knocked down to 0% before rival accumulates progress.
 * - Invaders knock defending progress down to 0% to force neutralization.
 * - Abandoned outposts bleed progress back to 0.0%.
 */
public class PassiveDecayEngine implements CaptureModeEngine {

    private static final double EPSILON = 1e-4;

    private final TeamRosterProvider teamProvider;
    private final ArenaMechanicsConfig config;

    public PassiveDecayEngine(@NotNull TeamRosterProvider teamProvider, @NotNull ArenaMechanicsConfig config) {
        this.teamProvider = teamProvider;
        this.config = config;
    }

    @Override
    public @NotNull CaptureModeType getType() {
        return CaptureModeType.PASSIVE_DECAY;
    }

    @Override
    public void evaluateCapture(@NotNull OutpostArena arena, @NotNull List<Player> validCappers, boolean isContested) {
        if (validCappers.isEmpty() || arena.isLocked()) {
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
        double step = config.getPercentPerTick() + Math.max(0, capperCount - 1) * config.getScalingPerMember();

        String controllerId = arena.getControllerTeamId();
        double currentProgress = arena.getProgress();
        boolean isControllerOrAlly = controllerId != null && (controllerId.equalsIgnoreCase(cappingTeamId) || (!isSolo && teamProvider.areAllies(controllerId, cappingTeamId)));

        if (controllerId == null) {
            String currentCappingId = arena.getCappingTeamId();
            boolean isSameOrAlly = currentCappingId == null || currentCappingId.equalsIgnoreCase(cappingTeamId)
                    || (!isSolo && teamProvider.areAllies(currentCappingId, cappingTeamId));

            if (isSameOrAlly) {
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
                // Rival team knocking down existing progress to 0% first
                double newProgress = Math.max(0.0, currentProgress - step);
                arena.setProgress(newProgress);

                if (arena instanceof DefaultOutpostArena defArena) {
                    defArena.setClearingTeamName(cappingTeamName);
                }

                if (newProgress <= EPSILON) {
                    arena.setCappingTeam(cappingTeamId, cappingTeamName);
                    if (arena instanceof DefaultOutpostArena defArena) {
                        defArena.setClearingTeamName(null);
                    }
                }
            }
        } else if (isControllerOrAlly) {
            if (currentProgress < 100.0) {
                arena.setProgress(Math.min(100.0, currentProgress + step));
            }
        } else {
            double newProgress = Math.max(0.0, currentProgress - step);
            arena.setProgress(newProgress);
            if (newProgress <= EPSILON) {
                arena.resetToNeutral();
                arena.setCappingTeam(cappingTeamId, cappingTeamName);
                if (arena instanceof DefaultOutpostArena defArena) {
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
        if (arena.isLocked()) {
            return;
        }

        double decayRate = config.getPassiveDecayRate();
        double current = arena.getProgress();

        if (arena.getControllerTeamId() == null && current > EPSILON) {
            double newProgress = Math.max(0.0, current - decayRate);
            arena.setProgress(newProgress);
            if (newProgress <= EPSILON && arena instanceof DefaultOutpostArena defArena) {
                defArena.setCappingTeam(null, null);
                defArena.setClearingTeamName(null);
            }
        } else if (arena.getControllerTeamId() != null && current > EPSILON) {
            double newProgress = Math.max(0.0, current - decayRate);
            arena.setProgress(newProgress);
            if (newProgress <= EPSILON) {
                arena.resetToNeutral();
            }
        }
    }
}
