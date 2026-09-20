package me.goosbanny.outposts.hook.team;

import me.goosbanny.outposts.api.team.TeamEconomyProvider;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Manages auto-detection and registration of team providers and economy integrations.
 */
public class TeamHookManager {

    private final Logger logger;
    private TeamRosterProvider activeRosterProvider;
    private TeamEconomyProvider activeEconomyProvider;

    public TeamHookManager(@NotNull Logger logger) {
        this.logger = logger;
        this.activeEconomyProvider = new VaultEconomyProvider();
        this.activeRosterProvider = new ScoreboardTeamProvider();
    }

    /**
     * Initializes the best available team provider based on configuration preference and installed plugins.
     *
     * @param preferredProvider configured provider name (e.g. "AUTO", "FACTIONS", "TOWNY", "SCOREBOARD")
     */
    public void detectAndInitialize(@NotNull String preferredProvider) {
        String pref = preferredProvider.toUpperCase();

        if ("SCOREBOARD".equals(pref)) {
            this.activeRosterProvider = new ScoreboardTeamProvider();
            logger.info("[Hooks] Explicitly using vanilla Scoreboard teams provider.");
            return;
        }

        // 1. ZelTeams
        Plugin zelTeams = Bukkit.getPluginManager().getPlugin("ZelTeams");
        if (zelTeams == null) {
            zelTeams = Bukkit.getPluginManager().getPlugin("zelteams");
        }
        if (zelTeams != null && ("AUTO".equals(pref) || pref.contains("ZEL"))) {
            try {
                com.zeltuv.teams.api.ITeamPlugin api = com.zeltuv.teams.api.ZelTeamsAPI.getInstance();
                if (api != null && api.getTeamManager() != null) {
                    this.activeRosterProvider = new ZelTeamsHookProvider(api.getTeamManager());
                    this.activeEconomyProvider = new ZelTeamsEconomyProvider(api.getTeamManager());
                    logger.info("[Hooks] Hooked into ZelTeams (" + zelTeams.getDescription().getVersion() + ") team and bank provider.");
                    return;
                }
            } catch (Throwable t) {
                logger.warning("[Hooks] Found ZelTeams plugin but failed to initialize adapter: " + t.getMessage());
            }
        }

        // Auto-detect or specific hook
        Plugin factions = Bukkit.getPluginManager().getPlugin("Factions");
        if (factions != null && ("AUTO".equals(pref) || pref.contains("FACTION"))) {
            try {
                // Attempt reflection or standard adapter
                this.activeRosterProvider = new FactionsHookProvider();
                logger.info("[Hooks] Hooked into Factions (" + factions.getDescription().getVersion() + ") team provider.");
                return;
            } catch (Throwable t) {
                logger.warning("[Hooks] Found Factions plugin but failed to initialize adapter: " + t.getMessage());
            }
        }

        Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
        if (towny != null && ("AUTO".equals(pref) || pref.contains("TOWNY"))) {
            try {
                this.activeRosterProvider = new TownyHookProvider();
                logger.info("[Hooks] Hooked into Towny (" + towny.getDescription().getVersion() + ") team provider.");
                return;
            } catch (Throwable t) {
                logger.warning("[Hooks] Found Towny plugin but failed to initialize adapter: " + t.getMessage());
            }
        }

        Plugin betterTeams = Bukkit.getPluginManager().getPlugin("BetterTeams");
        if (betterTeams != null && ("AUTO".equals(pref) || pref.contains("BETTERTEAMS") || pref.contains("BETTER_TEAMS"))) {
            try {
                this.activeRosterProvider = new BetterTeamsHookProvider();
                this.activeEconomyProvider = new BetterTeamsEconomyProvider();
                logger.info("[Hooks] Hooked into BetterTeams (" + betterTeams.getDescription().getVersion() + ") team and bank provider.");
                return;
            } catch (Throwable t) {
                logger.warning("[Hooks] Found BetterTeams plugin but failed to initialize adapter: " + t.getMessage());
            }
        }

        // Fallback to scoreboard teams
        this.activeRosterProvider = new ScoreboardTeamProvider();
        logger.info("[Hooks] No supported faction plugins detected or configured. Active team provider: " + activeRosterProvider.getProviderName());
    }

    @NotNull
    public TeamRosterProvider getRosterProvider() {
        return activeRosterProvider;
    }

    public void setRosterProvider(@NotNull TeamRosterProvider provider) {
        this.activeRosterProvider = provider;
    }

    @NotNull
    public TeamEconomyProvider getEconomyProvider() {
        return activeEconomyProvider;
    }

    public void setEconomyProvider(@NotNull TeamEconomyProvider provider) {
        this.activeEconomyProvider = provider;
    }
}
