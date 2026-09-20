package me.goosbanny.outposts.hook.team;

import com.zeltuv.teams.api.cache.IMember;
import com.zeltuv.teams.api.cache.IOwner;
import com.zeltuv.teams.api.cache.ITeam;
import com.zeltuv.teams.api.manager.ITeamManager;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Direct compile-time adapter for ZelTeams (by Zeltuv).
 * Integrates team rosters, roles, leaders, and alliance graphs.
 */
public class ZelTeamsHookProvider implements TeamRosterProvider {

    private final ITeamManager teamManager;

    public ZelTeamsHookProvider(@NotNull ITeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public @NotNull String getProviderName() {
        return "ZelTeams";
    }

    @Override
    public boolean hasTeam(@NotNull Player player) {
        Optional<ITeam> teamOpt = teamManager.getTeam(player);
        return teamOpt.isPresent() && !teamOpt.get().isClosed();
    }

    @Override
    public @Nullable String getTeamId(@NotNull Player player) {
        Optional<ITeam> teamOpt = teamManager.getTeam(player);
        if (teamOpt.isEmpty() || teamOpt.get().isClosed()) {
            return null;
        }
        return teamOpt.get().getTeamUUID().toString();
    }

    @Override
    public @Nullable String getTeamName(@NotNull Player player) {
        Optional<ITeam> teamOpt = teamManager.getTeam(player);
        if (teamOpt.isEmpty() || teamOpt.get().isClosed()) {
            return null;
        }
        ITeam team = teamOpt.get();
        String displayName = team.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        String tag = team.getTag();
        return tag != null ? tag : team.getTeamUUID().toString();
    }

    @Override
    public @Nullable UUID getTeamLeader(@NotNull Player player) {
        Optional<ITeam> teamOpt = teamManager.getTeam(player);
        if (teamOpt.isEmpty() || teamOpt.get().isClosed()) {
            return null;
        }
        IOwner owner = teamOpt.get().getOwner();
        return owner != null ? owner.getUuid() : null;
    }

    @Override
    public @NotNull List<Player> getOnlineMembers(@NotNull Player player) {
        Optional<ITeam> teamOpt = teamManager.getTeam(player);
        if (teamOpt.isEmpty() || teamOpt.get().isClosed()) {
            return Collections.singletonList(player);
        }

        ITeam team = teamOpt.get();
        List<IMember> members = team.getAllMembers();
        if (members == null || members.isEmpty()) {
            return Collections.singletonList(player);
        }

        List<Player> online = new ArrayList<>();
        for (IMember member : members) {
            if (member == null) continue;
            Player p = Bukkit.getPlayer(member.getUuid());
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }

        return online.isEmpty() ? Collections.singletonList(player) : online;
    }

    @Override
    public boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB) {
        if (teamIdA.equalsIgnoreCase(teamIdB)) return true;

        ITeam teamA = findTeam(teamIdA);
        ITeam teamB = findTeam(teamIdB);

        if (teamA == null || teamB == null || teamA.isClosed() || teamB.isClosed()) {
            return false;
        }

        if (teamA.isAlliedWith(teamB)) {
            return true;
        }

        List<UUID> alliesA = teamA.getAllyList();
        return alliesA != null && alliesA.contains(teamB.getTeamUUID());
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
