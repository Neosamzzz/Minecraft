package newearth.worldgen;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.init.Biomes;

/** Runtime hooks for the transformed vanilla biome provider. */
public final class IslandHooks {
    private static final java.util.Map<net.minecraft.world.biome.BiomeProvider, State> STATES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<net.minecraft.world.biome.BiomeProvider, State>());

    private static final class State {
        final IslandField field;
        final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);
        State(IslandField field) { this.field = field; }

        Biome biome(int x, int z) {
            int qx = x >> 2;
            int qz = z >> 2;
            long key = ((long) qx << 32) | (qz & 0xFFFFFFFFL);
            int slot = (qx * 31 + qz) & 511;
            Cache c = cache.get();
            if (c.keys[slot] == key && c.values[slot] != null) return c.values[slot];
            int worldX = (qx << 2) + 2;
            int worldZ = (qz << 2) + 2;
            Biome biome = pick(field, field.value(worldX, worldZ), worldX, worldZ);
            c.keys[slot] = key;
            c.values[slot] = biome;
            return biome;
        }
    }

    private static final class Cache {
        final long[] keys = new long[512];
        final Biome[] values = new Biome[512];
    }

    /** Sentinel returned by {@link #findBiomePosition} when the island field has no matching biome. */
    public static final Object NO_POSITION = new Object();

    private IslandHooks() { }

    /** Called after WorldProvider.setWorld has installed the world's biome provider, before spawn generation. */
    public static void registerWorld(Object value) {
        if (!(value instanceof net.minecraft.world.World)) return;
        net.minecraft.world.World world = (net.minecraft.world.World) value;
        net.minecraft.world.biome.BiomeProvider provider = world.getBiomeProvider();
        if (provider == null) return;
        STATES.remove(provider);
        if (!WorldGenConfig.enabled || world.provider.getDimension() != 0
                || world.provider.getClass() != net.minecraft.world.WorldProviderSurface.class
                || provider.getClass() != net.minecraft.world.biome.BiomeProvider.class) return;
        IslandField next = new IslandField(world.getSeed());
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
        STATES.put(provider, new State(next));
    }

    public static IslandField field(net.minecraft.world.World world) {
        State state = world == null ? null : STATES.get(world.getBiomeProvider());
        return state == null ? null : state.field;
    }

    /** True when this chunk is a village anchor: the main island centre or a grid island centre. */
    public static boolean isVillageAnchor(net.minecraft.world.World world, int chunkX, int chunkZ) {
        IslandField current = field(world);
        if (current == null) return false;
        if (chunkX == 0 && chunkZ == 0) return true;
        return current.largeIslandAnchorAtChunk(chunkX, chunkZ) != 0L;
    }
    /** True when a vanilla village may spawn at this chunk: solid land with no nearby water. */
    public static boolean villageAllowed(Object world, int chunkX, int chunkZ) {
        IslandField current = world instanceof net.minecraft.world.World ? field((net.minecraft.world.World) world) : null;
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
    public static void apply(Object provider, Object biomes, int x, int z, int width, int height) {
        State state = STATES.get(provider);
        IslandField current = state == null ? null : state.field;
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
    public static Object biomeAt(Object provider, Object position) {
        State state = STATES.get(provider);
        if (state == null) return null;
        if (!(position instanceof BlockPos)) return null;
        BlockPos pos = (BlockPos) position;
        return state.biome(pos.getX(), pos.getZ());
    }

    /** Mirrors areBiomesViable against the island field; null lets the vanilla method run. */
    public static Object areBiomesViable(Object provider, int x, int z, int radius, java.util.List<Biome> allowed) {
        State state = STATES.get(provider);
        if (state == null || allowed == null) return null;
        for (int i = x - radius; i <= x + radius; i += 2) {
            for (int j = z - radius; j <= z + radius; j += 2) {
                if (!allowed.contains(state.biome(i, j))) return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    /** Mirrors findBiomePosition against the island field; null lets the vanilla method run. */
    public static Object findBiomePosition(Object provider, int x, int z, int range, java.util.List<Biome> allowed) {
        State state = STATES.get(provider);
        if (state == null || allowed == null) return null;
        for (int i = x - range; i <= x + range; i += 4) {
            for (int j = z - range; j <= z + range; j += 4) {
                if (allowed.contains(state.biome(i, j))) return new BlockPos(i, 0, j);
            }
        }
        return NO_POSITION;
    }

    /** Moves the world spawn onto the island when the vanilla spawn search lands in open water. */
    public static void ensureSpawn(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        net.minecraft.world.WorldServer world = server.getWorld(0);
        IslandField current = field(world);
        if (current == null) return;
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
