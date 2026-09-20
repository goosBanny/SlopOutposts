package me.goosbanny.outposts.api.arena;

import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DynamicLocationConfig;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Internal mutable ticking engine contract for an Outpost arena.
 */
public interface OutpostArena extends ArenaDescriptor {

    /**
     * Creates an immutable thread-safe snapshot of the current arena state.
     *
     * @return ArenaView snapshot
     */
    ArenaView createSnapshot();

    /**
     * Pre-serialized MiniMessage string of the arena display name for zero-allocation HUD rendering.
     */
    default String getSerializedDisplayName() {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(getDisplayName());
    }

    /**
     * Executes one tick cycle of the game loop: spatial evaluation, capture mechanics,
     * hysteresis validation, and telemetry broadcasts.
     */
    void tickGameLoop();

    /**
     * Sets the current capture progress (clamped between 0.0 and 100.0).
     *
     * @param progress capture percentage
     */
    void setProgress(double progress);

    /**
     * Assigns control of this arena to a team and initiates any post-capture lockout.
     *
     * @param teamId   unique identifier of the winning team
     * @param teamName display name of the winning team
     * @param capturer UUID of the player who finalized the capture, or null if system/passive
     */
    void setController(String teamId, String teamName, @Nullable UUID capturer);

    /**
     * Resets the arena progress to 0.0 and clears any controlling or capping team.
     */
    void resetToNeutral();

    /**
     * Evaluates if raw block coordinates fall inside the arena's bounding box.
     *
     * @param x block X
     * @param y block Y
     * @param z block Z
     * @return true if within bounds
     */
    boolean isWithinBounds(int x, int y, int z);

    /**
     * Current lifecycle state of the arena.
     */
    ArenaState getState();

    /**
     * Current capture progress percentage (0.0 to 100.0).
     */
    double getProgress();

    /**
     * Unique identifier of the controlling team, or null if neutral.
     */
    @Nullable
    String getControllerTeamId();

    /**
     * Display name of the controlling team, or null if neutral.
     */
    @Nullable
    String getControllerTeamName();

    /**
     * Unique identifier of the active capping team, or null if none.
     */
    @Nullable
    String getCappingTeamId();

    /**
     * Display name of the active capping team, or null if none.
     */
    @Nullable
    String getCappingTeamName();

    /**
     * Updates the active capping team.
     */
    void setCappingTeam(@Nullable String teamId, @Nullable String teamName);

    /**
     * Number of valid cappers currently standing inside the zone.
     */
    int getCapperCount();

    /**
     * True if the zone is contested by opposing teams.
     */
    boolean isContested();

    /**
     * True if the arena is locked from capture (e.g. cooldown after being capped).
     */
    boolean isLocked();

    /**
     * Remaining lockout duration in seconds.
     */
    long getLockoutRemainingSeconds();

    /**
     * Center location of the arena bounding box.
     */
    @Nullable
    Location getCenterLocation();

    /**
     * Teleport/warp destination for this arena, if configured.
     */
    @Nullable
    Location getWarpLocation();

    /**
     * Updates the teleport/warp destination for this arena.
     */
    void setWarpLocation(@Nullable Location location);

    /**
     * Returns true if the arena is currently operational and accepting captures.
     */
    boolean isActive();

    /**
     * Toggles whether the arena is operational (e.g. via schedule or admin commands).
     */
    void setActive(boolean active);

    /**
     * Returns the dynamic location configuration and region pool.
     */
    @NotNull
    DynamicLocationConfig getDynamicLocationConfig();

    /**
     * Returns the currently active region candidate, or null if using base geometry.
     */
    @Nullable
    ArenaRegion getCurrentRegion();

    /**
     * Shifts the outpost to the specified region (or selects a weighted random region if target is null).
     */
    void shiftToRegion(@Nullable ArenaRegion target);

    /**
     * Returns the number of seconds until the next automatic relocation, or -1 if disabled.
     */
    long getNextShiftSeconds();

    /**
     * Returns the number of seconds remaining in the relocation warmup / activation grace period.
     */
    int getActivationGraceRemainingSeconds();

    /**
     * Returns true if the arena is currently in the post-relocation warmup grace period.
     */
    boolean isWarmingUp();

    /**
     * Returns map of per-outpost language and telemetry overrides.
     */
    @NotNull
    Map<String, String> getCustomLangOverrides();

    /**
     * Sets per-outpost language and telemetry overrides.
     */
    void setCustomLangOverrides(@NotNull Map<String, String> overrides);

    /**
     * Retrieves an optional language string override for this specific arena.
     */
    @Nullable
    String getCustomLang(@NotNull String path);

    /**
     * Toggles whether perimeter boundary particles are rendered for this arena.
     */
    boolean isBoundingParticlesEnabled();

    void setBoundingParticlesEnabled(boolean enabled);
}
