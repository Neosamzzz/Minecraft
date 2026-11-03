/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.continent;

public interface SimpleContinent extends Continent {
	float getEdgeValue(float x, float z);

	default float getDistanceToEdge(int cx, int cz, float dx, float dy) {
		return 1.0F;
	}

	default float getDistanceToOcean(int cx, int cz, float dx, float dy) {
		return 1.0F;
	}
}
