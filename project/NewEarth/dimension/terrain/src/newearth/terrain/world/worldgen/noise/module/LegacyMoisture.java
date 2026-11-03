/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.NoiseUtil;

public record LegacyMoisture(Noise source, int power) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
        float noise = this.source.compute(x, z, seed);
        if (this.power < 2) {
            return noise;
        }
        noise = (noise - 0.5F) * 2.0F;
        float value = NoiseUtil.pow(noise, this.power);
        value = NoiseUtil.copySign(value, noise);
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
		return visitor.apply(new LegacyMoisture(this.source.mapAll(visitor), this.power));
	}

}
