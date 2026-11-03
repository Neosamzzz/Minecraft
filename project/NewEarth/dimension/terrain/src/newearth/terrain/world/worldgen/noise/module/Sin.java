/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.NoiseUtil;

record Sin(float frequency, Noise alpha) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
        float a = this.alpha.compute(x, z, seed);
        x *= this.frequency;
        z *= this.frequency;
        float noise;
        if (a == 0.0F) {
            noise = NoiseUtil.sin(x);
        } else if (a == 1.0F) {
            noise = NoiseUtil.sin(z);
        } else {
            float sx = NoiseUtil.sin(x);
            float sy = NoiseUtil.sin(z);
            noise = NoiseUtil.lerp(sx, sy, a);
        }
        return NoiseUtil.map(noise, -1.0F, 1.0F, 2.0F);
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
		return visitor.apply(new Sin(this.frequency, this.alpha.mapAll(visitor)));
	}

}
