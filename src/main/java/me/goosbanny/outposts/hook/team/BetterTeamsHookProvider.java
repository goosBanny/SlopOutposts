package me.goosbanny.outposts.hook.team;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Direct compile-time adapter for BetterTeams (by booksaw).
 * Uses real BetterTeams API calls without reflection.
 */
public class BetterTeamsHookProvider implements TeamRosterProvider {

    @Override
    public @NotNull String getProviderName() {
        return "BetterTeams";
    }

    @Override
    public boolean hasTeam(@NotNull Player player) {
        return Team.getTeam(player) != null;
    }

    @Override
    public @Nullable String getTeamId(@NotNull Player player) {
        Team team = Team.getTeam(player);
        if (team == null) return null;
        return team.getID().toString();
    }

    @Override
    public @Nullable String getTeamName(@NotNull Player player) {
        Team team = Team.getTeam(player);
        if (team == null) return null;
        return team.getName();
    }

    @Override
    public @Nullable UUID getTeamLeader(@NotNull Player player) {
        Team team = Team.getTeam(player);
        if (team == null) return null;
        List<TeamPlayer> owners = team.getRank(com.booksaw.betterTeams.PlayerRank.OWNER);
        if (owners != null && !owners.isEmpty()) {
            return owners.get(0).getPlayer().getUniqueId();
        }
        return null;
    }

    @Override
    public @NotNull List<Player> getOnlineMembers(@NotNull Player player) {
        Team team = Team.getTeam(player);
        if (team == null) {
            return Collections.singletonList(player);
        }

        List<Player> online = new ArrayList<>();
        for (Player p : team.getOnlineMembers()) {
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        return online.isEmpty() ? Collections.singletonList(player) : online;
    }

    @Override
    public boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB) {
        if (teamIdA.equalsIgnoreCase(teamIdB)) return true;

        Team teamA = null;
        Team teamB = null;

        try {
            teamA = Team.getTeam(UUID.fromString(teamIdA));
        } catch (IllegalArgumentException ignored) {
            teamA = Team.getTeam(teamIdA);
        }

        try {
            teamB = Team.getTeam(UUID.fromString(teamIdB));
        } catch (IllegalArgumentException ignored) {
            teamB = Team.getTeam(teamIdB);
        }

        if (teamA != null && teamB != null) {
            return teamA.isAlly(teamB);
        }
        return false;
    }
}
