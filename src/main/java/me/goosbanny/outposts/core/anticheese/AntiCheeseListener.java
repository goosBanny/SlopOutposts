package me.goosbanny.outposts.core.anticheese;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.manager.SpatialGridManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Event listener enforcing anti-cheese constraints, combat tags, blocked commands,
 * and block protection within outpost cuboid boundaries.
 * Messages are dynamically resolved from lang.yml.
 */
public class AntiCheeseListener implements Listener {

    private final AntiCheeseValidator validator;
    private final SpatialGridManager spatialGridManager;
    private final LangManager langManager;
    private final Set<String> blockedCommands;
    private final boolean preventBlockBreak;
    private final boolean preventBlockPlace;
    private final boolean preventChorusFruit;

    public AntiCheeseListener(
            @NotNull AntiCheeseValidator validator,
            @NotNull SpatialGridManager spatialGridManager,
            @NotNull LangManager langManager,
            @NotNull List<String> blockedCommands,
            boolean preventBlockBreak,
            boolean preventBlockPlace,
            boolean preventChorusFruit
    ) {
        this.validator = validator;
        this.spatialGridManager = spatialGridManager;
        this.langManager = langManager;
        this.blockedCommands = Set.copyOf(blockedCommands);
        this.preventBlockBreak = preventBlockBreak;
        this.preventBlockPlace = preventBlockPlace;
        this.preventChorusFruit = preventChorusFruit;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            Location loc = victim.getLocation();
            OutpostArena arena = spatialGridManager.getArenaAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            if (arena != null) {
                validator.registerCombatDamage(victim, arena);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("outposts.admin.bypass")) {
            return;
        }

        Location loc = player.getLocation();
        OutpostArena arena = spatialGridManager.getArenaAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (arena == null) {
            return;
        }

        String raw = event.getMessage().substring(1).toLowerCase();
        for (String blocked : blockedCommands) {
            if (raw.equals(blocked.toLowerCase()) || raw.startsWith(blocked.toLowerCase() + " ")) {
                event.setCancelled(true);
                player.sendMessage(langManager.get("commands.blocked_command"));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!preventBlockBreak || event.getPlayer().hasPermission("outposts.admin.bypass")) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        OutpostArena arena = spatialGridManager.getArenaAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (arena != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(langManager.get("commands.blocked_block_break"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!preventBlockPlace || event.getPlayer().hasPermission("outposts.admin.bypass")) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        OutpostArena arena = spatialGridManager.getArenaAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (arena != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(langManager.get("commands.blocked_block_place"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!preventChorusFruit || event.getCause() != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }
        Location to = event.getTo();
        if (to == null) return;
        OutpostArena arena = spatialGridManager.getArenaAt(to.getWorld().getName(), to.getBlockX(), to.getBlockY(), to.getBlockZ());
        if (arena != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(langManager.get("commands.blocked_chorus"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        validator.cleanupPlayer(event.getPlayer().getUniqueId());
    }
}
