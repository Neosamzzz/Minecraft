/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.densityfunction.tile.filter;

import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;
import newearth.terrain.world.worldgen.cell.terrain.TerrainCategory;
import newearth.terrain.world.worldgen.cell.terrain.TerrainType;
import newearth.terrain.world.worldgen.densityfunction.tile.Size;

public record NoiseCorrection(Levels levels) implements Filter
{
    @Override
    public void apply(Filterable map, int seedX, int seedZ, int iterations) {
        Size size = map.getBlockSize();
        int total = size.total();
        int quartTotal = (total >> 2);
        for (int quartX = 0; quartX < quartTotal; ++quartX) {
            for (int quartZ = 0; quartZ < quartTotal; ++quartZ) {
                int z;
                int x;
                int startX = (quartX << 2);
                int startZ = (quartZ << 2);
                int endX = ((quartX + 1) << 2);
                int endZ = ((quartZ + 1) << 2);
                boolean isBeach = false;
                block2: for (x = startX; x < endX; ++x) {
                    for (z = startZ; z < endZ; ++z) {
                        Cell cell = map.getCellRaw(x, z);
                        if (cell.terrain.getDelegate() != TerrainCategory.BEACH && (!cell.terrain.isShallowOcean() && !cell.terrain.isDeepOcean() || !(cell.height > this.levels.water))) continue;
                        isBeach = true;
                        break block2;
                    }
                }
                if (!isBeach) continue;
                for (x = startX; x < endX; ++x) {
                    for (z = startZ; z < endZ; ++z) {
                        map.getCellRaw((int)x, (int)z).terrain = TerrainType.BEACH;
                    }
                }
            }
        }
    }
}

