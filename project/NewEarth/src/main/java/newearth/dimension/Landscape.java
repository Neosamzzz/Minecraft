package newearth.dimension;

/** Seeded, world-coordinate fields shared by terrain, biomes and decoration. */
public final class Landscape {
    public static final int WATER_LEVEL = 62;
    private final long seed;
    private final TerrainSampler terrain;

    public Landscape(long seed) {
        this.seed = seed;
        terrain = ReterraTerrain.acquire(seed);
    }

    public double height(int x, int z) { return terrain.height(x, z); }

    public double slope(int x, int z) {
        return Math.max(Math.abs(height(x + 4, z) - height(x - 4, z)),
                Math.abs(height(x, z + 4) - height(x, z - 4))) / 8;
    }

    public double woodland(int x, int z) { return terrain.woodland(x, z); }
    public int biome(int x, int z) { return terrain.biome(x, z); }
    public int[] spawnPoint() { return terrain.spawnPoint(); }

    public boolean tree(int x, int z) {
        double h = height(x, z);
        return h > WATER_LEVEL + 3 && h < 190 && slope(x, z) < 0.55
                && unit(x, z, 10) < woodland(x, z) * 0.85;
    }

    public double unit(int x, int z, int salt) {
        long n = seed ^ ((long) x * 0x632BE59BD9B4E019L)
                ^ ((long) z * 0x9E3779B97F4A7C15L) ^ ((long) salt * 0xD1B54A32D192ED03L);
        n = (n ^ (n >>> 30)) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 27)) * 0x94D049BB133111EBL;
        return ((n ^ (n >>> 31)) >>> 11) * 0x1.0p-53;
    }

}
