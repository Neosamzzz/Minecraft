package newearth.dimension;

/** Primitive-only boundary between the Forge adapter and the modern terrain engine. */
public interface TerrainSampler {
    int MIN_Y = 0, MAX_Y = 384;
    int OCEAN = 0, RIVER = 1, PLAINS = 2, FOREST = 3, TAIGA = 4,
            JUNGLE = 5, DESERT = 6, SAVANNA = 7, SNOW = 8, MOUNTAIN = 9;
    double height(int x, int z);
    double woodland(int x, int z);
    int biome(int x, int z);
    String biomeName(int x, int z);
    int[] spawnPoint();
    byte[] terrain(int chunkX, int chunkZ);
    boolean solid(int x, int y, int z);
    void close();
}
