package me.goosbanny.outposts.core.feedback;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Dispatches pitch-scaled audio feedback cues during capture progress and completion.
 */
public class AudioCueManager {

    private static final Key TICK_SOUND = Key.key("minecraft:block.note_block.pling");
    private static final Key CAPTURE_SOUND = Key.key("minecraft:ui.toast.challenge_complete");

    /**
     * Plays a pitch-scaled tick sound to players inside the arena as progress advances.
     */
    public void playProgressTick(@NotNull OutpostArena arena, @NotNull List<Player> players) {
        if (players.isEmpty()) return;
        float pitch = 0.5f + (float) ((arena.getProgress() / 100.0) * 1.5);
        Sound sound = Sound.sound(TICK_SOUND, Sound.Source.AMBIENT, 0.8f, pitch);
        for (Player p : players) {
            p.playSound(sound);
        }
    }

    /**
     * Plays a pitch-scaled tick sound to players inside the arena as progress advances.
     */
    public void playProgressTick(@NotNull OutpostArena arena) {
        Location center = arena.getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        // Pitch scales from 0.5 (at 0%) to 2.0 (at 100%)
        float pitch = 0.5f + (float) ((arena.getProgress() / 100.0) * 1.5);
        Sound sound = Sound.sound(TICK_SOUND, Sound.Source.AMBIENT, 0.8f, pitch);

        World world = center.getWorld();
        try {
            BoundingBox bb = new BoundingBox(
                    arena.getMinX(), arena.getMinY(), arena.getMinZ(),
                    arena.getMaxX() + 1.0, arena.getMaxY() + 1.0, arena.getMaxZ() + 1.0
            );
            Collection<Entity> nearby = world.getNearbyEntities(bb, e -> e instanceof Player);
            for (Entity e : nearby) {
                if (e instanceof Player p) {
                    p.playSound(sound);
                }
            }
        } catch (Throwable t) {
            for (Player p : world.getPlayers()) {
                if (FoliaCompatScheduler.isOwnedByCurrentRegion(p)) {
                    Location loc = p.getLocation();
                    if (arena.isWithinBounds(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                        p.playSound(sound);
                    }
                }
            }
        }
    }

    /**
     * Plays the triumphant completion fanfare when an outpost is captured.
     */
    public void playCaptureFanfare(@NotNull OutpostArena arena) {
        Location center = arena.getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        Sound sound = Sound.sound(CAPTURE_SOUND, Sound.Source.MASTER, 1.2f, 1.0f);
        World world = center.getWorld();
        try {
            Collection<Entity> nearby = world.getNearbyEntities(center, 64.0, 64.0, 64.0, e -> e instanceof Player);
            for (Entity e : nearby) {
                if (e instanceof Player p) {
                    p.playSound(sound);
                }
            }
        } catch (Throwable t) {
            for (Player p : world.getPlayers()) {
                if (FoliaCompatScheduler.isOwnedByCurrentRegion(p)) {
                    if (p.getLocation().distanceSquared(center) <= 4096.0) {
                        p.playSound(sound);
                    }
                }
            }
        }
    }
}
