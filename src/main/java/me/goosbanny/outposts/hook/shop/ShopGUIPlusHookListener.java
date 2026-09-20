package me.goosbanny.outposts.hook.shop;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.arena.ArenaMultipliers;
import me.goosbanny.outposts.core.manager.ArenaManager;
import net.brcdev.shopgui.event.ShopPreTransactionEvent;
import net.brcdev.shopgui.shop.ShopManager.ShopAction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Hook into ShopGUI+ applying the shopgui_sell_rate multiplier to sell transactions.
 */
public class ShopGUIPlusHookListener implements Listener {

    private final ArenaManager arenaManager;
    private final TeamRosterProvider teamProvider;
    private final double multiplierCap;

    public ShopGUIPlusHookListener(
            @NotNull ArenaManager arenaManager,
            @NotNull TeamRosterProvider teamProvider,
            double multiplierCap
    ) {
        this.arenaManager = arenaManager;
        this.teamProvider = teamProvider;
        this.multiplierCap = Math.max(1.0, multiplierCap);
    }

    @EventHandler
    public void onPostEnable(net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent event) {
        // Confirmation event from ShopGUIPlus
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShopPreTransaction(ShopPreTransactionEvent event) {
        if (event.getShopAction() != ShopAction.SELL
                && event.getShopAction() != ShopAction.SELL_ALL) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) return;

        double sellMultiplier = getPlayerSellMultiplier(player);
        if (sellMultiplier > 1.0) {
            double effectiveMultiplier = Math.min(sellMultiplier, multiplierCap);
            double newPrice = event.getPrice() * effectiveMultiplier;
            event.setPrice(newPrice);
        }
    }

    private double getPlayerSellMultiplier(@NotNull Player player) {
        String teamId = teamProvider.getTeamId(player);
        String playerUuid = player.getUniqueId().toString();
        double max = 1.0;

        for (OutpostArena arena : arenaManager.getArenas()) {
            boolean isControlling = arena.getOccupancyMode() == OccupancyMode.SOLO
                    ? playerUuid.equals(arena.getControllerTeamId())
                    : (teamId != null && teamId.equalsIgnoreCase(arena.getControllerTeamId()));

            if (isControlling) {
                double val = arena.getMultiplier(ArenaMultipliers.SHOPGUI_SELL_RATE);
                if (val > max) {
                    max = val;
                }
            }
        }
        return max;
    }
}
