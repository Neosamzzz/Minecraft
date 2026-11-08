package newearth.worldgen;

/**
 * Island layout for the main world: one large island around the origin, a few larger islands on a
 * deterministic grid, and small islets from noise. Pure math so the shape can be tuned offline.
 */
public final class IslandField {
    public enum Surface { DEEP_OCEAN, OCEAN, SHELF, BEACH, LAND }

    public static final int PLAINS = 0, FOREST = 1, TAIGA = 2, DESERT = 3, HILLS = 4;

    private final long seed;
    private final Perlin2D shape;
    private final Perlin2D coast;
    private final Perlin2D temperature;
    private final Perlin2D humidity;

    /** 1 / islet feature size in blocks. */
    public double isletScale = 1.0 / 60.0;
    /** Raw islet noise value where an islet starts; higher means fewer islets. */
    public double isletLevel = 0.38;
    /** Shared field value where ocean becomes land. */
    public double landLevel = 0.60;
    /** Width of the beach ring, in field units. */
    public double beachWidth = 0.03;
    /** Width of the shallow shelf between beach and ocean, in field units. */
    public double shelfWidth = 0.06;
    /** Below this value the sea is deep ocean. */
    public double deepLevel = 0.50;
    /** Radius of the guaranteed main island around the origin, in blocks. */
    public double mainIslandRadius = 100.0;
    /** How far the island field rises above the land level at its centre. */
    public double mainIslandRise = 0.08;

    /** Grid cell size for the extra large islands; one island per cell. */
    public double largeIslandCell = 800.0;
    /** Radius range of the extra large islands. */
    public double largeIslandMinRadius = 50.0;
    public double largeIslandMaxRadius = 80.0;

    public IslandField(long seed) {
        this.seed = seed;
        shape = new Perlin2D(seed * 341873128712L + 132897987541L);
        coast = new Perlin2D(seed * 981625601L + 271828182L);
        temperature = new Perlin2D(seed * 1414213562L + 173205080L);
        humidity = new Perlin2D(seed * 2236067977L + 316227766L);
    }

    /** Raw islet noise before the land-level mapping; exposed for tuning. */
    public double isletNoise(double x, double z) {
        return shape.fractal(x * isletScale, z * isletScale, 3);
    }

    /** Island field; land is at or above {@link #landLevel}. */
    public double value(double x, double z) {
        double islets = landLevel + (isletNoise(x, z) - isletLevel);
        double main = islandValue(x, z, 0.0, 0.0, mainIslandRadius);
        double large = Double.NEGATIVE_INFINITY;
        if (largeIslandCell > 0.0) {
            long cellX = Math.floorDiv((long) x, (long) largeIslandCell);
            long cellZ = Math.floorDiv((long) z, (long) largeIslandCell);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    double[] island = largeIsland(cellX + dx, cellZ + dz);
                    if (island == null) continue;
                    large = Math.max(large, islandValue(x, z, island[0], island[1], island[2]));
                }
            }
        }
        return Math.max(islets, Math.max(main, large));
    }

    /** Own radial island shape: solid centre, ragged coast only. */
    private double islandValue(double x, double z, double centerX, double centerZ, double radius) {
        double warpScale = Math.max(18.0, radius * 0.45);
        double warpAmount = radius * 0.45;
        double warpedX = x + warpAmount * coast.noise(x / warpScale, z / warpScale);
        double warpedZ = z + warpAmount * coast.noise(x / warpScale + 37.0, z / warpScale - 21.0);
        double dx = warpedX - centerX;
        double dz = warpedZ - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance >= radius) return Double.NEGATIVE_INFINITY;
        double t = distance / radius;
        double taper = 1.0 - t * t;
        return landLevel + mainIslandRise * taper
                + 0.18 * shape.noise(x / 22.0, z / 22.0) * t * t
                + 0.07 * shape.noise(x / 10.0, z / 10.0) * t * t * t;
    }

    /** Centre and radius of the grid island in the given cell, or null when the cell is skipped. */
    private double[] largeIsland(long cellX, long cellZ) {
        long hash = hash(cellX, cellZ);
        double offsetX = 0.25 + 0.50 * ((hash >>> 16) & 0xFFFFL) / 65535.0;
        double offsetZ = 0.25 + 0.50 * ((hash >>> 32) & 0xFFFFL) / 65535.0;
        double radius = largeIslandMinRadius
                + (largeIslandMaxRadius - largeIslandMinRadius) * ((hash >>> 48) & 0xFFFFL) / 65535.0;
        radius = Math.round(radius / 4.0) * 4.0;
        double centerX = cellX * largeIslandCell + offsetX * largeIslandCell;
        double centerZ = cellZ * largeIslandCell + offsetZ * largeIslandCell;
        // Snap to chunk centres so the island lines up with the four-block generation grid.
        centerX = Math.floor(centerX / 16.0) * 16.0 + 8.0;
        centerZ = Math.floor(centerZ / 16.0) * 16.0 + 8.0;
        // Keep the spawn island clear of grid islands.
        if (Math.sqrt(centerX * centerX + centerZ * centerZ) < mainIslandRadius + radius + 120.0) return null;
        return new double[]{centerX, centerZ, radius};
    }

    private long hash(long cellX, long cellZ) {
        long h = seed + cellX * 0x9E3779B97F4A7C15L + cellZ * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /** Packed key of the grid island whose centre lies in this chunk, or 0. */
    public long largeIslandAnchorAtChunk(int chunkX, int chunkZ) {
        if (largeIslandCell <= 0.0) return 0L;
        int blockX = chunkX * 16 + 8;
        int blockZ = chunkZ * 16 + 8;
        long cellX = Math.floorDiv((long) blockX, (long) largeIslandCell);
        long cellZ = Math.floorDiv((long) blockZ, (long) largeIslandCell);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long cx = cellX + dx;
                long cz = cellZ + dz;
                double[] island = largeIsland(cx, cz);
                if (island == null) continue;
                if (((int) Math.floor(island[0]) >> 4) == chunkX
                        && ((int) Math.floor(island[1]) >> 4) == chunkZ) {
                    return pack(cx, cz);
                }
            }
        }
        return 0L;
    }

    /** Centre block of a grid island from its packed key. */
    public int[] largeIslandCenter(long key) {
        long cx = key >> 32;
        long cz = key & 0xFFFFFFFFL;
        double[] island = largeIsland(cx, cz);
        if (island == null) return null;
        return new int[]{(int) Math.floor(island[0]), (int) Math.floor(island[1]), (int) island[2]};
    }

    private static long pack(long cellX, long cellZ) {
        return (cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    public Surface surface(double v) {
        if (v >= landLevel + beachWidth) return Surface.LAND;
        if (v >= landLevel) return Surface.BEACH;
        if (v >= landLevel - shelfWidth) return Surface.SHELF;
        if (v >= deepLevel) return Surface.OCEAN;
        return Surface.DEEP_OCEAN;
    }

    public int landBiomeAt(int x, int z) {
        return landBiomeAt(value(x, z), x, z);
    }

    public int landBiomeAt(double v, int x, int z) {
        if (v >= landLevel + beachWidth + 0.22) return HILLS;
        double t = temperature.noise(x / 1450.0, z / 1450.0);
        if (t < -0.32) return TAIGA;
        if (t > 0.36) return DESERT;
        return humidity.noise(x / 900.0, z / 900.0) > 0.05 ? FOREST : PLAINS;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }
}
