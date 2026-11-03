package newearth.dimension;

/** Primitive-only boundary between the Forge adapter and the modern terrain engine. */
public interface TerrainSampler {
    int OCEAN = 0, RIVER = 1, PLAINS = 2, FOREST = 3, TAIGA = 4,
            JUNGLE = 5, DESERT = 6, SAVANNA = 7, SNOW = 8, MOUNTAIN = 9;
    double height(int x, int z);
    double woodland(int x, int z);
    int biome(int x, int z);
    int[] spawnPoint();
    void close();
}
