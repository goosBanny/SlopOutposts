package me.goosbanny.outposts.api.arena;

import net.kyori.adventure.text.Component;

/**
 * Immutable value record snapshot of an arena's state at a single moment in time.
 * Guaranteed thread-safe with zero lock contention.
 */
public record ArenaViewSnapshot(
        String id,
        Component displayName,
        ArenaState state,
        double progress,
        String controllerTeam,
        String cappingTeam,
        int capperCount,
        boolean isContested,
        long lockoutRemainingSeconds,
        OccupancyMode occupancyMode,
        long activeDurationRemainingSeconds
) implements ArenaView {

    public ArenaViewSnapshot(
            String id,
            Component displayName,
            ArenaState state,
            double progress,
            String controllerTeam,
            String cappingTeam,
            int capperCount,
            boolean isContested,
            long lockoutRemainingSeconds,
            OccupancyMode occupancyMode
    ) {
        this(id, displayName, state, progress, controllerTeam, cappingTeam, capperCount, isContested, lockoutRemainingSeconds, occupancyMode, -1L);
    }

    @Override
    public OccupancyMode getOccupancyMode() {
        return occupancyMode;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public ArenaState getState() {
        return state;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public String getControllerTeam() {
        return controllerTeam;
    }

    @Override
    public String getCappingTeam() {
        return cappingTeam;
    }

    @Override
    public int getCapperCount() {
        return capperCount;
    }

    @Override
    public boolean isContested() {
        return isContested;
    }

    @Override
    public long getLockoutRemainingSeconds() {
        return lockoutRemainingSeconds;
    }

    @Override
    public long getActiveDurationRemainingSeconds() {
        return activeDurationRemainingSeconds;
    }
}
