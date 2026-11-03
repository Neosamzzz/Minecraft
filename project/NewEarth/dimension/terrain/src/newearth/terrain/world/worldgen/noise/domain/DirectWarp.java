/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.domain;

import newearth.terrain.world.worldgen.noise.module.Noise.Visitor;

public record DirectWarp() implements Domain {

	@Override
	public float getOffsetX(float x, float z, int seed) {
		return 0.0F;
	}

	@Override
	public float getOffsetZ(float x, float z, int seed) {
		return 0.0F;
	}

	@Override
	public Domain mapAll(Visitor visitor) {
		return this;
	}

}
