/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.densityfunction.tile.filter;

import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;

public record Steepness(int radius, float scaler, float waterLevel) implements Filter,
Filter.Visitor
{
    @Override
    public void apply(Filterable cellMap, int seedX, int seedZ, int iterations) {
        this.iterate(cellMap, this);
    }

    @Override
    public void visit(Filterable cellMap, Cell cell, int cx, int cz) {
        float totalHeightDif = 0.0f;
        for (int dz = -1; dz <= 2; ++dz) {
            for (int dx = -1; dx <= 2; ++dx) {
                int z;
                int x;
                Cell neighbour;
                if (dx == 0 && dz == 0 || (neighbour = cellMap.getCellRaw(x = cx + dx * this.radius, z = cz + dz * this.radius)).isAbsent()) continue;
                float height = Math.max(neighbour.height, this.waterLevel);
                totalHeightDif += Math.abs(cell.height - height) / (float)this.radius;
            }
        }
        cell.gradient = totalHeightDif * this.scaler;
    }

    public static Steepness make(int radius, float scaler, Levels levels) {
        return new Steepness(radius, scaler, levels.water);
    }
}

