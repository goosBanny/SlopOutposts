package me.goosbanny.outposts.hook.team;

import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Built-in zero-dependency team roster provider leveraging vanilla Minecraft Scoreboard Teams.
 * Serves as the robust default when external faction plugins are absent.
 */
public class ScoreboardTeamProvider implements TeamRosterProvider {

    @Override
    public @NotNull String getProviderName() {
        return "Scoreboard";
    }

    private Team getPlayerTeam(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        return scoreboard.getEntryTeam(player.getName());
    }

    @Override
    public boolean hasTeam(@NotNull Player player) {
        return getPlayerTeam(player) != null;
    }

    @Override
    public @Nullable String getTeamId(@NotNull Player player) {
        Team team = getPlayerTeam(player);
        return team != null ? team.getName() : null;
    }

    @Override
    public @Nullable String getTeamName(@NotNull Player player) {
        Team team = getPlayerTeam(player);
        return team != null ? team.getDisplayName() : null;
    }

    @Override
    public @Nullable UUID getTeamLeader(@NotNull Player player) {
        return null;
    }

    @Override
    public @NotNull List<Player> getOnlineMembers(@NotNull Player player) {
        Team team = getPlayerTeam(player);
        if (team == null) {
            return Collections.singletonList(player);
        }

        List<Player> members = new ArrayList<>();
        for (String entry : team.getEntries()) {
            Player p = Bukkit.getPlayerExact(entry);
            if (p != null && p.isOnline()) {
                members.add(p);
            }
        }
        return members;
    }

    @Override
    public boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB) {
        return teamIdA.equalsIgnoreCase(teamIdB);
    }
}
