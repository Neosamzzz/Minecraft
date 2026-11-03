/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell;

public interface CellPopulator {
    void apply(Cell cell, float x, float z);
}
