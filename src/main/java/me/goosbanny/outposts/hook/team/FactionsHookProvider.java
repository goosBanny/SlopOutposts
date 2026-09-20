package me.goosbanny.outposts.hook.team;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.perms.Relation;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Direct compile-time adapter for FactionsUUID / SaberFactions / SavageFactions.
 * Uses real Factions API calls without reflection.
 */
public class FactionsHookProvider implements TeamRosterProvider {

    @Override
    public @NotNull String getProviderName() {
        return "Factions";
    }

    private Faction getFaction(Player player) {
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        return fPlayer != null ? fPlayer.getFaction() : null;
    }

    @Override
    public boolean hasTeam(@NotNull Player player) {
        Faction faction = getFaction(player);
        return faction != null && !faction.isWilderness();
    }

    @Override
    public @Nullable String getTeamId(@NotNull Player player) {
        Faction faction = getFaction(player);
        return faction != null ? faction.getId() : null;
    }

    @Override
    public @Nullable String getTeamName(@NotNull Player player) {
        Faction faction = getFaction(player);
        return faction != null ? faction.getTag() : null;
    }

    @Override
    public @Nullable UUID getTeamLeader(@NotNull Player player) {
        Faction faction = getFaction(player);
        if (faction == null) return null;
        FPlayer admin = faction.getFPlayerAdmin();
        if (admin == null) return null;
        if (admin.getPlayer() != null) return admin.getPlayer().getUniqueId();
        if (admin.getOfflinePlayer() != null) return admin.getOfflinePlayer().getUniqueId();
        try {
            return UUID.fromString(admin.getId());
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public @NotNull List<Player> getOnlineMembers(@NotNull Player player) {
        Faction faction = getFaction(player);
        if (faction == null) return Collections.singletonList(player);
        List<Player> online = faction.getOnlinePlayers();
        return online.isEmpty() ? Collections.singletonList(player) : online;
    }

    @Override
    public boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB) {
        if (teamIdA.equalsIgnoreCase(teamIdB)) return true;

        Faction fA = Factions.getInstance().getFactionById(teamIdA);
        Faction fB = Factions.getInstance().getFactionById(teamIdB);

        if (fA != null && fB != null) {
            return fA.getRelationTo(fB) == Relation.ALLY;
        }
        return false;
    }
}
