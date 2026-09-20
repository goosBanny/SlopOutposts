package me.goosbanny.outposts.core.pipeline.actions;

import me.goosbanny.outposts.api.team.TeamEconomyProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Legacy alias for TeamBankDepositAction.
 * Kept for backwards compatibility with configurations using "FACTION_BANK_DEPOSIT".
 */
public class FactionBankDepositAction extends TeamBankDepositAction {

    public FactionBankDepositAction(double amount, @NotNull TeamEconomyProvider economyProvider) {
        super(amount, economyProvider);
    }

    @Override
    public @NotNull String getType() {
        return "FACTION_BANK_DEPOSIT";
    }
}
