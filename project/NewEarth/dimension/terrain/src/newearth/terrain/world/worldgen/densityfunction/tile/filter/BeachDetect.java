/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.densityfunction.tile.filter;

import newearth.terrain.data.worldgen.preset.settings.WorldSettings;
import newearth.terrain.world.worldgen.GeneratorContext;
import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;
import newearth.terrain.world.worldgen.cell.terrain.TerrainType;
import newearth.terrain.world.worldgen.densityfunction.tile.Size;

public record BeachDetect(Levels levels, WorldSettings.ControlPoints transition) implements Filter
{
    @Override
    public void apply(Filterable map, int seedX, int seedZ, int iterations) {
        Size size = map.getBlockSize();
        int total = size.total();
        for (int x = 0; x < total; ++x) {
            for (int z = 0; z < total; ++z) {
                float gz;
                Cell w;
                Cell cell = map.getCellRaw(x, z);
                if (!cell.terrain.isCoast() || cell.terrain.isWetland() || !(cell.continentEdge < this.transition.beach)) continue;
                Cell n = map.getCellRaw(x, z - 8);
                Cell s = map.getCellRaw(x, z + 8);
                Cell e = map.getCellRaw(x + 8, z);
                float gx = this.grad(e, w = map.getCellRaw(x - 8, z), cell);
                float d2 = gx * gx + (gz = this.grad(n, s, cell)) * gz;
                if (!(d2 < 0.275f)) continue;
                map.getCellRaw((int)x, (int)z).terrain = TerrainType.BEACH;
            }
        }
    }

    private float grad(Cell a, Cell b, Cell def) {
        int distance = 17;
        if (a.isAbsent()) {
            a = def;
            distance -= 8;
        }
        if (b.isAbsent()) {
            b = def;
            distance -= 8;
        }
        return (a.height - b.height) / (float)distance;
    }

    public static BeachDetect make(GeneratorContext ctx) {
        Levels levels = ctx.levels;
        WorldSettings.ControlPoints transition = ctx.preset.world().controlPoints;
        return new BeachDetect(levels, transition);
    }
}

