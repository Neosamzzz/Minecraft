/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.densityfunction.tile.filter;

import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.densityfunction.tile.Size;

public interface Filterable {
    public int getBlockX();

    public int getBlockZ();

    public Size getBlockSize();

    public Cell[] getBacking();

    public Cell getCellRaw(int var1, int var2);
}

