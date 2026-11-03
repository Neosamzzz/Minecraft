/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.function;

public class CurveFunctions {

	public static CurveFunction scurve(float lower, float upper) {
		return new SCurveFunction(lower, upper);
	}

}
