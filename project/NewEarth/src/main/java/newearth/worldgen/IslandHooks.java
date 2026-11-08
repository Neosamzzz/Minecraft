package newearth.worldgen;

import java.util.Arrays;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.WorldInfo;

/** Runtime hooks for the transformed vanilla biome provider. */
public final class IslandHooks {
    private static final int CACHE_BITS = 9;
    private static final int CACHE_MASK = (1 << CACHE_BITS) - 1;
    private static final ThreadLocal<int[]> CACHE_KEYS = ThreadLocal.withInitial(() -> {
        int[] keys = new int[1 << CACHE_BITS];
        Arrays.fill(keys, Integer.MIN_VALUE);
        return keys;
    });
    private static final ThreadLocal<Biome[]> CACHE_VALUES = ThreadLocal.withInitial(() -> new Biome[1 << CACHE_BITS]);

    private static volatile IslandField field;

    private IslandHooks() { }

    public static void register(Object provider, WorldInfo info) {
        if (!WorldGenConfig.enabled || info == null) {
            field = null;
            return;
        }
        IslandField next = new IslandField(info.getSeed());
        next.mainIslandRadius = WorldGenConfig.mainIslandRadius;
        next.isletScale = 1.0 / Math.max(1.0, WorldGenConfig.isletScaleBlocks);
        next.isletLevel = WorldGenConfig.isletLevel;
        next.landLevel = WorldGenConfig.landLevel;
        next.beachWidth = WorldGenConfig.beachWidth;
        next.deepLevel = WorldGenConfig.deepLevel;
        next.isletFadeStart = WorldGenConfig.isletFadeStart;
        next.isletFadeEnd = WorldGenConfig.isletFadeEnd;
        field = next;
    }

    /** Rewrites a vanilla biome grid in place. */
    public static void apply(Biome[] biomes, int x, int z, int width, int height) {
        IslandField current = field;
        if (current == null || biomes == null) return;
        for (int i = 0; i < width; i++) {
            int worldX = x + i;
            for (int j = 0; j < height; j++) {
                int worldZ = z + j;
                double value = current.value(worldX, worldZ);
                biomes[j * width + i] = pick(current, value, worldX, worldZ);
            }
        }
    }

    /** Biome at a block position, cached on a four-block grid like vanilla's biome layer. */
    public static Biome biomeAt(BlockPos pos) {
        IslandField current = field;
        if (current == null) return null;
        int qx = pos.getX() >> 2;
        int qz = pos.getZ() >> 2;
        int slot = (qx * 31 + qz) & CACHE_MASK;
        int[] keys = CACHE_KEYS.get();
        if (keys[slot] == qx * 31 + qz) {
            Biome cached = CACHE_VALUES.get()[slot];
            if (cached != null) return cached;
        }
        int worldX = (qx << 2) + 2;
        int worldZ = (qz << 2) + 2;
        Biome biome = pick(current, current.value(worldX, worldZ), worldX, worldZ);
        keys[slot] = qx * 31 + qz;
        CACHE_VALUES.get()[slot] = biome;
        return biome;
    }

    private static Biome pick(IslandField current, double value, int x, int z) {
        switch (current.surface(value)) {
            case DEEP_OCEAN: return Biomes.DEEP_OCEAN;
            case OCEAN: return Biomes.OCEAN;
            case BEACH: return Biomes.BEACH;
            default: break;
        }
        switch (current.landBiomeAt(value, x, z)) {
            case IslandField.FOREST: return Biomes.FOREST;
            case IslandField.TAIGA: return Biomes.TAIGA;
            case IslandField.DESERT: return Biomes.DESERT;
            case IslandField.HILLS: return Biomes.EXTREME_HILLS;
            default: return Biomes.PLAINS;
        }
    }
}
