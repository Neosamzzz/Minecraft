package newearth.dimension;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

public final class LandscapeBiomes extends BiomeProvider {
    private final Landscape landscape;
    public LandscapeBiomes(long seed) { landscape = new Landscape(seed); }

    @Override public Biome getBiome(BlockPos pos) { return getBiome(pos, Biomes.PLAINS); }
    @Override public Biome getBiome(BlockPos pos, Biome fallback) { return at(pos.getX(), pos.getZ()); }
    private Biome at(int x, int z) {
        double h = landscape.height(x, z);
        if (h < Landscape.WATER_LEVEL) return Biomes.RIVER;
        if (h > 165) return Biomes.EXTREME_HILLS;
        return landscape.woodland(x, z) > 0.5 && h < 150 ? Biomes.FOREST : Biomes.PLAINS;
    }
    private Biome[] sample(Biome[] out, int x, int z, int width, int depth, int scale) {
        if (out == null || out.length < width * depth) out = new Biome[width * depth];
        for (int dz = 0; dz < depth; dz++) for (int dx = 0; dx < width; dx++)
            out[dx + dz * width] = at((x + dx) * scale, (z + dz) * scale);
        return out;
    }
    @Override public Biome[] getBiomesForGeneration(Biome[] out, int x, int z, int w, int d) {
        return sample(out, x, z, w, d, 4);
    }
    @Override public Biome[] getBiomes(Biome[] out, int x, int z, int w, int d) {
        return sample(out, x, z, w, d, 1);
    }
    @Override public Biome[] getBiomes(Biome[] out, int x, int z, int w, int d, boolean cache) {
        return sample(out, x, z, w, d, 1);
    }
    @Override public List<Biome> getBiomesToSpawnIn() { return Arrays.asList(Biomes.PLAINS, Biomes.FOREST); }
    @Override public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        for (int dz = (z - radius) >> 2; dz <= (z + radius) >> 2; dz++)
            for (int dx = (x - radius) >> 2; dx <= (x + radius) >> 2; dx++)
                if (!allowed.contains(at(dx * 4, dz * 4))) return false;
        return true;
    }
    @Override public BlockPos findBiomePosition(int x, int z, int radius, List<Biome> allowed, Random random) {
        BlockPos result = null;
        int count = 0;
        for (int dz = (z - radius) >> 2; dz <= (z + radius) >> 2; dz++)
            for (int dx = (x - radius) >> 2; dx <= (x + radius) >> 2; dx++)
                if (allowed.contains(at(dx * 4, dz * 4)) && random.nextInt(++count) == 0)
                    result = new BlockPos(dx * 4, 0, dz * 4);
        return result;
    }
    @Override public boolean isFixedBiome() { return false; }
}
