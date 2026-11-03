/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.NoiseUtil;	

public record LegacyTemperature(float frequency, int power) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		z *= this.frequency;
		float sin = NoiseUtil.sin(z);
		sin = NoiseUtil.clamp(sin, -1.0F, 1.0F);
		float value = NoiseUtil.pow(sin, this.power);
		value = NoiseUtil.copySign(value, sin);
		return NoiseUtil.map(value, -1.0F, 1.0F, 2.0F);
	}

	@Override
	public float minValue() {
		return 0.0F;
	}

	@Override
	public float maxValue() {
		return 1.0F;
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(this);
	}

}
