/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.densityfunction.tile.filter;

import newearth.terrain.data.worldgen.preset.settings.FilterSettings;
import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;
import newearth.terrain.world.worldgen.noise.NoiseUtil;

public record Smoothing(float smoothingRadius, float smoothingRate, Modifier modifier) implements Filter
{
    @Override
    public void apply(Filterable map, int seedX, int seedZ, int iterations) {
        while (iterations-- > 0) {
            this.apply(map);
        }
    }

    private void apply(Filterable cellMap) {
        int radius = NoiseUtil.round(this.smoothingRadius + 0.5f);
        float radiusSq = this.smoothingRadius * this.smoothingRadius;
        int maxZ = cellMap.getBlockSize().total() - radius;
        int maxX = cellMap.getBlockSize().total() - radius;
        for (int z = radius; z < maxZ; ++z) {
            for (int x = radius; x < maxX; ++x) {
                Cell cell = cellMap.getCellRaw(x, z);
                if (cell.erosionMask) continue;
                float total = 0.0f;
                float weights = 0.0f;
                for (int dz = -radius; dz <= radius; ++dz) {
                    for (int dx = -radius; dx <= radius; ++dx) {
                        int pz;
                        int px;
                        Cell neighbour;
                        float dist2 = dx * dx + dz * dz;
                        if (!(dist2 <= radiusSq) || (neighbour = cellMap.getCellRaw(px = x + dx, pz = z + dz)).isAbsent()) continue;
                        float value = neighbour.height;
                        float weight = 1.0f - dist2 / radiusSq;
                        total += value * weight;
                        weights += weight;
                    }
                }
                if (!(weights > 0.0f)) continue;
                float dif = cell.height - total / weights;
                Cell cell2 = cell;
                cell2.height -= this.modifier.modify(cell, dif * this.smoothingRate);
            }
        }
    }

    public static Smoothing make(FilterSettings.Smoothing settings, Levels levels) {
        return new Smoothing(settings.smoothingRadius, settings.smoothingRate, Modifier.range(levels.ground(1), levels.ground(120)).invert());
    }
}

