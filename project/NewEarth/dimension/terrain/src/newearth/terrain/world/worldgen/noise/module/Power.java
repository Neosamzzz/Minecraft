/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.NoiseUtil;

record Power(Noise input, float power) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		return NoiseUtil.pow(this.input.compute(x, z, seed), this.power);
	}

	@Override
	public float minValue() {
		return this.input.minValue();
	}

	@Override
	public float maxValue() {
		return this.input.maxValue();
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new Power(this.input.mapAll(visitor), this.power));
	}

}
