/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen;

import newearth.terrain.data.worldgen.preset.settings.Preset;
import newearth.terrain.world.worldgen.cell.heightmap.Heightmap;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;

import newearth.terrain.world.worldgen.densityfunction.tile.generation.TileGenerator;
import newearth.terrain.world.worldgen.util.Seed;

public class GeneratorContext {
    public Seed seed;
    public Levels levels;
    public Preset preset;
    public newearth.terrain.NoiseRegistry noiseLookup;
    public TileGenerator generator;

    public GeneratorContext(Preset preset, newearth.terrain.NoiseRegistry noiseLookup, int seed, int tileSize, int tileBorder, int batchCount) {
        this.preset = preset;
        this.noiseLookup = noiseLookup;
        this.seed = new Seed(seed);
        this.levels = new Levels(preset.world().properties.terrainScaler(), preset.world().properties.seaLevel);
        this.generator = new TileGenerator(Heightmap.make(this), new WorldFilters(this), tileSize, tileBorder, batchCount);

    }

    public static GeneratorContext makeUncached(Preset preset, newearth.terrain.NoiseRegistry noiseLookup, int seed, int tileSize, int tileBorder, int batchCount) {
    	return new GeneratorContext(preset, noiseLookup, seed, tileSize, tileBorder, batchCount);
    }
}
