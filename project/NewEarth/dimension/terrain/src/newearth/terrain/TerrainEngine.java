package newearth.terrain;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import newearth.dimension.TerrainSampler;
import newearth.terrain.data.worldgen.preset.PresetNoiseData;
import newearth.terrain.data.worldgen.preset.settings.Preset;
import newearth.terrain.world.worldgen.GeneratorContext;
import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.densityfunction.tile.Tile;
import newearth.terrain.world.worldgen.util.PosUtil;

/** Full RTF height/climate pipeline; only the final block-height mapping is platform specific. */
public final class TerrainEngine implements TerrainSampler {
    private final GeneratorContext context;
    private final Map<Long, Snapshot> tiles = new LinkedHashMap<>(16, 0.75F, true);
    private static final int TILE_BITS = 7, WIDTH = 1 << TILE_BITS, MASK = WIDTH - 1;

    public TerrainEngine(long seed) {
        var stream = TerrainEngine.class.getResourceAsStream("/newearth/terrain/default-preset.json");
        if (stream == null) throw new IllegalStateException("Missing RTF default preset");
        Preset preset;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            preset = new Gson().fromJson(reader, Preset.class);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
        var registry = new NoiseRegistry();
        PresetNoiseData.bootstrap(preset, registry);
        // RTF's RandomState adapter also narrows the world seed to int.
        context = GeneratorContext.makeUncached(preset, registry, (int) seed, 3, 1, 1);
    }

    private record Snapshot(float[] height, byte[] biome, float[] woodland) {}

    private synchronized Snapshot tile(int x, int z) {
        long key = PosUtil.pack(x >> TILE_BITS, z >> TILE_BITS);
        Snapshot found = tiles.get(key);
        if (found != null) return found;
        Tile generated = context.generator.generate(x >> TILE_BITS, z >> TILE_BITS).join();
        float[] height = new float[WIDTH * WIDTH], woodland = new float[WIDTH * WIDTH];
        byte[] biome = new byte[WIDTH * WIDTH];
        try {
            for (int dz = 0; dz < WIDTH; dz++) for (int dx = 0; dx < WIDTH; dx++) {
                Cell cell = generated.lookup(dx, dz);
                int index = dz * WIDTH + dx;
                height[index] = cell.height * context.levels.worldHeight;
                if (!Float.isFinite(height[index])) throw new IllegalStateException("Non-finite terrain height");
                biome[index] = (byte) biome(cell, height[index]);
                woodland[index] = woodland(cell, height[index]);
            }
        } finally {
            generated.close();
        }
        found = new Snapshot(height, biome, woodland);
        tiles.put(key, found);
        if (tiles.size() > 16) tiles.remove(tiles.keySet().iterator().next());
        return found;
    }

    private static int index(int x, int z) { return (z & MASK) * WIDTH + (x & MASK); }

    /** Continuous slope at Y192; approaches Y252 without a clipped mountain plateau. */
    public static double mapHeight(double height) {
        return Math.max(5, height <= 192 ? height : 192 + 60 * -Math.expm1(-(height - 192) / 60));
    }

    @Override public double height(int x, int z) { return mapHeight(tile(x, z).height[index(x, z)]); }
    @Override public double woodland(int x, int z) { return tile(x, z).woodland[index(x, z)]; }
    @Override public int biome(int x, int z) { return tile(x, z).biome[index(x, z)]; }
    @Override public synchronized void close() { tiles.clear(); context.generator.getHeightmap().continent().close(); }

    private int biome(Cell cell, float height) {
        if (cell.terrain.isDeepOcean() || cell.terrain.isShallowOcean()) return OCEAN;
        if ((cell.terrain.isRiver() || cell.terrain.isLake()) && height <= context.levels.waterLevel) return RIVER;
        if (height > 205) return SNOW;
        if (height > 165) return MOUNTAIN;
        return switch (cell.biome) {
            case DESERT -> DESERT;
            case SAVANNA -> SAVANNA;
            case TROPICAL_RAINFOREST -> JUNGLE;
            case TEMPERATE_RAINFOREST, TEMPERATE_FOREST -> FOREST;
            case TAIGA -> TAIGA;
            case TUNDRA, ALPINE -> SNOW;
            default -> PLAINS;
        };
    }

    private static float woodland(Cell cell, float height) {
        float cover = switch (cell.biome) {
            case TEMPERATE_FOREST -> 0.85F;
            case TEMPERATE_RAINFOREST, TROPICAL_RAINFOREST -> 0.95F;
            case TAIGA -> 0.7F;
            case SAVANNA -> 0.08F;
            case GRASSLAND, STEPPE, COLD_STEPPE -> 0.015F;
            default -> 0;
        };
        return cover * Math.max(0, Math.min(1, (190 - height) / 45));
    }

    @Override public int[] spawnPoint() {
        long center = context.generator.getHeightmap().continent().getNearestCenter(0, 0);
        int cx = PosUtil.unpackLeft(center), cz = PosUtil.unpackRight(center);
        Cell cell = new Cell();
        for (int radius = 0; radius <= 1024; radius += 32) {
            int directions = radius == 0 ? 1 : 32;
            for (int i = 0; i < directions; i++) {
                double angle = i * Math.PI * 2 / directions;
                int x = cx + (int) (Math.cos(angle) * radius), z = cz + (int) (Math.sin(angle) * radius);
                cell.reset();
                context.generator.getHeightmap().apply(cell, x, z, true);
                double raw = cell.height * context.levels.worldHeight;
                if (raw < 68 || raw > 145 || cell.terrain.isRiver() || cell.terrain.isLake()) continue;
                double y = height(x, z);
                if (y > 65 && y < 150) return new int[] {x, (int) Math.floor(y) + 1, z};
            }
        }
        // The selected continent remains the fallback; the caller can inspect this point.
        return new int[] {cx, (int) Math.floor(height(cx, cz)) + 1, cz};
    }
}
