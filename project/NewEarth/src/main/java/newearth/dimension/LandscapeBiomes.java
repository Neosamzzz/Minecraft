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
        String name=landscape.biomeName(x,z);
        switch(name.substring(name.indexOf(':')+1)) {
            case "deep_ocean": case "deep_cold_ocean": case "deep_lukewarm_ocean": case "deep_frozen_ocean": return Biomes.DEEP_OCEAN;
            case "ocean": case "cold_ocean": case "lukewarm_ocean": case "warm_ocean": return Biomes.OCEAN;
            case "frozen_ocean": return Biomes.FROZEN_OCEAN;
            case "river": return Biomes.RIVER;
            case "frozen_river": return Biomes.FROZEN_RIVER;
            case "swamp": case "mangrove_swamp": return Biomes.SWAMPLAND;
            case "mushroom_fields": return Biomes.MUSHROOM_ISLAND;
            case "beach": return Biomes.BEACH;
            case "snowy_beach": return Biomes.COLD_BEACH;
            case "stony_shore": return Biomes.STONE_BEACH;
            case "desert": return Biomes.DESERT;
            case "badlands": return Biomes.MESA;
            case "wooded_badlands": return Biomes.MESA_ROCK;
            case "eroded_badlands": return Biomes.MUTATED_MESA;
            case "savanna": return Biomes.SAVANNA;
            case "savanna_plateau": return Biomes.SAVANNA_PLATEAU;
            case "windswept_savanna": return Biomes.MUTATED_SAVANNA;
            case "forest": case "windswept_forest": return Biomes.FOREST;
            case "birch_forest": return Biomes.BIRCH_FOREST;
            case "old_growth_birch_forest": return Biomes.MUTATED_BIRCH_FOREST;
            case "dark_forest": return Biomes.ROOFED_FOREST;
            case "flower_forest": case "cherry_grove": return Biomes.MUTATED_FOREST;
            case "jungle": case "bamboo_jungle": return Biomes.JUNGLE;
            case "sparse_jungle": return Biomes.JUNGLE_EDGE;
            case "taiga": return Biomes.TAIGA;
            case "old_growth_pine_taiga": return Biomes.REDWOOD_TAIGA;
            case "old_growth_spruce_taiga": return Biomes.MUTATED_REDWOOD_TAIGA;
            case "snowy_taiga": case "grove": return Biomes.COLD_TAIGA;
            case "snowy_plains": return Biomes.ICE_PLAINS;
            case "ice_spikes": return Biomes.MUTATED_ICE_FLATS;
            case "snowy_slopes": case "jagged_peaks": case "frozen_peaks": return Biomes.ICE_MOUNTAINS;
            case "windswept_hills": case "stony_peaks": return Biomes.EXTREME_HILLS;
            case "windswept_gravelly_hills": return Biomes.MUTATED_EXTREME_HILLS;
            case "sunflower_plains": return Biomes.MUTATED_PLAINS;
            default: return Biomes.PLAINS;
        }
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
