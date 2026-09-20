package me.goosbanny.outposts.core.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the 3D cuboid geometry, center point, and warp location for an outpost arena.
 */
public class ArenaGeometry {

    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final double warpX, warpY, warpZ;
    private final float warpYaw, warpPitch;
    private final boolean hasWarp;

    public ArenaGeometry(@NotNull String worldName,
                         int x1, int y1, int z1,
                         int x2, int y2, int z2,
                         @Nullable Location warpLocation) {
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);

        if (warpLocation != null) {
            this.hasWarp = true;
            this.warpX = warpLocation.getX();
            this.warpY = warpLocation.getY();
            this.warpZ = warpLocation.getZ();
            this.warpYaw = warpLocation.getYaw();
            this.warpPitch = warpLocation.getPitch();
        } else {
            this.hasWarp = false;
            this.warpX = (minX + maxX) / 2.0 + 0.5;
            this.warpY = maxY + 1.0;
            this.warpZ = (minZ + maxZ) / 2.0 + 0.5;
            this.warpYaw = 0f;
            this.warpPitch = 0f;
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean isWithinBounds(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    @Nullable
    public Location getCenterLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, (minX + maxX) / 2.0 + 0.5, (minY + maxY) / 2.0 + 0.5, (minZ + maxZ) / 2.0 + 0.5);
    }

    @Nullable
    public Location getWarpLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, warpX, warpY, warpZ, warpYaw, warpPitch);
    }

    public boolean hasWarp() {
        return hasWarp;
    }

    public double getWarpX() {
        return warpX;
    }

    public double getWarpY() {
        return warpY;
    }

    public double getWarpZ() {
        return warpZ;
    }

    public float getWarpYaw() {
        return warpYaw;
    }

    public float getWarpPitch() {
        return warpPitch;
    }
}
