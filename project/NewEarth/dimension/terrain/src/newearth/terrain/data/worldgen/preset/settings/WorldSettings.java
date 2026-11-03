/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.data.worldgen.preset.settings;

import newearth.terrain.world.worldgen.cell.continent.MushroomIslandPopulator;
import newearth.terrain.world.worldgen.noise.function.DistanceFunction;

public class WorldSettings {

    public Continent continent;
    public ControlPoints controlPoints;
    public Properties properties;
    
    public WorldSettings(Continent continent, ControlPoints controlPoints, Properties properties) {
        this.continent = continent;
        this.controlPoints = controlPoints;
        this.properties = properties;
    }
    
    public WorldSettings copy() {
    	return new WorldSettings(this.continent.copy(), this.controlPoints.copy(), this.properties.copy());
    }
    
    public static class Continent {

        public ContinentType continentType;
        public DistanceFunction continentShape;
        public int continentScale;
        public float continentJitter;
        public float continentSkipping;
        public float continentSizeVariance;
        public int continentNoiseOctaves;
        public float continentNoiseGain;
        public float continentNoiseLacunarity;
        
        public Continent(ContinentType continentType, DistanceFunction continentShape, int continentScale, float continentJitter, float continentSkipping, float continentSizeVariance, int continentNoiseOctaves, float continentNoiseGain, float continentNoiseLacunarity) {
            this.continentType = continentType;
            this.continentShape = continentShape;
            this.continentScale = continentScale;
            this.continentJitter = continentJitter;
            this.continentSkipping = continentSkipping;
            this.continentSizeVariance = continentSizeVariance;
            this.continentNoiseOctaves = continentNoiseOctaves;
            this.continentNoiseGain = continentNoiseGain;
            this.continentNoiseLacunarity = continentNoiseLacunarity;
        }
        
        public Continent copy() {
        	return new Continent(this.continentType, this.continentShape, this.continentScale, this.continentJitter, this.continentSkipping, this.continentSizeVariance, this.continentNoiseOctaves, this.continentNoiseGain, this.continentNoiseLacunarity);
        }
    }
    
    public static class ControlPoints {

    	public float mushroomFieldsInland;
    	public float mushroomFieldsCoast;
        public float deepOcean;
        public float shallowOcean;
        public float beach;
        public float coast;
        public float inland;
        
        public ControlPoints(float mushroomFieldsInland, float mushroomFieldsCoast, float deepOcean, float shallowOcean, float beach, float coast, float inland) {
        	this.mushroomFieldsInland = mushroomFieldsInland;
        	this.mushroomFieldsCoast = mushroomFieldsCoast;
            this.deepOcean = deepOcean;
            this.shallowOcean = shallowOcean;
            this.beach = beach;
            this.coast = coast;
            this.inland = inland;
        }
        
        public ControlPoints copy() {
        	return new ControlPoints(this.mushroomFieldsInland, this.mushroomFieldsCoast, this.deepOcean, this.shallowOcean, this.beach, this.coast, this.inland);
        }
    }
    
    public static class Properties {

        public SpawnType spawnType;
        public int worldHeight;
        public int worldDepth;
        public int seaLevel;
        public int lavaLevel;
        
        public Properties(SpawnType spawnType, int worldHeight, int worldDepth, int seaLevel, int lavaLevel) {
        	this.spawnType = spawnType;
        	this.worldHeight = worldHeight;
        	this.worldDepth = worldDepth;
        	this.seaLevel = seaLevel;
        	this.lavaLevel = lavaLevel;
        }
        
        public Properties copy() {
        	return new Properties(this.spawnType, this.worldHeight, this.worldDepth, this.seaLevel, this.lavaLevel);
        }
        
        @Deprecated
        public int terrainScaler() {
        	return Math.min(this.worldHeight, 256);
        }
    }
}
