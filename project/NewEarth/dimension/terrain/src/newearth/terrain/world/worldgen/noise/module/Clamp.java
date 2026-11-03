/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.NoiseUtil;

record Clamp(Noise input, Noise min, Noise max) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		return NoiseUtil.clamp(this.input.compute(x, z, seed), this.min.compute(x, z, seed), this.max.compute(x, z, seed));
	}

	@Override
	public float minValue() {
		return this.min.minValue();
	}

	@Override
	public float maxValue() {
		return this.max.maxValue();
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new Clamp(this.input.mapAll(visitor), this.min.mapAll(visitor), this.max.mapAll(visitor)));
	}

}
