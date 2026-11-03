package newearth.dimension;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shares one bounded terrain cache between each world's biome and chunk adapters. */
final class ReterraTerrain {
    private static final Map<Long, TerrainSampler> WORLDS = new LinkedHashMap<Long, TerrainSampler>(4, 0.75F, true);
    static synchronized TerrainSampler acquire(long seed) {
        TerrainSampler sampler = WORLDS.get(seed);
        if (sampler == null) {
            sampler = TerrainLoader.create(seed);
            WORLDS.put(seed, sampler);
            if (WORLDS.size() > 4) {
                Long oldest = WORLDS.keySet().iterator().next();
                WORLDS.remove(oldest).close();
            }
        }
        return sampler;
    }
    static synchronized void release(long seed) {
        TerrainSampler sampler = WORLDS.remove(seed);
        if (sampler != null) sampler.close();
    }
}
