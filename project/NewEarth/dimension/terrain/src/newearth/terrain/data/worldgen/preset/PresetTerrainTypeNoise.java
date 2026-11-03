/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.data.worldgen.preset;

import newearth.terrain.data.worldgen.preset.settings.Preset;
import newearth.terrain.data.worldgen.preset.settings.WorldSettings;
import newearth.terrain.world.worldgen.noise.module.Noise;
import newearth.terrain.world.worldgen.noise.module.Noises;

public class PresetTerrainTypeNoise {
	public static final String GROUND = PresetNoiseData.createKey("terrain/ground");
	
	public static void bootstrap(Preset preset, newearth.terrain.NoiseRegistry ctx) {
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		float seaLevel = properties.seaLevel;
		int terrainScaler = properties.terrainScaler();

		ctx.register(GROUND, Noises.constant(seaLevel / (float)terrainScaler));
	}
}
