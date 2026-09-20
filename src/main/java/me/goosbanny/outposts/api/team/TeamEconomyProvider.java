package me.goosbanny.outposts.api.team;

import org.jetbrains.annotations.NotNull;

/**
 * Single-responsibility contract for team banks and currency transactions.
 */
public interface TeamEconomyProvider {

    /**
     * Checks if team-level shared banks are supported by this provider.
     *
     * @return true if team banks are operational
     */
    boolean supportsBank();

    /**
     * Queries the balance of a team's bank.
     *
     * @param teamId unique identifier of the team
     * @return current balance, or 0.0 if not found
     */
    double getBalance(@NotNull String teamId);

    /**
     * Deposits an amount into the team's bank.
     *
     * @param teamId unique identifier of the team
     * @param amount amount to deposit
     */
    void deposit(@NotNull String teamId, double amount);

    /**
     * Withdraws an amount from the team's bank.
     *
     * @param teamId unique identifier of the team
     * @param amount amount to withdraw
     */
    void withdraw(@NotNull String teamId, double amount);
}
