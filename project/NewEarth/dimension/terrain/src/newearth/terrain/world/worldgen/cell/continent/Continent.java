/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.continent;

import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.cell.CellPopulator;
import newearth.terrain.world.worldgen.cell.rivermap.Rivermap;

public interface Continent extends CellPopulator {
    default void close() {}
    float getEdgeValue(float x, float z);
    
    default float getLandValue(float x, float z) {
        return this.getEdgeValue(x, z);
    }
    
    long getNearestCenter(float x, float z);
    
    Rivermap getRivermap(int x, int z);
    
    default Rivermap getRivermap(Cell cell) {
        return this.getRivermap(cell.continentX, cell.continentZ);
    }
}
