/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.function;

public enum DistanceFunction {
    EUCLIDEAN("EUCLIDEAN") {
        @Override
        public float apply(float x, float y) {
            return x * x + y * y;
        }
    }, 
    MANHATTAN("MANHATTAN") {
        @Override
        public float apply(float x, float y) {
            return Math.abs(x) + Math.abs(y);
        }
    }, 
    NATURAL("NATURAL") {
        @Override
        public float apply(float x, float y) {
            return Math.abs(x) + Math.abs(y) + (x * x + y * y);
        }
    };

	private String name;
	
	private DistanceFunction(String name) {
		this.name = name;
	}
    
	public String getSerializedName() {
		return this.name;
	}
	
    public abstract float apply(float x, float z);
}
