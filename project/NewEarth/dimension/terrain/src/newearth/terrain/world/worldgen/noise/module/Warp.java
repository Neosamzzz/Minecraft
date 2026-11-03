/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.domain.Domain;

record Warp(Noise input, Domain domain) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		return this.input.compute(this.domain.getX(x, z, seed), this.domain.getZ(x, z, seed), seed);
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
		return visitor.apply(new Warp(this.input.mapAll(visitor), this.domain.mapAll(visitor)));
	}

}
