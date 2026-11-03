/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.domain;

import newearth.terrain.world.worldgen.noise.NoiseUtil;
import newearth.terrain.world.worldgen.noise.module.Noise;
import newearth.terrain.world.worldgen.noise.module.Noise.Visitor;

record DirectionWarp(Noise direction, Noise strength) implements Domain {

	@Override
	public float getOffsetX(float x, float z, int seed) {
        float angle = this.direction.compute(x, z, seed) * 6.2831855F;
        return NoiseUtil.sin(angle) * this.strength.compute(x, z, seed);
	}

	@Override
	public float getOffsetZ(float x, float z, int seed) {
        float angle = this.direction.compute(x, z, seed) * 6.2831855F;
        return NoiseUtil.cos(angle) * this.strength.compute(x, z, seed);
	}

	@Override
	public Domain mapAll(Visitor visitor) {
		return new DirectionWarp(this.direction.mapAll(visitor), this.strength.mapAll(visitor));
	}

}
