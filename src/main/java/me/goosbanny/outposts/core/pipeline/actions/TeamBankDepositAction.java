package me.goosbanny.outposts.core.pipeline.actions;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import me.goosbanny.outposts.api.team.TeamEconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Action that deposits money into the controlling team's bank (or directly to the player's balance in SOLO mode).
 */
public class TeamBankDepositAction implements ArenaAction {

    private final double amount;
    private final TeamEconomyProvider economyProvider;

    public TeamBankDepositAction(double amount, @NotNull TeamEconomyProvider economyProvider) {
        this.amount = amount;
        this.economyProvider = economyProvider;
    }

    @Override
    public @NotNull String getType() {
        return "TEAM_BANK_DEPOSIT";
    }

    @Override
    public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        String controllerId = (String) context.getOrDefault("team_id", arena.getControllerTeamId());
        if (controllerId == null || amount <= 0.0) {
            return;
        }

        // In SOLO mode: controllerId is the player's UUID string
        if (arena.getOccupancyMode() == OccupancyMode.SOLO) {
            try {
                UUID playerUuid = UUID.fromString(controllerId);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null && rsp.getProvider() != null) {
                    rsp.getProvider().depositPlayer(offlinePlayer, amount);
                    return;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        // In TEAM mode: deposit into team bank (ZelTeams bank or Vault faction bank)
        if (economyProvider.supportsBank()) {
            economyProvider.deposit(controllerId, amount);
        }
    }
}
