/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.continent.simple;

import newearth.terrain.world.worldgen.GeneratorContext;
import newearth.terrain.world.worldgen.cell.Cell;
import newearth.terrain.world.worldgen.noise.NoiseUtil.Vec2i;
import newearth.terrain.world.worldgen.util.PosUtil;
import newearth.terrain.world.worldgen.util.Seed;

public class SingleContinentGenerator extends ContinentGenerator {
    private Vec2i center;
    
    public SingleContinentGenerator(Seed seed, GeneratorContext context) {
        super(seed, context);
        long center = this.getNearestCenter(0.0F, 0.0F);
        int cx = PosUtil.unpackLeft(center);
        int cz = PosUtil.unpackRight(center);
        this.center = new Vec2i(cx, cz);
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        super.apply(cell, x, y);
        if (cell.continentX != this.center.x() || cell.continentZ != this.center.y()) {
            cell.continentId = 0.0F;
            cell.continentEdge = 0.0F;
            cell.continentX = 0;
            cell.continentZ = 0;
        }
    }
}
