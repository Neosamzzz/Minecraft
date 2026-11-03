/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

public interface Noise {

	float compute(float x, float z, int seed);
	
	float minValue();
	
	float maxValue();
	
	Noise mapAll(Visitor visitor);

	public interface Visitor {
		Noise apply(Noise input);
	}
}
