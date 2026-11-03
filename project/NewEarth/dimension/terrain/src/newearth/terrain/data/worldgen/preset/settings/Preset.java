/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.data.worldgen.preset.settings;
public final class Preset {
 private WorldSettings world;private ClimateSettings climate;private TerrainSettings terrain;
 private RiverSettings rivers;private FilterSettings filters;private MiscellaneousSettings miscellaneous;
 public WorldSettings world(){return world;}public ClimateSettings climate(){return climate;}
 public TerrainSettings terrain(){return terrain;}public RiverSettings rivers(){return rivers;}
 public FilterSettings filters(){return filters;}public MiscellaneousSettings miscellaneous(){return miscellaneous;}
}