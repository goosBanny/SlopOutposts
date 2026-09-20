package me.goosbanny.outposts.hook.team;

import me.goosbanny.outposts.api.team.TeamEconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Vault-backed implementation of TeamEconomyProvider.
 */
public class VaultEconomyProvider implements TeamEconomyProvider {

    private Economy economy;

    public VaultEconomyProvider() {
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.economy = rsp.getProvider();
        }
    }

    private Economy getEconomy() {
        if (economy == null) {
            setupEconomy();
        }
        return economy;
    }

    @Override
    public boolean supportsBank() {
        Economy eco = getEconomy();
        return eco != null && eco.hasBankSupport();
    }

    @Override
    public double getBalance(@NotNull String teamId) {
        Economy eco = getEconomy();
        if (eco == null) return 0.0;
        if (eco.hasBankSupport()) {
            return eco.bankBalance(teamId).balance;
        }
        return 0.0;
    }

    @Override
    public void deposit(@NotNull String teamId, double amount) {
        Economy eco = getEconomy();
        if (eco == null) return;
        if (eco.hasBankSupport()) {
            eco.bankDeposit(teamId, amount);
        }
    }

    @Override
    public void withdraw(@NotNull String teamId, double amount) {
        Economy eco = getEconomy();
        if (eco == null) return;
        if (eco.hasBankSupport()) {
            eco.bankWithdraw(teamId, amount);
        }
    }
}
