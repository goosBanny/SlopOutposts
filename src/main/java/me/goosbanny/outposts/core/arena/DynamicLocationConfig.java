package me.goosbanny.outposts.core.arena;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Configuration and region pool container for dynamic shifting outpost arenas.
 */
public class DynamicLocationConfig {

    public enum SwitchMode {
        INTERVAL,
        SCHEDULED_TIMESTAMPS,
        RANDOM_INTERVAL,
        ON_CAPTURE
    }

    public enum StateTransition {
        KEEP_PROGRESS,
        RESET_PROGRESS,
        RESET_IF_UNCONTROLLED
    }

    public enum PlayerTransition {
        NONE,
        TELEPORT_TO_NEW
    }

    private boolean enabled;
    private SwitchMode switchMode;
    private long intervalSeconds;
    private long minIntervalSeconds;
    private long maxIntervalSeconds;
    private List<Long> scheduledTimestamps;
    private boolean switchOnCapture;
    private List<Integer> warningSeconds;
    private int activationGraceSeconds;
    private int warpInvincibilitySeconds;
    private StateTransition stateTransition;
    private PlayerTransition playerTransition;
    private boolean noImmediateRepeat;
    private boolean effectsEnabled;
    private String soundEffect;
    private boolean beaconBeam;
    private final List<ArenaRegion> regions = new ArrayList<>();

    public DynamicLocationConfig(
            boolean enabled,
            @NotNull SwitchMode switchMode,
            long intervalSeconds,
            long minIntervalSeconds,
            long maxIntervalSeconds,
            @Nullable List<Long> scheduledTimestamps,
            boolean switchOnCapture,
            @Nullable List<Integer> warningSeconds,
            int activationGraceSeconds,
            int warpInvincibilitySeconds,
            @NotNull StateTransition stateTransition,
            @NotNull PlayerTransition playerTransition,
            boolean noImmediateRepeat,
            boolean effectsEnabled,
            @Nullable String soundEffect,
            boolean beaconBeam,
            @Nullable List<ArenaRegion> regions
    ) {
        this.enabled = enabled;
        this.switchMode = switchMode;
        this.intervalSeconds = Math.max(10, intervalSeconds);
        this.minIntervalSeconds = Math.max(10, minIntervalSeconds);
        this.maxIntervalSeconds = Math.max(minIntervalSeconds, maxIntervalSeconds);
        this.scheduledTimestamps = scheduledTimestamps != null ? new ArrayList<>(scheduledTimestamps) : new ArrayList<>();
        this.switchOnCapture = switchOnCapture;
        this.warningSeconds = warningSeconds != null ? new ArrayList<>(warningSeconds) : List.of(60, 30, 10, 5, 3, 2, 1);
        this.activationGraceSeconds = Math.max(0, activationGraceSeconds);
        this.warpInvincibilitySeconds = Math.max(0, warpInvincibilitySeconds);
        this.stateTransition = stateTransition;
        this.playerTransition = playerTransition;
        this.noImmediateRepeat = noImmediateRepeat;
        this.effectsEnabled = effectsEnabled;
        this.soundEffect = soundEffect != null ? soundEffect : "ENTITY_ENDERMAN_TELEPORT";
        this.beaconBeam = beaconBeam;
        if (regions != null) {
            this.regions.addAll(regions);
        }
    }

    public static DynamicLocationConfig createDisabled() {
        return new DynamicLocationConfig(
                false, SwitchMode.INTERVAL, 300, 180, 420,
                Collections.emptyList(), false, List.of(60, 30, 10, 5, 3, 2, 1),
                5, 5, StateTransition.KEEP_PROGRESS, PlayerTransition.NONE,
                true, true, "ENTITY_ENDERMAN_TELEPORT", true, Collections.emptyList()
        );
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @NotNull
    public SwitchMode getSwitchMode() { return switchMode; }
    public void setSwitchMode(@NotNull SwitchMode switchMode) { this.switchMode = switchMode; }

    public long getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(long intervalSeconds) { this.intervalSeconds = Math.max(10, intervalSeconds); }

    public long getMinIntervalSeconds() { return minIntervalSeconds; }
    public long getMaxIntervalSeconds() { return maxIntervalSeconds; }

    @NotNull
    public List<Long> getScheduledTimestamps() { return Collections.unmodifiableList(scheduledTimestamps); }

    public boolean isSwitchOnCapture() { return switchOnCapture; }
    public void setSwitchOnCapture(boolean switchOnCapture) { this.switchOnCapture = switchOnCapture; }

    @NotNull
    public List<Integer> getWarningSeconds() { return Collections.unmodifiableList(warningSeconds); }

    public int getActivationGraceSeconds() { return activationGraceSeconds; }
    public void setActivationGraceSeconds(int activationGraceSeconds) { this.activationGraceSeconds = Math.max(0, activationGraceSeconds); }

    public int getWarpInvincibilitySeconds() { return warpInvincibilitySeconds; }
    public void setWarpInvincibilitySeconds(int warpInvincibilitySeconds) { this.warpInvincibilitySeconds = Math.max(0, warpInvincibilitySeconds); }

    @NotNull
    public StateTransition getStateTransition() { return stateTransition; }
    public void setStateTransition(@NotNull StateTransition stateTransition) { this.stateTransition = stateTransition; }

    @NotNull
    public PlayerTransition getPlayerTransition() { return playerTransition; }
    public void setPlayerTransition(@NotNull PlayerTransition playerTransition) { this.playerTransition = playerTransition; }

    public boolean isNoImmediateRepeat() { return noImmediateRepeat; }
    public void setNoImmediateRepeat(boolean noImmediateRepeat) { this.noImmediateRepeat = noImmediateRepeat; }

    public boolean isEffectsEnabled() { return effectsEnabled; }
    public String getSoundEffect() { return soundEffect; }
    public boolean isBeaconBeam() { return beaconBeam; }

    @NotNull
    public List<ArenaRegion> getRegions() { return Collections.unmodifiableList(regions); }

    public synchronized void addRegion(@NotNull ArenaRegion region) {
        regions.removeIf(r -> r.getId().equalsIgnoreCase(region.getId()));
        regions.add(region);
    }

    public synchronized boolean removeRegion(@NotNull String regionId) {
        return regions.removeIf(r -> r.getId().equalsIgnoreCase(regionId));
    }

    @Nullable
    public ArenaRegion getRegion(@NotNull String regionId) {
        for (ArenaRegion r : regions) {
            if (r.getId().equalsIgnoreCase(regionId)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Selects a random region using weighted distribution.
     * If noImmediateRepeat is true and more than 1 region exists, the excludeRegion will not be selected.
     */
    @Nullable
    public ArenaRegion selectNextRegion(@Nullable ArenaRegion excludeRegion, @NotNull Random random) {
        if (regions.isEmpty()) {
            return null;
        }
        if (regions.size() == 1) {
            return regions.get(0);
        }

        List<ArenaRegion> candidates = new ArrayList<>(regions);
        if (noImmediateRepeat && excludeRegion != null && candidates.size() > 1) {
            candidates.removeIf(r -> r.getId().equalsIgnoreCase(excludeRegion.getId()));
        }

        List<ArenaRegion> positiveCandidates = new ArrayList<>();
        int totalWeight = 0;
        for (ArenaRegion r : candidates) {
            if (r.getWeight() > 0) {
                positiveCandidates.add(r);
                totalWeight += r.getWeight();
            }
        }

        if (totalWeight <= 0 || positiveCandidates.isEmpty()) {
            return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
        }

        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (ArenaRegion r : positiveCandidates) {
            current += r.getWeight();
            if (roll < current) {
                return r;
            }
        }
        return positiveCandidates.get(0);
    }
}
