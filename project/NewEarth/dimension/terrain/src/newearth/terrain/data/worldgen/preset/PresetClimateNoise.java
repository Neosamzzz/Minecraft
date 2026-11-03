/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.data.worldgen.preset;

import newearth.terrain.data.worldgen.preset.settings.ClimateSettings;
import newearth.terrain.data.worldgen.preset.settings.Preset;
import newearth.terrain.data.worldgen.preset.settings.WorldSettings;
import newearth.terrain.world.worldgen.noise.module.Noise;

public class PresetClimateNoise {
	public static final String BIOME_EDGE_SHAPE = createKey("biome_edge_shape");
	
	public static void bootstrap(Preset preset, newearth.terrain.NoiseRegistry ctx) {
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		
		ClimateSettings climateSettings = preset.climate();
		ClimateSettings.BiomeNoise biomeEdgeShape = climateSettings.biomeEdgeShape;
		
		ctx.register(BIOME_EDGE_SHAPE, biomeEdgeShape.build(0));
	}

	private static String createKey(String name) {
		return PresetNoiseData.createKey("climate/" + name);
	}
}
