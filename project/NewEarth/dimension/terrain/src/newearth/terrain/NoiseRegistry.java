/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain;
import java.util.HashMap;
import newearth.terrain.world.worldgen.noise.module.Noise;
public final class NoiseRegistry {
 private final HashMap<String,Noise> entries=new HashMap<>();
 public Noise register(String key,Noise value){entries.put(key,value);return value;}
 public Noise getOrThrow(String key){Noise v=entries.get(key);if(v==null)throw new IllegalArgumentException(key);return v;}
}