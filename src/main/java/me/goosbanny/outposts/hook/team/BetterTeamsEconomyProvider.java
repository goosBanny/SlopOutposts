package me.goosbanny.outposts.hook.team;

import com.booksaw.betterTeams.Team;
import me.goosbanny.outposts.api.team.TeamEconomyProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Direct compile-time economy/bank adapter for BetterTeams.
 * Uses real BetterTeams API (Team#getMoney, Team#setMoney) without reflection.
 */
public class BetterTeamsEconomyProvider implements TeamEconomyProvider {

    @Override
    public boolean supportsBank() {
        return true;
    }

    @Nullable
    private Team findTeam(@NotNull String teamId) {
        try {
            UUID uuid = UUID.fromString(teamId);
            Team team = Team.getTeam(uuid);
            if (team != null) return team;
        } catch (IllegalArgumentException ignored) {
        }
        return Team.getTeam(teamId);
    }

    @Override
    public double getBalance(@NotNull String teamId) {
        Team team = findTeam(teamId);
        return team != null ? team.getMoney() : 0.0;
    }

    @Override
    public void deposit(@NotNull String teamId, double amount) {
        if (amount <= 0.0) return;
        Team team = findTeam(teamId);
        if (team != null) {
            team.setMoney(team.getMoney() + amount);
        }
    }

    @Override
    public void withdraw(@NotNull String teamId, double amount) {
        if (amount <= 0.0) return;
        Team team = findTeam(teamId);
        if (team != null) {
            team.setMoney(Math.max(0.0, team.getMoney() - amount));
        }
    }
}
