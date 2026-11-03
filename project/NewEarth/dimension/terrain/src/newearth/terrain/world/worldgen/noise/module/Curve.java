/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.noise.function.CurveFunction;

record Curve(Noise input, CurveFunction curveFunction) implements Noise {

	@Override
	public float compute(float x, float z, int seed) {
		return this.curveFunction.apply(this.input.compute(x, z, seed));
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
		return visitor.apply(new Curve(this.input.mapAll(visitor), this.curveFunction));
	}

}
