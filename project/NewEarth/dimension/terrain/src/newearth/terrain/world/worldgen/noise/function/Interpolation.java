/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.function;

import newearth.terrain.world.worldgen.noise.NoiseUtil;

public enum Interpolation implements CurveFunction {
    LINEAR("LINEAR") {
    	
        @Override
        public float apply(float f) {
            return f;
        }
    }, 
    CURVE3("CURVE3") {
    	
        @Override
        public float apply(float f) {
        	return NoiseUtil.interpHermite(f);
        }
    }, 
    CURVE4("CURVE4") {
    	
        @Override
        public float apply(float f) {
        	return NoiseUtil.interpQuintic(f);
        }
    };

	private String name;
	
	private Interpolation(String name) {
		this.name = name;
	}

	public String getSerializedName() {
		return this.name;
	}

}
