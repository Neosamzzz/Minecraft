/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.data.worldgen.preset;
import newearth.terrain.NoiseRegistry;
import newearth.terrain.world.worldgen.noise.module.Noise;
import newearth.terrain.data.worldgen.preset.settings.Preset;
public final class PresetNoiseData {
 public static Noise getNoise(NoiseRegistry registry,String key){return registry.getOrThrow(key);}
 public static String createKey(String name){return name;}
 public static void bootstrap(Preset preset,NoiseRegistry registry){
  PresetTerrainTypeNoise.bootstrap(preset,registry);PresetClimateNoise.bootstrap(preset,registry);
 }
}
