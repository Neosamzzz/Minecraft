/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

public record Max(Noise input1, Noise input2) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		return Math.max(this.input1.compute(x, z, seed), this.input2.compute(x, z, seed));
	}

	@Override
	public float minValue() {
		return Math.max(this.input1.minValue(), this.input2.minValue());
	}

	@Override
	public float maxValue() {
		return Math.max(this.input1.maxValue(), this.input2.maxValue());
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new Max(this.input1.mapAll(visitor), this.input2.mapAll(visitor)));
	}

}
