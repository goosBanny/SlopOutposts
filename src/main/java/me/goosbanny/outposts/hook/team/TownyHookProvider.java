package me.goosbanny.outposts.hook.team;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Direct compile-time adapter for Towny.
 * Uses real TownyAPI calls without reflection.
 */
public class TownyHookProvider implements TeamRosterProvider {

    private final TownyAPI towny = TownyAPI.getInstance();

    @Override
    public @NotNull String getProviderName() {
        return "Towny";
    }

    private Town getTown(Player player) {
        return towny.getTown(player);
    }

    @Override
    public boolean hasTeam(@NotNull Player player) {
        return getTown(player) != null;
    }

    private boolean shouldUseNations() {
        Outposts plugin = Outposts.getInstance();
        return plugin != null && plugin.getConfigManager().isTownyUseNations();
    }

    @Override
    public @Nullable String getTeamId(@NotNull Player player) {
        Town town = getTown(player);
        if (town == null) return null;
        if (shouldUseNations() && town.hasNation()) {
            com.palmergames.bukkit.towny.object.Nation nation = town.getNationOrNull();
            if (nation != null) {
                return nation.getUUID().toString();
            }
        }
        return town.getUUID().toString();
    }

    @Override
    public @Nullable String getTeamName(@NotNull Player player) {
        Town town = getTown(player);
        if (town == null) return null;
        if (shouldUseNations() && town.hasNation()) {
            com.palmergames.bukkit.towny.object.Nation nation = town.getNationOrNull();
            if (nation != null) {
                return nation.getName();
            }
        }
        return town.getName();
    }

    @Override
    public @Nullable UUID getTeamLeader(@NotNull Player player) {
        Town town = getTown(player);
        if (town == null) return null;
        Resident mayor = town.getMayor();
        return mayor != null ? mayor.getUUID() : null;
    }

    @Override
    public @NotNull List<Player> getOnlineMembers(@NotNull Player player) {
        Town town = getTown(player);
        if (town == null) return Collections.singletonList(player);

        List<Player> online = new ArrayList<>();
        for (Resident res : town.getResidents()) {
            Player p = res.getPlayer();
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        return online.isEmpty() ? Collections.singletonList(player) : online;
    }

    @Override
    public boolean areAllies(@NotNull String teamIdA, @NotNull String teamIdB) {
        if (teamIdA.equalsIgnoreCase(teamIdB)) return true;

        com.palmergames.bukkit.towny.object.Nation nationA = null;
        com.palmergames.bukkit.towny.object.Nation nationB = null;
        try {
            nationA = towny.getNation(UUID.fromString(teamIdA));
        } catch (IllegalArgumentException ignored) {
            nationA = towny.getNation(teamIdA);
        }
        try {
            nationB = towny.getNation(UUID.fromString(teamIdB));
        } catch (IllegalArgumentException ignored) {
            nationB = towny.getNation(teamIdB);
        }

        if (nationA != null && nationB != null) {
            if (nationA.equals(nationB)) return true;
            return nationA.hasAlly(nationB);
        }

        Town townA = null;
        Town townB = null;

        try {
            townA = towny.getTown(UUID.fromString(teamIdA));
        } catch (IllegalArgumentException ignored) {
            townA = towny.getTown(teamIdA);
        }

        try {
            townB = towny.getTown(UUID.fromString(teamIdB));
        } catch (IllegalArgumentException ignored) {
            townB = towny.getTown(teamIdB);
        }

        if (townA != null && townB != null) {
            if (townA.hasNation() && townB.hasNation()) {
                com.palmergames.bukkit.towny.object.Nation nA = townA.getNationOrNull();
                com.palmergames.bukkit.towny.object.Nation nB = townB.getNationOrNull();
                if (nA != null && nB != null) {
                    if (nA.equals(nB)) {
                        return true;
                    }
                    return nA.hasAlly(nB);
                }
            }
        }
        return false;
    }
}
