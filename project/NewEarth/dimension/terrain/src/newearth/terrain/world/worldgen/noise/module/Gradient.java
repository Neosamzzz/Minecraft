/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.NoiseUtil;

record Gradient(Noise input, Noise lower, Noise upper, Noise strength) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		float noiseValue = this.input.compute(x, z, seed);
        float upperBound = this.upper.compute(x, z, seed);
        if (noiseValue > upperBound) {
            return noiseValue;
        }
        float amount = this.strength.compute(x, z, seed);
        float lowerBound = this.lower.compute(x, z, seed);
        if (noiseValue < lowerBound) {
            return NoiseUtil.pow(noiseValue, 1.0F - amount);
        }
        float alpha = 1.0F - (noiseValue - lowerBound) / (upperBound - lowerBound);
        float power = 1.0F - amount * alpha;
        return NoiseUtil.pow(noiseValue, power);
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
		return visitor.apply(new Gradient(this.input.mapAll(visitor), this.lower.mapAll(visitor), this.upper.mapAll(visitor), this.strength.mapAll(visitor)));
	}

}
