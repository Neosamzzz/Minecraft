/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.climate;

import newearth.terrain.data.worldgen.preset.PresetClimateNoise;
import newearth.terrain.data.worldgen.preset.PresetNoiseData;
import newearth.terrain.data.worldgen.preset.settings.ClimateSettings;
import newearth.terrain.data.worldgen.preset.settings.Preset;
import newearth.terrain.data.worldgen.preset.settings.WorldSettings;
import newearth.terrain.world.worldgen.GeneratorContext;
import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.cell.continent.Continent;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;
import newearth.terrain.world.worldgen.cell.terrain.TerrainType;
import newearth.terrain.world.worldgen.noise.NoiseUtil;
import newearth.terrain.world.worldgen.noise.module.Noise;
import newearth.terrain.world.worldgen.noise.module.Noises;

public record Climate(int randomSeed, Noise offsetX, Noise offsetZ, int offsetDistance, Levels levels, ClimateModule biomeNoise) {

	public void apply(Cell cell, float x, float z, boolean applyClimate) {
		if(applyClimate)
			this.biomeNoise.apply(cell, x, z, x, z, true);
		float edgeBlend = 0.4F;
		if (cell.height <= this.levels.water) {
			if (cell.terrain == TerrainType.COAST) {
				cell.terrain = TerrainType.SHALLOW_OCEAN;
			}
		} else if (applyClimate && (cell.biomeRegionEdge < edgeBlend || cell.terrain == TerrainType.MOUNTAIN_CHAIN)) {
			float modifier = 1.0F - NoiseUtil.map(cell.biomeRegionEdge, 0.0F, edgeBlend, edgeBlend);
			float distance = this.offsetDistance * modifier;
			float dx = this.offsetX.compute(x, z, 0) * distance;
			float dz = this.offsetZ.compute(x, z, 0) * distance;
			float ox = x;
			float oz = z;
			x += dx;
			z += dz;
			this.biomeNoise.apply(cell, x, z, ox, oz, false);
		}
	}
	
	public static Climate make(Continent continent, GeneratorContext context) {
		newearth.terrain.NoiseRegistry noiseLookup = context.noiseLookup;
		
		Preset preset = context.preset;
		
		WorldSettings worldSettings = preset.world();
		ClimateSettings climateSettings = preset.climate();
		
		ClimateModule biomeNoise = new ClimateModule(context.seed, continent, worldSettings.controlPoints, climateSettings, context.levels);
		Levels levels = context.levels;
		int randSeed = context.seed.next();
		
		Noise biomeEdgeShape = PresetNoiseData.getNoise(noiseLookup, PresetClimateNoise.BIOME_EDGE_SHAPE);
		Noise offsetX = Noises.shiftSeed(biomeEdgeShape, context.seed.next());
		Noise offsetZ = Noises.shiftSeed(biomeEdgeShape, context.seed.next());
		int offsetDistance = climateSettings.biomeEdgeShape.strength;
		return new Climate(randSeed, offsetX, offsetZ, offsetDistance, levels, biomeNoise);
	}
}
