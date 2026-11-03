/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.rivermap;

public interface RiverGenerator {
	Rivermap generateRivers(int x, int z, long id);
}
