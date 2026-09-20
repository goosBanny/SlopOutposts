package me.goosbanny.outposts.api.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Single-responsibility contract for team membership, rosters, and alliance relations.
 */
public interface TeamRosterProvider {

    /**
     * Unique display name of this provider (e.g. "Scoreboard", "FactionsUUID", "Towny").
     */
    @NotNull
    String getProviderName();

    /**
     * Checks if a player belongs to any team or faction.
     *
     * @param player player to test
     * @return true if player has a team
     */
    boolean hasTeam(@NotNull Player player);

    /**
     * Gets the persistent unique ID of the player's team (e.g., Faction ID, Town UUID, or team name).
     *
     * @param player player
     * @return team ID, or null if unaffiliated
     */
    @Nullable
    String getTeamId(@NotNull Player player);

    /**
     * Gets the human-readable display name of the player's team.
     *
     * @param player player
     * @return team display name, or null if unaffiliated
     */
    @Nullable
    String getTeamName(@NotNull Player player);

    /**
     * Gets the UUID of the team leader/owner.
     *
     * @param player player
     * @return leader UUID, or null if not available
     */
    @Nullable
    UUID getTeamLeader(@NotNull Player player);

    /**
     * Gets a list of all currently online players belonging to the player's team.
     *
     * @param player player
     * @return list of online teammates
     */
    @NotNull
    List<Player> getOnlineMembers(@NotNull Player player);

    /**
     * Gets a list of all currently online players belonging to the given team ID.
     * Default implementation iterates online players once.
     *
     * @param teamId persistent team identifier
     * @return list of online teammates
     */
    @NotNull
    default List<Player> getOnlineMembersById(@NotNull String teamId) {
        List<Player> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (teamId.equalsIgnoreCase(getTeamId(p))) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Checks if two teams/factions are considered allies.
     * Used by anti-cheese validation to prevent friendly allied stall.
     *
     * @param teamIdA first team ID
     * @param teamIdB second team ID
     * @return true if ally relation exists
     */
    boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB);
}
