package me.goosbanny.outposts.core.anticheese;

import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.event.OutpostAntiCheeseTriggerEvent;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates player eligibility via line of sight, combat tagging,
 * flight, godmode, and vanish checks.
 */
public class AntiCheeseValidator {

    private final AntiCheeseConfig config;
    private final Map<UUID, Long> lastCombatDamageTimes = new ConcurrentHashMap<>();

    public AntiCheeseValidator(@NotNull AntiCheeseConfig config) {
        this.config = config;
    }

    public AntiCheeseConfig getConfig() {
        return config;
    }

    /**
     * Records combat damage taken by a player for the combat interruption policy.
     */
    public void registerCombatDamage(@NotNull Player player, @NotNull OutpostArena arena) {
        lastCombatDamageTimes.put(player.getUniqueId(), System.currentTimeMillis());

        if (config.getCombatDamagePolicy() == CombatDamagePolicy.RESET_PROGRESS) {
            arena.setProgress(0.0);
            Bukkit.getPluginManager().callEvent(new OutpostAntiCheeseTriggerEvent(
                    arena, player, OutpostAntiCheeseTriggerEvent.ViolationType.COMBAT_RESET
            ));
        }
    }

    private final Map<String, Long> lastViolationEventTimes = new ConcurrentHashMap<>();

    private void fireViolationEvent(@NotNull OutpostArena arena, @NotNull Player player, @NotNull OutpostAntiCheeseTriggerEvent.ViolationType type) {
        String key = player.getUniqueId() + ":" + type.name();
        long now = System.currentTimeMillis();
        Long last = lastViolationEventTimes.get(key);
        if (last == null || now - last >= 5000L) {
            lastViolationEventTimes.put(key, now);
            Bukkit.getPluginManager().callEvent(new OutpostAntiCheeseTriggerEvent(arena, player, type));
        }
    }

    /**
     * Evaluates whether a player inside the capture pad is a valid, eligible capper.
     *
     * @param arena  arena being captured
     * @param player player to test
     * @return true if player is allowed to contribute
     */
    public boolean isValidCapper(@NotNull OutpostArena arena, @NotNull Player player) {
        // Creative / Spectator bypass or ignore
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        // 1. Flight check
        if (config.isDisallowFlying() && (player.isFlying() || (player.getAllowFlight() && player.getGameMode() != GameMode.CREATIVE))) {
            fireViolationEvent(arena, player, OutpostAntiCheeseTriggerEvent.ViolationType.FLYING);
            return false;
        }

        // 2. Elytra glide check
        if (config.isDisallowElytra() && player.isGliding()) {
            fireViolationEvent(arena, player, OutpostAntiCheeseTriggerEvent.ViolationType.ELYTRA_GLIDE);
            return false;
        }

        // 3. Godmode & Spawn Protection check
        if (config.isDisallowGodmode()) {
            if (player.isInvulnerable() || player.hasMetadata("godmode") || player.hasMetadata("god")) {
                fireViolationEvent(arena, player, OutpostAntiCheeseTriggerEvent.ViolationType.GODMODE);
                return false;
            }
        }
        Outposts plugin = Outposts.getInstance();
        if (plugin != null && plugin.getWarpProtectionListener() != null) {
            if (plugin.getWarpProtectionListener().isProtected(player.getUniqueId())) {
                return false; // Active warp spawn protection prevents capping/contesting
            }
        }

        // 4. Vanish check
        if (config.isDisallowVanished()) {
            if (player.hasMetadata("vanished") || player.hasMetadata("vanish")) {
                fireViolationEvent(arena, player, OutpostAntiCheeseTriggerEvent.ViolationType.VANISHED);
                return false;
            }
        }

        // 5. Combat pause policy
        if (config.getCombatDamagePolicy() == CombatDamagePolicy.PAUSE_CAPTURE) {
            Long lastDmg = lastCombatDamageTimes.get(player.getUniqueId());
            if (lastDmg != null) {
                long elapsed = System.currentTimeMillis() - lastDmg;
                if (elapsed < config.getCombatPauseSeconds() * 1000L) {
                    fireViolationEvent(arena, player, OutpostAntiCheeseTriggerEvent.ViolationType.COMBAT_PAUSED);
                    return false;
                }
            }
        }

        // 6. Raytraced Line-of-Sight Check
        if (config.isLineOfSight()) {
            Location center = arena.getCenterLocation();
            if (center != null && center.getWorld() != null && center.getWorld().equals(player.getWorld())) {
                Location eyeLoc = player.getEyeLocation();
                Vector direction = center.toVector().subtract(eyeLoc.toVector());
                double distance = direction.length();

                if (distance > 0.5) {
                    direction.normalize();
                    RayTraceResult result = center.getWorld().rayTraceBlocks(
                            eyeLoc,
                            direction,
                            distance,
                            FluidCollisionMode.NEVER,
                            true
                    );

                    if (result != null && result.getHitBlock() != null) {
                        Block block = result.getHitBlock();
                        // Ignore non-occluding or passable decorative blocks (signs, banners, vines, tall grass)
                        if (block.getType().isOccluding()) {
                            Bukkit.getPluginManager().callEvent(new OutpostAntiCheeseTriggerEvent(
                                    arena, player, OutpostAntiCheeseTriggerEvent.ViolationType.NO_LINE_OF_SIGHT
                            ));
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public void cleanupPlayer(UUID uuid) {
        lastCombatDamageTimes.remove(uuid);
        String prefix = uuid.toString() + ":";
        lastViolationEventTimes.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clearAll() {
        lastCombatDamageTimes.clear();
        lastViolationEventTimes.clear();
    }
}
