package me.goosbanny.outposts.core.pipeline.actions;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Action that plays sound effects to nearby players or the entire arena.
 */
public class SoundAction implements ArenaAction {

    private final String soundKey;
    private final float volume;
    private final float pitch;
    private final String target;
    private final me.goosbanny.outposts.api.team.TeamRosterProvider teamProvider;
    private final me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler scheduler;

    public SoundAction(@NotNull String soundKey, float volume, float pitch) {
        this(soundKey, volume, pitch, "ZONE", null, null);
    }

    public SoundAction(
            @NotNull String soundKey,
            float volume,
            float pitch,
            @org.jetbrains.annotations.Nullable String target,
            @org.jetbrains.annotations.Nullable me.goosbanny.outposts.api.team.TeamRosterProvider teamProvider,
            @org.jetbrains.annotations.Nullable me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler scheduler
    ) {
        this.soundKey = soundKey;
        this.volume = volume;
        this.pitch = pitch;
        this.target = target != null ? target : "ZONE";
        this.teamProvider = teamProvider;
        this.scheduler = scheduler;
    }

    @Override
    public @NotNull String getType() {
        return "SOUND";
    }

    @Override
    public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        Sound sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch);

        if (teamProvider != null && scheduler != null && !"ZONE".equalsIgnoreCase(target)) {
            List<Player> targets = me.goosbanny.outposts.core.pipeline.ActionTargetResolver.resolvePlayers(
                    target, "ZONE", arena, context, teamProvider
            );
            for (Player p : targets) {
                if (p != null && p.isOnline()) {
                    scheduler.runForEntity(p, () -> p.playSound(sound));
                }
            }
            return;
        }

        Location center = arena.getCenterLocation();
        if (center != null && center.getWorld() != null) {
            World world = center.getWorld();
            for (Player p : world.getPlayers()) {
                if (arena.isWithinBounds(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ())) {
                    p.playSound(sound);
                }
            }
        }
    }
}
