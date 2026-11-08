package newearth.worldgen;

import java.util.Arrays;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.init.Biomes;
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

    public static void register(Object provider, Object worldInfo) {
        if (!WorldGenConfig.enabled || !(worldInfo instanceof WorldInfo)) {
            field = null;
            return;
        }
        IslandField next = new IslandField(((WorldInfo) worldInfo).getSeed());
        next.mainIslandRadius = WorldGenConfig.mainIslandRadius;
        next.largeIslandCell = WorldGenConfig.largeIslandCell;
        next.largeIslandMinRadius = WorldGenConfig.largeIslandMinRadius;
        next.largeIslandMaxRadius = WorldGenConfig.largeIslandMaxRadius;
        next.isletScale = 1.0 / Math.max(1.0, WorldGenConfig.isletScaleBlocks);
        next.isletLevel = WorldGenConfig.isletLevel;
        next.landLevel = WorldGenConfig.landLevel;
        next.beachWidth = WorldGenConfig.beachWidth;
        next.shelfWidth = WorldGenConfig.shelfWidth;
        next.deepLevel = WorldGenConfig.deepLevel;
        field = next;
    }

    public static IslandField field() {
        return field;
    }

    /** True when a vanilla village may spawn at this chunk: solid land with no nearby water. */
    public static boolean villageAllowed(int chunkX, int chunkZ) {
        IslandField current = field;
        if (current == null) return true;
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        if (current.surface(current.value(x, z)) != IslandField.Surface.LAND) return false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                IslandField.Surface surface = current.surface(current.value(x + dx * 24, z + dz * 24));
                if (surface != IslandField.Surface.LAND && surface != IslandField.Surface.BEACH) return false;
            }
        }
        return true;
    }
    /** Rewrites a vanilla biome grid in place; the array is passed as Object so the call site stays name-neutral. */
    public static void apply(Object biomes, int x, int z, int width, int height) {
        IslandField current = field;
        if (current == null || !(biomes instanceof Biome[])) return;
        Biome[] cells = (Biome[]) biomes;
        // The generation grid is 10x10 cells of four blocks each, with cell coordinates.
        int cellSize = (width == 10 && height == 10) ? 4 : 1;
        int baseX = x * cellSize;
        int baseZ = z * cellSize;
        for (int i = 0; i < width; i++) {
            int worldX = baseX + i * cellSize;
            for (int j = 0; j < height; j++) {
                int worldZ = baseZ + j * cellSize;
                double value = current.value(worldX, worldZ);
                cells[j * width + i] = pick(current, value, worldX, worldZ);
            }
        }
    }

    /** Biome at a block position, cached on a four-block grid like vanilla's biome layer. */
    public static Object biomeAt(Object position) {
        IslandField current = field;
        if (current == null || !(position instanceof BlockPos)) return null;
        BlockPos pos = (BlockPos) position;
        int qx = pos.getX() >> 2;
        int qz = pos.getZ() >> 2;
        int key = qx * 31 + qz;
        int slot = key & CACHE_MASK;
        int[] keys = CACHE_KEYS.get();
        if (keys[slot] == key) {
            Biome cached = CACHE_VALUES.get()[slot];
            if (cached != null) return cached;
        }
        int worldX = (qx << 2) + 2;
        int worldZ = (qz << 2) + 2;
        Biome biome = pick(current, current.value(worldX, worldZ), worldX, worldZ);
        keys[slot] = key;
        CACHE_VALUES.get()[slot] = biome;
        return biome;
    }

    /** Moves the world spawn onto the island when the vanilla spawn search lands in open water. */
    public static void ensureSpawn(net.minecraft.server.MinecraftServer server) {
        IslandField current = field;
        if (current == null || server == null) return;
        net.minecraft.world.WorldServer world = server.getWorld(0);
        if (world == null) return;
        BlockPos spawn = world.getSpawnPoint();
        double dx = spawn.getX();
        double dz = spawn.getZ();
        boolean onMainIsland = dx * dx + dz * dz <= current.mainIslandRadius * current.mainIslandRadius;
        if (onMainIsland && current.surface(current.value(spawn.getX(), spawn.getZ())) == IslandField.Surface.LAND) return;
        BlockPos surface = world.getTopSolidOrLiquidBlock(new BlockPos(0, 64, 0));
        if (surface.getY() <= 63) return;
        if (current.surface(current.value(surface.getX(), surface.getZ())) != IslandField.Surface.LAND) return;
        world.setSpawnPoint(surface);
    }
    private static Biome pick(IslandField current, double value, int x, int z) {
        switch (current.surface(value)) {
            case DEEP_OCEAN: return Biomes.DEEP_OCEAN;
            case OCEAN: return Biomes.OCEAN;
            case BEACH: return Biomes.BEACH;
            case SHELF: return Biomes.RIVER;
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
