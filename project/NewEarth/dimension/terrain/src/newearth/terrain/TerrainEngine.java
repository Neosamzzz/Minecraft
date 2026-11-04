package newearth.terrain;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import newearth.dimension.TerrainSampler;
import newearth.terrain.density.DensityRouter;
import newearth.terrain.data.worldgen.preset.PresetNoiseData;
import newearth.terrain.data.worldgen.preset.settings.Preset;
import newearth.terrain.world.worldgen.GeneratorContext;
import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.densityfunction.tile.Tile;
import newearth.terrain.world.worldgen.util.PosUtil;
import newearth.terrain.world.worldgen.noise.NoiseUtil;
import newearth.terrain.world.worldgen.cell.terrain.TerrainCategory;

/** Full RTF height/climate pipeline; only the final block-height mapping is platform specific. */
public final class TerrainEngine implements TerrainSampler {
    private final GeneratorContext context;
    private final DensityRouter density;
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
        density = new DensityRouter(seed, this::field);
    }

    private record Snapshot(float[] height, byte[] biome, float[] woodland, float[][] climate, String[] names) {}

    private synchronized Snapshot tile(int x, int z) {
        long key = PosUtil.pack(x >> TILE_BITS, z >> TILE_BITS);
        Snapshot found = tiles.get(key);
        if (found != null) return found;
        Tile generated = context.generator.generate(x >> TILE_BITS, z >> TILE_BITS).join();
        float[] height = new float[WIDTH * WIDTH], woodland = new float[WIDTH * WIDTH];
        byte[] biome = new byte[WIDTH * WIDTH];
        float[][] climate = new float[5][WIDTH * WIDTH];
        String[] names=new String[32*32];
        try {
            for(int qz=0;qz<32;qz++)for(int qx=0;qx<32;qx++) {
                Cell c=generated.lookup(qx*4,qz*4);
                names[qz*32+qx]=ClimateBiomes.at(c.temperature,c.moisture,continentalness(c),c.erosion,0,c.weirdness);
            }
            for (int dz = 0; dz < WIDTH; dz++) for (int dx = 0; dx < WIDTH; dx++) {
                Cell cell = generated.lookup(dx, dz);
                int index = dz * WIDTH + dx;
                height[index] = cell.height * context.levels.worldHeight;
                if (!Float.isFinite(height[index])) throw new IllegalStateException("Non-finite terrain height");
                String name=names[(dz>>2)*32+(dx>>2)];
                biome[index] = (byte) kind(name);
                woodland[index] = cover(name);
                climate[0][index]=continentalness(cell);
                climate[1][index]=cell.erosion;climate[2][index]=cell.weirdness;
                climate[3][index]=cell.temperature;climate[4][index]=cell.moisture;
            }
        } finally {
            generated.close();
        }
        found = new Snapshot(height, biome, woodland, climate, names);
        tiles.put(key, found);
        if (tiles.size() > 16) tiles.remove(tiles.keySet().iterator().next());
        return found;
    }

    private static int index(int x, int z) { return (z & MASK) * WIDTH + (x & MASK); }

    @Override public double height(int x, int z) { return tile(x, z).height[index(x, z)]; }
    @Override public double woodland(int x, int z) { return tile(x, z).woodland[index(x, z)]; }
    @Override public int biome(int x, int z) { return tile(x, z).biome[index(x, z)]; }
    @Override public String biomeName(int x,int z) {return tile(x,z).names[((z&MASK)>>2)*32+((x&MASK)>>2)];}
    @Override public synchronized void close() { tiles.clear(); context.generator.getHeightmap().continent().close(); }

    public double field(String name, int x, int z) {
        Snapshot snapshot=tile(x,z);int i=index(x,z);
        return switch(name) {
            case "height"->snapshot.height[i]/256F;
            case "continent"->snapshot.climate[0][i];case "erosion"->snapshot.climate[1][i];
            case "weirdness"->snapshot.climate[2][i];case "temperature"->snapshot.climate[3][i];
            case "moisture"->snapshot.climate[4][i];
            default->throw new IllegalArgumentException("Unknown terrain field "+name);
        };
    }

    private float continentalness(Cell cell) {
        var points=context.preset.world().controlPoints;
        if(cell.terrain.isDeepOcean())return NoiseUtil.lerp(-1.05F+.05F,-.455F,
                NoiseUtil.lerp(NoiseUtil.clamp(cell.continentEdge,0,points.deepOcean),0,points.deepOcean,0,1));
        if(cell.terrain.isShallowOcean())return NoiseUtil.lerp(-.455F,-.19F,
                NoiseUtil.lerp(NoiseUtil.clamp(cell.continentEdge,points.deepOcean,points.shallowOcean),points.deepOcean,points.shallowOcean,0,.98F));
        if(cell.terrain.getDelegate()==TerrainCategory.BEACH && cell.height+cell.beachNoise<context.levels.water(5))
            return NoiseUtil.lerp(-.19F,-.11F,NoiseUtil.lerp(NoiseUtil.clamp(cell.continentEdge,points.shallowOcean,points.beach),points.shallowOcean,points.beach,0,1));
        return NoiseUtil.lerp(-.04F,1F,NoiseUtil.lerp(NoiseUtil.clamp(cell.continentEdge,points.beach,points.inland),points.beach,points.inland,0,1));
    }

    @Override public synchronized boolean solid(int x,int y,int z) {return density.sample(x,y,z)>0;}

    @Override public synchronized byte[] terrain(int chunkX,int chunkZ) {
        density.beginChunk();
        int height=MAX_Y-MIN_Y;byte[] blocks=new byte[16*16*height];
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=MIN_Y;y<MAX_Y;y++) {
            int wx=chunkX*16+x,wz=chunkZ*16+z;
            blocks[(x*16+z)*height+y-MIN_Y]=(byte)(density.sample(wx,y,wz)>0?1:y<=-54?3:y<63?2:0);
        }
        return blocks;
    }

    private static int kind(String name) {
        if(name.contains("ocean"))return OCEAN;
        if(name.endsWith("river"))return RIVER;
        if(name.contains("snow") || name.contains("frozen") || name.contains("ice_spikes") || name.endsWith("grove") && !name.endsWith("cherry_grove"))return SNOW;
        if(name.contains("desert") || name.contains("badlands"))return DESERT;
        if(name.contains("savanna"))return SAVANNA;
        if(name.contains("jungle"))return JUNGLE;
        if(name.contains("taiga"))return TAIGA;
        if(name.contains("forest") || name.endsWith("cherry_grove"))return FOREST;
        if(name.contains("peaks") || name.contains("hills"))return MOUNTAIN;
        return PLAINS;
    }

    private static float cover(String name) {
        if(name.contains("jungle"))return .95F;
        if(name.endsWith("dark_forest"))return .95F;
        if(name.contains("forest") || name.endsWith("grove"))return .85F;
        if(name.contains("taiga"))return .75F;
        if(name.contains("savanna"))return .08F;
        if(name.endsWith("plains"))return .015F;
        return 0;
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
