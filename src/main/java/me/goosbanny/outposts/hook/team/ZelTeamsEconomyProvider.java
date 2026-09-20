package me.goosbanny.outposts.hook.team;

import com.zeltuv.teams.api.cache.ITeam;
import com.zeltuv.teams.api.manager.ITeamManager;
import me.goosbanny.outposts.api.team.TeamEconomyProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter for ZelTeams shared team bank balances and deposits.
 */
public class ZelTeamsEconomyProvider implements TeamEconomyProvider {

    private final ITeamManager teamManager;

    public ZelTeamsEconomyProvider(@NotNull ITeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean supportsBank() {
        return true;
    }

    @Override
    public double getBalance(@NotNull String teamId) {
        ITeam team = findTeam(teamId);
        if (team == null || team.isClosed()) {
            return 0.0;
        }
        return team.getBankBalance();
    }

    @Override
    public void deposit(@NotNull String teamId, double amount) {
        if (amount <= 0.0) return;
        ITeam team = findTeam(teamId);
        if (team == null || team.isClosed()) return;
        team.setBankBalance(team.getBankBalance() + amount);
    }

    @Override
    public void withdraw(@NotNull String teamId, double amount) {
        if (amount <= 0.0) return;
        ITeam team = findTeam(teamId);
        if (team == null || team.isClosed()) return;
        team.setBankBalance(Math.max(0.0, team.getBankBalance() - amount));
    }

    @Nullable
    private ITeam findTeam(@NotNull String id) {
        try {
            UUID uuid = UUID.fromString(id);
            ITeam team = teamManager.getCachedTeams().get(uuid);
            if (team != null) return team;
        } catch (IllegalArgumentException ignored) {
        }

        Optional<ITeam> byTag = teamManager.getByTag(id);
        return byTag.orElse(null);
    }
}
