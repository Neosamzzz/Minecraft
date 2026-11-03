/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.data.worldgen.preset.settings;

public class TerrainSettings {

    public General general;
    public Terrain steppe;
    public Terrain plains;
    public Terrain hills;
    public Terrain dales;
    public Terrain plateau;
    public Terrain badlands;
    public Terrain torridonian;
    public Terrain mountains;
    public Terrain volcano;

    public TerrainSettings(General general, Terrain steppe, Terrain plains, Terrain hills, Terrain dales, Terrain plateau, Terrain badlands, Terrain torridonian, Terrain mountains, Terrain volcano) {
        this.general = general;
        this.steppe = steppe;
        this.plains = plains;
        this.hills = hills;
        this.dales = dales;
        this.plateau = plateau;
        this.badlands = badlands;
        this.torridonian = torridonian;
        this.mountains = mountains;
        this.volcano = volcano;
    }

    public TerrainSettings copy() {
        return new TerrainSettings(this.general.copy(), this.steppe.copy(), this.plains.copy(), this.hills.copy(), this.dales.copy(), this.plateau.copy(), this.badlands.copy(), this.torridonian.copy(), this.mountains.copy(), this.volcano.copy());
    }

    public static class General {

        public int terrainSeedOffset;
        public int terrainRegionSize;
        public float globalVerticalScale;
        public float globalHorizontalScale;
        public boolean fancyMountains;
        public boolean legacyMountainScaling;

        public General(int terrainSeedOffset, int terrainRegionSize, float globalVerticalScale, float globalHorizontalScale, boolean fancyMountains, boolean legacyMountainScaling) {
            this.terrainSeedOffset = terrainSeedOffset;
            this.terrainRegionSize = terrainRegionSize;
            this.globalVerticalScale = globalVerticalScale;
            this.globalHorizontalScale = globalHorizontalScale;
            this.fancyMountains = fancyMountains;
            this.legacyMountainScaling = legacyMountainScaling;
        }

        public General copy() {
            return new General(this.terrainSeedOffset, this.terrainRegionSize, this.globalVerticalScale, this.globalHorizontalScale, this.fancyMountains, this.legacyMountainScaling);
        }
    }

    public static class Terrain {

        public float weight;
        public float baseScale;
        public float verticalScale;
        public float horizontalScale;

        public Terrain(float weight, float baseScale, float verticalScale, float horizontalScale) {
            this.weight = weight;
            this.baseScale = baseScale;
            this.verticalScale = verticalScale;
            this.horizontalScale = horizontalScale;
        }

        public Terrain copy() {
            return new Terrain(this.weight, this.baseScale, this.verticalScale, this.horizontalScale);
        }
    }
}

