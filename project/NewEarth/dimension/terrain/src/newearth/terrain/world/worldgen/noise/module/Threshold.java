/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

public record Threshold(Noise input, Noise lower, Noise upper, Noise threshold) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		if(this.input.compute(x, z, seed) > this.threshold.compute(x, z, seed)) {
			return this.upper.compute(x, z, seed);
		} else {
			return this.lower.compute(x, z, seed);
		}
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
		return visitor.apply(new Threshold(this.input.mapAll(visitor), this.lower.mapAll(visitor), this.upper.mapAll(visitor), this.threshold.mapAll(visitor)));
	}

}
