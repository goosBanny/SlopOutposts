package me.goosbanny.outposts.api.arena;

import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import net.kyori.adventure.text.Component;

/**
 * Immutable arena configuration and geometric metadata.
 */
public interface ArenaDescriptor {

    /**
     * Unique alphanumeric identifier of the arena (matches the filename id).
     *
     * @return unique arena ID
     */
    String getId();

    /**
     * Display name formatted as an Adventure Component (supports MiniMessage & RGB).
     *
     * @return display name component
     */
    Component getDisplayName();

    /**
     * Name of the Bukkit world the arena resides in.
     *
     * @return world name
     */
    String getWorldName();

    /**
     * Minimum X coordinate of the cuboid boundary (inclusive).
     */
    int getMinX();

    /**
     * Minimum Y coordinate of the cuboid boundary (inclusive).
     */
    int getMinY();

    /**
     * Minimum Z coordinate of the cuboid boundary (inclusive).
     */
    int getMinZ();

    /**
     * Maximum X coordinate of the cuboid boundary (inclusive).
     */
    int getMaxX();

    /**
     * Maximum Y coordinate of the cuboid boundary (inclusive).
     */
    int getMaxY();

    /**
     * Maximum Z coordinate of the cuboid boundary (inclusive).
     */
    int getMaxZ();

    /**
     * The configured capture mode engine type.
     *
     * @return capture mode type
     */
    CaptureModeType getCaptureModeType();

    /**
     * Defines whether this arena is controlled by teams or individual solo players.
     *
     * @return occupancy mode (TEAM or SOLO)
     */
    OccupancyMode getOccupancyMode();

    /**
     * Multiplier configured for this arena (e.g., "spawner_rate", "mob_drop_rate", "damage_rate").
     *
     * @param key multiplier key name
     * @return double value, defaults to 1.0 if not configured
     */
    double getMultiplier(String key);
}
