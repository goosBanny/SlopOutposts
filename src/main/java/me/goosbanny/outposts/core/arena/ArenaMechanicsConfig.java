package me.goosbanny.outposts.core.arena;

import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.mechanics.TugOfWarTeamAssignment;

/**
 * Immutable configuration settings for arena capture mechanics and behavior parameters.
 */
public class ArenaMechanicsConfig {

    private final boolean enabled;
    private final boolean autoStart;
    private final CaptureModeType mode;
    private final double percentPerSecond;
    private final double uncapturePercentPerSecond;
    private final double scalingPerMember;
    private final int maxCappersCounted;
    private final boolean freezeWhenContested;
    private final double loseControlThreshold;
    private final long lockoutSeconds;
    private final long knockDelaySeconds;
    private final boolean passiveDecayEnabled;
    private final double passiveDecayRate;
    private final double hysteresisBufferPercent;
    private final double stateChangeCooldownSeconds;

    private final int minCappersRequired;
    private final double neutralAnchorPercent;
    private final double contestedAdvantageScaling;
    private final double neutralDriftRate;
    private final int targetTickets;
    private final double ticketsPerSecond;
    private final TugOfWarTeamAssignment tugOfWarTeamAssignment;
    private final double deadzoneBufferPercent;

    public ArenaMechanicsConfig(
            boolean enabled,
            CaptureModeType mode,
            double percentPerSecond,
            double scalingPerMember,
            int maxCappersCounted,
            boolean freezeWhenContested,
            double loseControlThreshold,
            long lockoutSeconds,
            long knockDelaySeconds,
            boolean passiveDecayEnabled,
            double passiveDecayRate,
            double hysteresisBufferPercent,
            double stateChangeCooldownSeconds
    ) {
        this(enabled, false, mode, percentPerSecond, percentPerSecond, scalingPerMember, maxCappersCounted,
                freezeWhenContested, loseControlThreshold, lockoutSeconds, knockDelaySeconds,
                passiveDecayEnabled, passiveDecayRate, hysteresisBufferPercent, stateChangeCooldownSeconds);
    }

    public ArenaMechanicsConfig(
            boolean enabled,
            boolean autoStart,
            CaptureModeType mode,
            double percentPerSecond,
            double scalingPerMember,
            int maxCappersCounted,
            boolean freezeWhenContested,
            double loseControlThreshold,
            long lockoutSeconds,
            long knockDelaySeconds,
            boolean passiveDecayEnabled,
            double passiveDecayRate,
            double hysteresisBufferPercent,
            double stateChangeCooldownSeconds
    ) {
        this(enabled, autoStart, mode, percentPerSecond, percentPerSecond, scalingPerMember, maxCappersCounted,
                freezeWhenContested, loseControlThreshold, lockoutSeconds, knockDelaySeconds,
                passiveDecayEnabled, passiveDecayRate, hysteresisBufferPercent, stateChangeCooldownSeconds);
    }

    public ArenaMechanicsConfig(
            boolean enabled,
            boolean autoStart,
            CaptureModeType mode,
            double percentPerSecond,
            double uncapturePercentPerSecond,
            double scalingPerMember,
            int maxCappersCounted,
            boolean freezeWhenContested,
            double loseControlThreshold,
            long lockoutSeconds,
            long knockDelaySeconds,
            boolean passiveDecayEnabled,
            double passiveDecayRate,
            double hysteresisBufferPercent,
            double stateChangeCooldownSeconds
    ) {
        this(enabled, autoStart, mode, percentPerSecond, uncapturePercentPerSecond, scalingPerMember, maxCappersCounted,
                freezeWhenContested, loseControlThreshold, lockoutSeconds, knockDelaySeconds,
                passiveDecayEnabled, passiveDecayRate, hysteresisBufferPercent, stateChangeCooldownSeconds,
                1, 50.0, 0.5, passiveDecayRate > 0 ? passiveDecayRate : 2.5, 1000, 10.0,
                TugOfWarTeamAssignment.FIRST_TWO_FACTIONS, 2.5);
    }

    public ArenaMechanicsConfig(
            boolean enabled,
            boolean autoStart,
            CaptureModeType mode,
            double percentPerSecond,
            double uncapturePercentPerSecond,
            double scalingPerMember,
            int maxCappersCounted,
            boolean freezeWhenContested,
            double loseControlThreshold,
            long lockoutSeconds,
            long knockDelaySeconds,
            boolean passiveDecayEnabled,
            double passiveDecayRate,
            double hysteresisBufferPercent,
            double stateChangeCooldownSeconds,
            int minCappersRequired,
            double neutralAnchorPercent,
            double contestedAdvantageScaling,
            double neutralDriftRate,
            int targetTickets,
            double ticketsPerSecond
    ) {
        this(enabled, autoStart, mode, percentPerSecond, uncapturePercentPerSecond, scalingPerMember, maxCappersCounted,
                freezeWhenContested, loseControlThreshold, lockoutSeconds, knockDelaySeconds,
                passiveDecayEnabled, passiveDecayRate, hysteresisBufferPercent, stateChangeCooldownSeconds,
                minCappersRequired, neutralAnchorPercent, contestedAdvantageScaling, neutralDriftRate, targetTickets, ticketsPerSecond,
                TugOfWarTeamAssignment.FIRST_TWO_FACTIONS, 2.5);
    }

    public ArenaMechanicsConfig(
            boolean enabled,
            boolean autoStart,
            CaptureModeType mode,
            double percentPerSecond,
            double uncapturePercentPerSecond,
            double scalingPerMember,
            int maxCappersCounted,
            boolean freezeWhenContested,
            double loseControlThreshold,
            long lockoutSeconds,
            long knockDelaySeconds,
            boolean passiveDecayEnabled,
            double passiveDecayRate,
            double hysteresisBufferPercent,
            double stateChangeCooldownSeconds,
            int minCappersRequired,
            double neutralAnchorPercent,
            double contestedAdvantageScaling,
            double neutralDriftRate,
            int targetTickets,
            double ticketsPerSecond,
            TugOfWarTeamAssignment tugOfWarTeamAssignment,
            double deadzoneBufferPercent
    ) {
        this.enabled = enabled;
        this.autoStart = autoStart;
        this.mode = mode != null ? mode : CaptureModeType.STANDARD_HILL;
        this.percentPerSecond = percentPerSecond > 0 ? percentPerSecond : 2.5;
        this.uncapturePercentPerSecond = uncapturePercentPerSecond > 0 ? uncapturePercentPerSecond : this.percentPerSecond;
        this.scalingPerMember = scalingPerMember >= 0 ? scalingPerMember : 0.5;
        this.maxCappersCounted = maxCappersCounted > 0 ? maxCappersCounted : 4;
        this.freezeWhenContested = freezeWhenContested;
        this.loseControlThreshold = loseControlThreshold > 0 ? loseControlThreshold : 100.0;
        this.lockoutSeconds = lockoutSeconds >= 0 ? lockoutSeconds : 10;
        this.knockDelaySeconds = knockDelaySeconds >= 0 ? knockDelaySeconds : 5;
        this.passiveDecayEnabled = passiveDecayEnabled;
        this.passiveDecayRate = passiveDecayRate > 0 ? passiveDecayRate : 2.5;
        this.hysteresisBufferPercent = hysteresisBufferPercent >= 0 ? hysteresisBufferPercent : 2.0;
        this.stateChangeCooldownSeconds = stateChangeCooldownSeconds >= 0 ? stateChangeCooldownSeconds : 1.0;
        this.minCappersRequired = Math.max(1, minCappersRequired);
        this.neutralAnchorPercent = neutralAnchorPercent >= 0.0 && neutralAnchorPercent <= 100.0 ? neutralAnchorPercent : 50.0;
        this.contestedAdvantageScaling = contestedAdvantageScaling > 0 ? contestedAdvantageScaling : 0.5;
        this.neutralDriftRate = neutralDriftRate > 0 ? neutralDriftRate : 2.5;
        this.targetTickets = Math.max(1, targetTickets);
        this.ticketsPerSecond = ticketsPerSecond > 0 ? ticketsPerSecond : 10.0;
        this.tugOfWarTeamAssignment = tugOfWarTeamAssignment != null ? tugOfWarTeamAssignment : TugOfWarTeamAssignment.FIRST_TWO_FACTIONS;
        this.deadzoneBufferPercent = deadzoneBufferPercent >= 0 ? deadzoneBufferPercent : 2.5;
    }

    public ArenaMechanicsConfig(
            boolean enabled,
            boolean autoStart,
            CaptureModeType mode,
            double percentPerSecond,
            double scalingPerMember,
            int maxCappersCounted,
            boolean freezeWhenContested,
            double loseControlThreshold,
            long lockoutSeconds,
            long knockDelaySeconds,
            boolean passiveDecayEnabled,
            double passiveDecayRate,
            double hysteresisBufferPercent,
            int stateChangeCooldownTicks
    ) {
        this(enabled, autoStart, mode, percentPerSecond, percentPerSecond, scalingPerMember, maxCappersCounted,
                freezeWhenContested, loseControlThreshold, lockoutSeconds, knockDelaySeconds,
                passiveDecayEnabled, passiveDecayRate, hysteresisBufferPercent, stateChangeCooldownTicks / 20.0);
    }

    public static ArenaMechanicsConfig createDefault() {
        return new ArenaMechanicsConfig(
                true,
                false,
                CaptureModeType.STANDARD_HILL,
                2.5,
                2.5,
                0.5,
                4,
                true,
                100.0,
                10,
                5,
                true,
                2.5,
                2.0,
                1.0
        );
    }

    public boolean isEnabled() { return enabled; }
    public boolean isAutoStart() { return autoStart; }
    public CaptureModeType getMode() { return mode; }
    public double getPercentPerSecond() { return percentPerSecond; }
    public double getUncapturePercentPerSecond() { return uncapturePercentPerSecond; }
    @Deprecated
    public double getPercentPerTick() { return percentPerSecond; }
    public double getScalingPerMember() { return scalingPerMember; }
    public int getMaxCappersCounted() { return maxCappersCounted; }
    public boolean isFreezeWhenContested() { return freezeWhenContested; }
    public double getLoseControlThreshold() { return loseControlThreshold; }
    public long getLockoutSeconds() { return lockoutSeconds; }
    public long getKnockDelaySeconds() { return knockDelaySeconds; }
    public boolean isPassiveDecayEnabled() { return passiveDecayEnabled; }
    public double getPassiveDecayRate() { return passiveDecayRate; }
    public double getPassiveDecayRatePerSecond() { return passiveDecayRate; }
    public double getHysteresisBufferPercent() { return hysteresisBufferPercent; }
    public double getStateChangeCooldownSeconds() { return stateChangeCooldownSeconds; }
    public int getStateChangeCooldownTicks() { return (int) Math.round(stateChangeCooldownSeconds * 20.0); }

    public int getMinCappersRequired() { return minCappersRequired; }
    public double getNeutralAnchorPercent() { return neutralAnchorPercent; }
    public double getContestedAdvantageScaling() { return contestedAdvantageScaling; }
    public double getNeutralDriftRate() { return neutralDriftRate; }
    public int getTargetTickets() { return targetTickets; }
    public double getTicketsPerSecond() { return ticketsPerSecond; }
    public TugOfWarTeamAssignment getTugOfWarTeamAssignment() { return tugOfWarTeamAssignment; }
    public double getDeadzoneBufferPercent() { return deadzoneBufferPercent; }
}
