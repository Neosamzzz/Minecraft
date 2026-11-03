/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.heightmap;

import newearth.terrain.data.worldgen.preset.settings.WorldSettings;

@Deprecated
public record ControlPoints(float mushroomFieldsInland, float mushroomFieldsCoast, float deepOcean, float shallowOcean, float beach, float coast, float coastMarker, float inland) {
    
    public static ControlPoints make(WorldSettings.ControlPoints points) {
        if (!validate(points)) {
            points = new WorldSettings.ControlPoints(0.0F,0.0F,0.1F,0.25F,0.327F,0.448F,0.502F);
        }
        return new ControlPoints(points.mushroomFieldsInland, points.mushroomFieldsCoast, points.deepOcean, points.shallowOcean, points.beach, points.coast, points.coast + (points.inland - points.coast) / 2.0F, points.inland);
   }
    
    public static boolean validate(WorldSettings.ControlPoints points) {
        return points.inland <= 1.0F && points.inland > points.coast && points.coast > points.beach && points.beach > points.shallowOcean && points.shallowOcean > points.deepOcean && points.mushroomFieldsCoast >= points.mushroomFieldsInland && points.mushroomFieldsInland >= 0.0F;
    }
}
