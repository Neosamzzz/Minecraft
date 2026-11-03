/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell;

public interface CellLookup {
	Cell lookup(int blockX, int blockZ);
}
