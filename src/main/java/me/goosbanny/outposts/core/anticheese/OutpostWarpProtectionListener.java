package me.goosbanny.outposts.core.anticheese;

import me.goosbanny.outposts.api.event.OutpostTeleportEvent;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages temporary warp invulnerability/spawn protection upon teleporting to an outpost arena.
 */
public class OutpostWarpProtectionListener implements Listener {

    private final LangManager langManager;
    private final Map<UUID, Long> protectedPlayers = new ConcurrentHashMap<>();

    public OutpostWarpProtectionListener(@NotNull LangManager langManager) {
        this.langManager = langManager;
    }

    public void applyProtection(@NotNull Player player, int durationSeconds) {
        if (durationSeconds <= 0) return;
        long expiry = System.currentTimeMillis() + (durationSeconds * 1000L);
        protectedPlayers.put(player.getUniqueId(), expiry);
        player.sendMessage(langManager.get("commands.spawn_protection_applied", Map.of("seconds", String.valueOf(durationSeconds))));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOutpostTeleport(OutpostTeleportEvent event) {
        Player player = event.getPlayer();
        int seconds = 5;
        if (event.getArena() instanceof DefaultOutpostArena def && def.getDynamicLocationConfig().isEnabled()) {
            seconds = def.getDynamicLocationConfig().getWarpInvincibilitySeconds();
        }
        if (seconds > 0) {
            applyProtection(player, seconds);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Long expiry = protectedPlayers.get(player.getUniqueId());
            if (expiry != null) {
                if (System.currentTimeMillis() < expiry) {
                    event.setCancelled(true);
                } else {
                    protectedPlayers.remove(player.getUniqueId());
                }
            }
        }
    }

    public boolean isProtected(@NotNull UUID uuid) {
        Long expiry = protectedPlayers.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() < expiry) {
            return true;
        }
        protectedPlayers.remove(uuid);
        return false;
    }

    public void removeProtection(@NotNull UUID uuid) {
        protectedPlayers.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker != null) {
            // Invincibility wears off immediately if the player chooses to engage in combat
            if (protectedPlayers.remove(attacker.getUniqueId()) != null) {
                attacker.sendMessage(langManager.get("commands.spawn_protection_expired"));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        protectedPlayers.remove(event.getPlayer().getUniqueId());
    }
}
