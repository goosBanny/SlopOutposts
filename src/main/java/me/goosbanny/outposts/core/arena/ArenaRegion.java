package me.goosbanny.outposts.core.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single predefined region candidate in a dynamic shifting outpost.
 */
public class ArenaRegion {

    private final String id;
    private final String rawName;
    private final Component displayName;
    private final int weight;
    private final ArenaGeometry geometry;

    public ArenaRegion(
            @NotNull String id,
            @Nullable String rawName,
            int weight,
            @NotNull ArenaGeometry geometry
    ) {
        this.id = id.toLowerCase();
        this.rawName = rawName != null ? rawName : "<yellow>" + id + "</yellow>";
        this.displayName = MiniMessage.miniMessage().deserialize(this.rawName);
        this.weight = Math.max(0, weight);
        this.geometry = geometry;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getRawName() {
        return rawName;
    }

    @NotNull
    public Component getDisplayName() {
        return displayName;
    }

    public int getWeight() {
        return weight;
    }

    @NotNull
    public ArenaGeometry getGeometry() {
        return geometry;
    }
}
