/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

record ShiftSeed(Noise input, int shift) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		return this.input.compute(x, z, seed + this.shift);
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new ShiftSeed(this.input.mapAll(visitor), this.shift));
	}

	@Override
	public float minValue() {
		return this.input.minValue();
	}

	@Override
	public float maxValue() {
		return this.input.maxValue();
	}

}
