package me.goosbanny.outposts.core.multipliers;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.arena.ArenaMultipliers;
import me.goosbanny.outposts.core.manager.ArenaManager;
import me.goosbanny.outposts.core.manager.SpatialGridManager;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Event listener applying economic and combat multipliers to players belonging
 * to factions that control outposts.
 */
public class MultiplierListener implements Listener {

    private final ArenaManager arenaManager;
    private final SpatialGridManager spatialGridManager;
    private final TeamRosterProvider teamProvider;

    public MultiplierListener(
            @NotNull ArenaManager arenaManager,
            @NotNull SpatialGridManager spatialGridManager,
            @NotNull TeamRosterProvider teamProvider
    ) {
        this.arenaManager = arenaManager;
        this.spatialGridManager = spatialGridManager;
        this.teamProvider = teamProvider;
    }

    /**
     * Calculates the highest multiplier held by a player (either via their team or solo ownership) across all controlled outposts.
     */
    private double getPlayerMultiplier(Player player, String multiplierKey) {
        if (player == null) return 1.0;
        String teamId = teamProvider.getTeamId(player);
        String playerUuid = player.getUniqueId().toString();
        double max = 1.0;

        for (OutpostArena arena : arenaManager.getArenas()) {
            boolean isControlling = arena.getOccupancyMode() == OccupancyMode.SOLO
                    ? playerUuid.equals(arena.getControllerTeamId())
                    : (teamId != null && teamId.equalsIgnoreCase(arena.getControllerTeamId()));

            if (isControlling) {
                double val = arena.getMultiplier(multiplierKey);
                if (val > max) {
                    max = val;
                }
            }
        }
        return max;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        // Mob drop multiplier
        double mobDropRate = getPlayerMultiplier(killer, ArenaMultipliers.MOB_DROP_RATE);
        if (mobDropRate > 1.0) {
            List<ItemStack> extraDrops = new ArrayList<>();
            double extraMultiplier = mobDropRate - 1.0;
            for (ItemStack drop : event.getDrops()) {
                int extraAmount = (int) Math.floor(drop.getAmount() * extraMultiplier);
                double remainder = (drop.getAmount() * extraMultiplier) - extraAmount;
                if (ThreadLocalRandom.current().nextDouble() < remainder) {
                    extraAmount++;
                }
                if (extraAmount > 0) {
                    ItemStack extra = drop.clone();
                    extra.setAmount(extraAmount);
                    extraDrops.add(extra);
                }
            }
            event.getDrops().addAll(extraDrops);
        }

        // EXP drop multiplier
        double expDropRate = getPlayerMultiplier(killer, ArenaMultipliers.EXP_DROP_RATE);
        if (expDropRate > 1.0) {
            int newExp = (int) Math.round(event.getDroppedExp() * expDropRate);
            event.setDroppedExp(newExp);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = event.getSpawner();
        if (spawner != null) {
            int chunkX = spawner.getLocation().getBlockX() >> 4;
            int chunkZ = spawner.getLocation().getBlockZ() >> 4;

            // Check if chunk is owned by a controlling outpost team
            if (spatialGridManager.hasTerritoryBoost(spawner.getWorld().getName(), chunkX, chunkZ)) {
                // Accelerate spawner tick delay by 35%
                spawner.setDelay(Math.max(20, (int) (spawner.getDelay() * 0.65)));
                spawner.update();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            double damageRate = getPlayerMultiplier(attacker, ArenaMultipliers.DAMAGE_RATE);
            if (damageRate > 1.0) {
                event.setDamage(event.getDamage() * damageRate);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHungerLoss(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getFoodLevel() < player.getFoodLevel()) {
                String teamId = teamProvider.getTeamId(player);
                String playerUuid = player.getUniqueId().toString();
                for (OutpostArena arena : arenaManager.getArenas()) {
                    boolean isControlling = arena.getOccupancyMode() == OccupancyMode.SOLO
                            ? playerUuid.equals(arena.getControllerTeamId())
                            : (teamId != null && teamId.equalsIgnoreCase(arena.getControllerTeamId()));
                    if (isControlling) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}
