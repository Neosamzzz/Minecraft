/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.function;

import newearth.terrain.world.worldgen.noise.NoiseUtil;

record SCurveFunction(float lower, float upper) implements CurveFunction {

	public SCurveFunction {
		upper = upper < 0.0F ? Math.max(-lower, upper) : upper;
	}

	@Override
	public float apply(float f) {
        return NoiseUtil.pow(f, this.lower + this.upper * f);
	}

}
