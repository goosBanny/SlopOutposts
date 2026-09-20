package me.goosbanny.outposts.core.arena;

import me.goosbanny.outposts.api.mechanics.CaptureModeType;

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
}
