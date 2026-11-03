/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.rivermap.wetland;

import newearth.terrain.data.worldgen.preset.settings.RiverSettings;
import newearth.terrain.world.worldgen.noise.NoiseUtil;
import newearth.terrain.world.worldgen.util.Variance;

public class WetlandConfig {
    public int skipSize;
    public Variance length;
    public Variance width;
    
    public WetlandConfig(RiverSettings.Wetland settings) {
        this.skipSize = Math.max(1, NoiseUtil.round((1.0F - settings.chance) * 10.0F));
        this.length = Variance.of(settings.sizeMin, settings.sizeMax);
        this.width = Variance.of(50.0F, 150.0F);
    }
}
