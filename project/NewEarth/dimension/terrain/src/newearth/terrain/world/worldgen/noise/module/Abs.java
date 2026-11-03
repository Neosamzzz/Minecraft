/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

public record Abs(Noise input) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		return Math.abs(this.input.compute(x, z, seed));
	}

	@Override
	public float minValue() {
		return Math.abs(this.input.minValue());
	}

	@Override
	public float maxValue() {
		return Math.abs(this.input.maxValue());
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(this.input.mapAll(visitor));
	}

}
