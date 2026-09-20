package me.goosbanny.outposts.hook;

import com.zeltuv.teams.api.cache.IMember;
import com.zeltuv.teams.api.cache.IOwner;
import com.zeltuv.teams.api.cache.ITeam;
import com.zeltuv.teams.api.manager.ITeamManager;
import me.goosbanny.outposts.hook.team.ZelTeamsEconomyProvider;
import me.goosbanny.outposts.hook.team.ZelTeamsHookProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ZelTeamsHookTest {

    @Test
    public void testZelTeamsEconomyAndAlliances() {
        UUID teamAUuid = UUID.randomUUID();
        UUID teamBUuid = UUID.randomUUID();

        double[] teamABank = new double[]{1500.0};
        boolean[] isClosed = new boolean[]{false};

        ITeam teamA = (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getTeamUUID")) return teamAUuid;
                    if (name.equals("getTag")) return "ALPHA";
                    if (name.equals("getDisplayName")) return "Team Alpha";
                    if (name.equals("isClosed")) return isClosed[0];
                    if (name.equals("getBankBalance")) return teamABank[0];
                    if (name.equals("setBankBalance")) {
                        teamABank[0] = (double) args[0];
                        return null;
                    }
                    if (name.equals("getAllyList")) return List.of(teamBUuid);
                    if (name.equals("isAlliedWith")) {
                        ITeam other = (ITeam) args[0];
                        return other.getTeamUUID().equals(teamBUuid);
                    }
                    return null;
                }
        );

        ITeam teamB = (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getTeamUUID")) return teamBUuid;
                    if (name.equals("getTag")) return "BETA";
                    if (name.equals("getDisplayName")) return "Team Beta";
                    if (name.equals("isClosed")) return false;
                    if (name.equals("getAllyList")) return List.of(teamAUuid);
                    if (name.equals("isAlliedWith")) {
                        ITeam other = (ITeam) args[0];
                        return other.getTeamUUID().equals(teamAUuid);
                    }
                    return null;
                }
        );

        Map<UUID, ITeam> cachedTeams = new HashMap<>();
        cachedTeams.put(teamAUuid, teamA);
        cachedTeams.put(teamBUuid, teamB);

        ITeamManager teamManager = (ITeamManager) Proxy.newProxyInstance(
                ITeamManager.class.getClassLoader(),
                new Class<?>[]{ITeamManager.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getCachedTeams")) return cachedTeams;
                    if (name.equals("getByTag")) {
                        String tag = (String) args[0];
                        if ("ALPHA".equalsIgnoreCase(tag)) return Optional.of(teamA);
                        if ("BETA".equalsIgnoreCase(tag)) return Optional.of(teamB);
                        return Optional.empty();
                    }
                    return null;
                }
        );

        ZelTeamsHookProvider rosterProvider = new ZelTeamsHookProvider(teamManager);
        assertEquals("ZelTeams", rosterProvider.getProviderName());

        // Test alliance query
        assertTrue(rosterProvider.areAllies(teamAUuid.toString(), teamBUuid.toString()));
        assertTrue(rosterProvider.areAllies(teamAUuid.toString(), teamAUuid.toString()));
        assertFalse(rosterProvider.areAllies(teamAUuid.toString(), UUID.randomUUID().toString()));

        // Test economy provider
        ZelTeamsEconomyProvider economyProvider = new ZelTeamsEconomyProvider(teamManager);
        assertTrue(economyProvider.supportsBank());
        assertEquals(1500.0, economyProvider.getBalance(teamAUuid.toString()));

        economyProvider.deposit(teamAUuid.toString(), 500.0);
        assertEquals(2000.0, economyProvider.getBalance(teamAUuid.toString()));

        economyProvider.withdraw(teamAUuid.toString(), 250.0);
        assertEquals(1750.0, economyProvider.getBalance(teamAUuid.toString()));

        // Closed team tests
        isClosed[0] = true;
        assertEquals(0.0, economyProvider.getBalance(teamAUuid.toString()));
        assertFalse(rosterProvider.areAllies(teamAUuid.toString(), teamBUuid.toString()));
    }
}
