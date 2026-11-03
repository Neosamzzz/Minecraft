/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.densityfunction.tile.filter;

import newearth.terrain.world.worldgen.cell.Cell;

public interface Filter {
    public void apply(Filterable var1, int var2, int var3, int var4);

    default public void iterate(Filterable map, Visitor visitor) {
        for (int dz = 0; dz < map.getBlockSize().total(); ++dz) {
            for (int dx = 0; dx < map.getBlockSize().total(); ++dx) {
                Cell cell = map.getCellRaw(dx, dz);
                visitor.visit(map, cell, dx, dz);
            }
        }
    }

    public static interface Visitor {
        public void visit(Filterable var1, Cell var2, int var3, int var4);
    }
}

