/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.terrain;

import newearth.terrain.data.worldgen.preset.settings.TerrainSettings;
import newearth.terrain.world.worldgen.biome.Erosion;
import newearth.terrain.world.worldgen.biome.Weirdness;
import newearth.terrain.world.worldgen.cell.CellPopulator;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;
import newearth.terrain.world.worldgen.cell.terrain.populator.OceanPopulator;
import newearth.terrain.world.worldgen.cell.terrain.populator.TerrainPopulator;
import newearth.terrain.world.worldgen.noise.domain.Domain;
import newearth.terrain.world.worldgen.noise.domain.Domains;
import newearth.terrain.world.worldgen.noise.function.CurveFunction;
import newearth.terrain.world.worldgen.noise.function.DistanceFunction;
import newearth.terrain.world.worldgen.noise.function.EdgeFunction;
import newearth.terrain.world.worldgen.noise.function.Interpolation;

import newearth.terrain.world.worldgen.noise.module.Noise;
import newearth.terrain.world.worldgen.noise.module.Noises;
import newearth.terrain.world.worldgen.util.Seed;

public class Populators {
    @Deprecated
    public static final Noise DEFAULT_EROSION = Erosion.LEVEL_4.source();
    @Deprecated
    public static final Noise DEFAULT_WEIRDNESS = Weirdness.MID_SLICE_NORMAL_DESCENDING.source();
    private static final int MOUNTAINS_H = 610;
    private static final float MOUNTAINS_V = 1.3f;
    private static final int MOUNTAINS3_H = 600;
    private static final float MOUNTAINS3_V = 1.185f;

    public static CellPopulator makeDeepOcean(@Deprecated int seed, float seaLevel) {
        Noise hills = Noises.perlin(++seed, 150, 3);
        hills = Noises.mul(hills, seaLevel * 0.7f);
        Noise hillBias = Noises.perlin(++seed, 200, 1);
        hillBias = Noises.mul(hillBias, seaLevel * 0.2f);
        hills = Noises.add(hills, hillBias);
        Noise canyons = Noises.perlin(++seed, 150, 4);
        canyons = Noises.powCurve(canyons, 0.2f);
        canyons = Noises.invert(canyons);
        canyons = Noises.mul(canyons, seaLevel * 0.7f);
        Noise canyonBias = Noises.perlin(++seed, 170, 1);
        canyonBias = Noises.mul(canyonBias, seaLevel * 0.15f);
        canyons = Noises.add(canyons, canyonBias);
        Noise selector = Noises.perlin(++seed, 500, 1);
        Noise height = Noises.blend(selector, hills, canyons, 0.6f, 0.65f);
        height = Noises.warpPerlin(height, ++seed, 50, 2, 50.0f);
        return new OceanPopulator(TerrainType.DEEP_OCEAN, height);
    }

    public static CellPopulator makeShallowOcean(Levels levels) {
        return new OceanPopulator(TerrainType.SHALLOW_OCEAN, Noises.constant(levels.water(-7)));
    }

    public static CellPopulator makeCoast(Levels levels) {
        return new OceanPopulator(TerrainType.COAST, Noises.constant(levels.water));
    }

    public static TerrainPopulator makeSteppe(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings) {
        int scaleH = Math.round(250.0f * settings.horizontalScale);
        Noise erosion = Noises.perlin(seed.next(), scaleH * 2, 3, 3.75f);
        erosion = Noises.alpha(erosion, 0.45f);
        Noise warpX = Noises.perlin(seed.next(), scaleH / 4, 3, 3.0f);
        Noise warpZ = Noises.perlin(seed.next(), scaleH / 4, 3, 3.0f);
        Noise height = Noises.perlin(seed.next(), scaleH, 1);
        height = Noises.mul(height, erosion);
        height = Noises.warp(height, warpX, warpZ, (float)scaleH / 4.0f);
        height = Noises.warpPerlin(height, seed.next(), 256, 1, 200.0f);
        height = Noises.mul(height, 0.08f);
        height = Noises.add(height, -0.02f);
        return TerrainPopulator.make(TerrainType.FLATS, ground, height, DEFAULT_EROSION, DEFAULT_WEIRDNESS, settings);
    }

    private static TerrainPopulator makePlains(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain noiseSettings, TerrainSettings.Terrain scalingSettings, float verticalScale) {
        int scaleH = Math.round(250.0f * noiseSettings.horizontalScale);
        Noise erosion = Noises.perlin(seed.next(), scaleH * 2, 3, 3.75f);
        erosion = Noises.alpha(erosion, 0.45f);
        Noise warpX = Noises.perlin(seed.next(), scaleH / 4, 3, 3.5f);
        Noise warpZ = Noises.perlin(seed.next(), scaleH / 4, 3, 3.5f);
        Noise height = Noises.perlin(seed.next(), scaleH, 1);
        height = Noises.mul(height, erosion);
        height = Noises.warp(height, warpX, warpZ, (float)scaleH / 4.0f);
        height = Noises.warpPerlin(height, seed.next(), 256, 1, 256.0f);
        height = Noises.mul(height, 0.15f * verticalScale);
        height = Noises.add(height, -0.02f);
        return TerrainPopulator.make(TerrainType.FLATS, ground, height, DEFAULT_EROSION, DEFAULT_WEIRDNESS, scalingSettings);
    }

    public static TerrainPopulator makePlains(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float verticalScale) {
        return Populators.makePlains(seed, ground, settings, settings, verticalScale);
    }

    public static TerrainPopulator makePlateau(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float verticalScale) {
        Noise valley = Noises.perlinRidge(seed.next(), 500, 1);
        valley = Noises.invert(valley);
        valley = Noises.warpPerlin(valley, seed.next(), 100, 1, 150.0f);
        valley = Noises.warpPerlin(valley, seed.next(), 20, 1, 15.0f);
        Noise top = Noises.perlinRidge(seed.next(), 150, 3, 2.45f);
        top = Noises.warpPerlin(top, seed.next(), 300, 1, 150.0f);
        top = Noises.warpPerlin(top, seed.next(), 40, 2, 20.0f);
        top = Noises.mul(top, 0.15f);
        Noise valleyScaler = Noises.clamp(valley, 0.02f, 0.1f);
        valleyScaler = Noises.map(valleyScaler, 0.0f, 1.0f);
        top = Noises.mul(top, valleyScaler);
        Noise surface = Noises.perlin(seed.next(), 20, 3);
        surface = Noises.mul(surface, 0.05f);
        surface = Noises.warpPerlin(surface, seed.next(), 40, 2, 20.0f);
        Noise cubic = Noises.cubic(seed.next(), 500, 1);
        cubic = Noises.mul(cubic, 0.6f);
        cubic = Noises.add(cubic, 0.3f);
        Noise valleyBase = Noises.mul(valley, cubic);
        valleyBase = Noises.add(valleyBase, top);
        Noise height = Noises.terrace(valleyBase, 0.9f, 0.15f, 0.35f, 0.4f, 4);
        height = Noises.add(height, surface);
        height = Noises.mul(height, 0.475f * verticalScale);
        Noise weirdness = Noises.clamp(valleyBase, 0.0f, 0.415f);
        weirdness = Noises.map(weirdness, 0.0f, 1.0f);
        weirdness = Noises.map(weirdness, Weirdness.LOW_SLICE_NORMAL_DESCENDING.mid(), -0.42f);
        return TerrainPopulator.make(TerrainType.PLATEAU, ground, height, Erosion.LEVEL_3.source(), weirdness, settings);
    }

    public static TerrainPopulator makeHills1(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float verticalScale) {
        Noise height = Noises.perlin(seed.next(), 200, 3);
        Noise scaler = Noises.billow(seed.next(), 400, 3);
        scaler = Noises.alpha(scaler, 0.5f);
        height = Noises.mul(height, scaler);
        height = Noises.warpPerlin(height, seed.next(), 30, 3, 20.0f);
        height = Noises.warpPerlin(height, seed.next(), 400, 3, 200.0f);
        height = Noises.mul(height, 0.6f * verticalScale);
        return TerrainPopulator.make(TerrainType.HILLS, ground, height, DEFAULT_EROSION, DEFAULT_WEIRDNESS, settings);
    }

    public static TerrainPopulator makeHills2(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float verticalScale) {
        Noise height = Noises.cubic(seed.next(), 128, 2);
        Noise scaler1 = Noises.perlin(seed.next(), 32, 4);
        scaler1 = Noises.alpha(scaler1, 0.075f);
        height = Noises.mul(height, scaler1);
        height = Noises.warpPerlin(height, seed.next(), 30, 3, 20.0f);
        height = Noises.warpPerlin(height, seed.next(), 400, 3, 200.0f);
        Noise scaler2 = Noises.perlinRidge(seed.next(), 512, 2);
        scaler2 = Noises.alpha(scaler2, 0.8f);
        height = Noises.mul(height, scaler2);
        height = Noises.mul(height, 0.55f * verticalScale);
        return TerrainPopulator.make(TerrainType.HILLS, ground, height, DEFAULT_EROSION, DEFAULT_WEIRDNESS, settings);
    }

    public static TerrainPopulator makeDales(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings) {
        Noise hillsBlend;
        Noise hills1 = Noises.billow(seed.next(), 300, 4, 4.0f, 0.8f);
        hills1 = Noises.powCurve(hills1, 0.5f);
        hills1 = Noises.mul(hills1, 0.75f);
        Noise hills2 = Noises.billow(seed.next(), 350, 3, 4.0f, 0.8f);
        hills2 = Noises.pow(hills2, 1.25f);
        Noise selector = Noises.perlin(seed.next(), 400, 1);
        selector = Noises.clamp(selector, 0.3f, 0.6f);
        selector = Noises.map(selector, 0.0f, 1.0f);
        int warpSeed = seed.next();
        Noise height = hillsBlend = Noises.blend(selector, hills1, hills2, 0.4f, 0.75f);
        height = Noises.pow(height, 1.125f);
        height = Noises.warpPerlin(height, warpSeed, 300, 1, 100.0f);
        return TerrainPopulator.make(TerrainType.HILLS, ground, Noises.mul(height, 0.4f), Noises.threshold(selector, Erosion.LEVEL_2.mid(), Erosion.LEVEL_4.mid(), 0.5f), Noises.min(Noises.mul(height, -1.0f), Noises.constant(-0.06f)), settings);
    }

    public static TerrainPopulator makeBadlands(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings) {
        Noise mask = Noises.perlin(seed.next(), 270, 3);
        mask = Noises.clamp(mask, 0.35f, 0.65f);
        mask = Noises.map(mask, 0.0f, 1.0f);
        Noise hills = Noises.perlinRidge(seed.next(), 275, 4);
        hills = Noises.warpPerlin(hills, seed.next(), 400, 2, 100.0f);
        hills = Noises.warpPerlin(hills, seed.next(), 18, 1, 20.0f);
        hills = Noises.mul(hills, mask);
        float modulation = 0.4f;
        float alpha = 1.0f - modulation;
        Noise mod1 = Noises.warpPerlin(hills, seed.next(), 100, 1, 50.0f);
        mod1 = Noises.mul(mod1, modulation);
        Noise lowFreq = Noises.steps(hills, 4, 0.6f, 0.7f);
        lowFreq = Noises.mul(lowFreq, alpha);
        lowFreq = Noises.add(lowFreq, mod1);
        Noise highFreq = Noises.steps(hills, 10, 0.6f, 0.7f);
        highFreq = Noises.mul(highFreq, alpha);
        highFreq = Noises.add(highFreq, mod1);
        Noise detail = Noises.add(lowFreq, highFreq);
        detail = Noises.alpha(detail, 0.5f);
        Noise scaler = Noises.perlin(seed.next(), 200, 3);
        scaler = Noises.mul(scaler, modulation);
        Noise mod2 = Noises.mul(hills, scaler);
        Noise shape = Noises.steps(hills, 4, 0.65f, 0.75f, (CurveFunction)Interpolation.CURVE3);
        shape = Noises.mul(shape, alpha);
        shape = Noises.add(shape, mod2);
        shape = Noises.mul(shape, alpha);
        Noise height = Noises.mul(shape, detail);
        height = Noises.mul(height, 0.55f);
        height = Noises.add(height, 0.025f);
        return TerrainPopulator.make(TerrainType.BADLANDS, ground, height, DEFAULT_EROSION, DEFAULT_WEIRDNESS, settings);
    }

    public static TerrainPopulator makeTorridonian(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings) {
        Noise plains = Noises.perlin(seed.next(), 100, 3);
        plains = Noises.warpPerlin(plains, seed.next(), 300, 1, 150.0f);
        plains = Noises.warpPerlin(plains, seed.next(), 20, 1, 40.0f);
        plains = Noises.mul(plains, 0.15f);
        Noise hills = Noises.perlin(seed.next(), 150, 4);
        hills = Noises.warpPerlin(hills, seed.next(), 300, 1, 200.0f);
        hills = Noises.warpPerlin(hills, seed.next(), 20, 2, 20.0f);
        hills = Noises.boost(hills);
        Noise selector = Noises.perlin(seed.next(), 200, 3);
        Noise modulation = Noises.perlin(seed.next(), 120, 1);
        modulation = Noises.mul(modulation, 0.25f);
        Noise mask = Noises.perlin(seed.next(), 200, 1);
        mask = Noises.mul(mask, 0.5f);
        mask = Noises.add(mask, 0.5f);
        Noise slope = Noises.constant(0.5f);
        Noise blend = Noises.blend(selector, plains, hills, 0.6f, 0.6f);
        blend = Noises.advancedTerrace(blend, modulation, mask, slope, 0.0f, 0.3f, 6, 1);
        Noise height = Noises.boost(blend);
        height = Noises.mul(height, 0.5f);
        Noise weirdness = Noises.negative(blend);
        weirdness = Noises.min(weirdness, Noises.constant(Weirdness.LOW_SLICE_NORMAL_DESCENDING.max() - 0.01f));
        return TerrainPopulator.make(TerrainType.HILLS, ground, height, Erosion.LEVEL_5.source(), weirdness, settings);
    }

    private static TerrainPopulator makeMountains(Terrain terrainType, @Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float horizontalScale, float verticalScale, boolean makeFancy, boolean legacyScaling) {
        int scaleH = legacyScaling ? Math.round(410.0f * settings.horizontalScale) : Math.round(610.0f * settings.horizontalScale);
        Noise height = Noises.perlinRidge(seed.next(), scaleH, 4, 2.35f, 1.15f);
        Noise scaler = Noises.perlin(seed.next(), 24, 4);
        scaler = Noises.alpha(scaler, 0.075f);
        height = Noises.mul(height, scaler);
        height = Noises.warpPerlin(height, seed.next(), 350, 1, 150.0f);
        if (makeFancy) {
            height = Populators.makeFancy(seed, height);
        }
        height = Noises.cache2d(height);
        return TerrainPopulator.make(terrainType, ground, Noises.mul(height, (legacyScaling ? 0.7f : 1.3f) * verticalScale), Erosion.LEVEL_1.source(), Noises.min(Noises.mul(height, Noises.constant(-1.0f)), Noises.constant(-0.08f)), settings);
    }

    public static TerrainPopulator makeMountains(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float horizontalScale, float verticalScale, boolean makeFancy, boolean legacyScaling) {
        return Populators.makeMountains(TerrainType.MOUNTAINS_1, seed, ground, settings, horizontalScale, verticalScale, makeFancy, legacyScaling);
    }

    public static TerrainPopulator makeMountainChain(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float horizontalScale, float verticalScale, boolean makeFancy, boolean legacyScaling) {
        return Populators.makeMountains(TerrainType.MOUNTAIN_CHAIN, seed, ground, settings, legacyScaling ? horizontalScale : horizontalScale * 2.25f, verticalScale, makeFancy, legacyScaling);
    }

    public static TerrainPopulator makeMountains2(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float verticalScale, boolean makeFancy, boolean legacyScaling) {
        Noise cell = Noises.worleyEdge(seed.next(), legacyScaling ? 360 : Math.round(360.0f * settings.horizontalScale), EdgeFunction.DISTANCE_2, DistanceFunction.EUCLIDEAN);
        cell = Noises.mul(cell, 1.2f);
        cell = Noises.clamp(cell, 0.0f, 1.0f);
        cell = Noises.warpPerlin(cell, seed.next(), 200, 2, 100.0f);
        Noise blur = Noises.perlin(seed.next(), 10, 1);
        blur = Noises.alpha(blur, 0.025f);
        Noise surface = Noises.perlinRidge(seed.next(), 125, 4);
        surface = Noises.alpha(surface, 0.37f);
        Noise height = Noises.clamp(cell, 0.0f, 1.0f);
        height = Noises.mul(height, blur);
        height = Noises.mul(height, surface);
        height = Noises.pow(height, 1.1f);
        if (makeFancy) {
            height = Populators.makeFancy(seed, height);
        }
        height = Noises.cache2d(height);
        return TerrainPopulator.make(TerrainType.MOUNTAINS_2, ground, Noises.mul(height, 0.645f * verticalScale), Erosion.LEVEL_2.source(), Noises.min(Noises.mul(height, Noises.constant(-1.0f)), Noises.constant(-0.08f)), settings);
    }

    public static TerrainPopulator makeMountains3(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain settings, float verticalScale, boolean makeFancy, boolean legacyScaling) {
        Noise cell = Noises.worleyEdge(seed.next(), legacyScaling ? 400 : Math.round(600.0f * settings.horizontalScale), EdgeFunction.DISTANCE_2, DistanceFunction.EUCLIDEAN);
        cell = Noises.mul(cell, 1.2f);
        cell = Noises.clamp(cell, 0.0f, 1.0f);
        cell = Noises.warpPerlin(cell, seed.next(), 200, 2, 100.0f);
        Noise blur = Noises.perlin(seed.next(), 10, 1);
        blur = Noises.alpha(blur, 0.025f);
        Noise surface = Noises.perlinRidge(seed.next(), 125, 4);
        surface = Noises.alpha(surface, 0.37f);
        Noise mountains = Noises.clamp(cell, 0.0f, 1.0f);
        mountains = Noises.mul(mountains, blur);
        mountains = Noises.mul(mountains, surface);
        mountains = Noises.pow(mountains, 1.1f);
        Noise modulation = Noises.perlin(seed.next(), 50, 1);
        modulation = Noises.mul(modulation, 0.5f);
        Noise mask = Noises.perlin(seed.next(), 100, 1);
        mask = Noises.clamp(mask, 0.5f, 0.95f);
        mask = Noises.map(mask, 0.0f, 1.0f);
        Noise slope = Noises.constant(0.45f);
        Noise height = Noises.advancedTerrace(mountains, modulation, mask, slope, 0.2f, 0.45f, 24, 1);
        if (makeFancy) {
            height = Populators.makeFancy(seed, height);
        }
        height = Noises.cache2d(height);
        return TerrainPopulator.make(TerrainType.MOUNTAINS_3, ground, Noises.mul(height, (legacyScaling ? 0.645f : 1.185f) * verticalScale), Erosion.LEVEL_1.source(), Noises.min(Noises.mul(height, Noises.constant(-1.0f)), Noises.constant(-0.08f)), settings);
    }

    public static Noise makeFancy(@Deprecated Seed seed, Noise input) {
        Domain domain = Domains.direction(Noises.perlin(seed.next(), 10, 1), Noises.constant(2.0f));
        Noise erosion = Noises.erosion(input, seed.next(), 2, 0.65f, 128.0f, 0.15f, 3.1f, 0.8f, newearth.terrain.world.worldgen.noise.module.Erosion.BlendMode.CONSTANT);
        erosion = Noises.warp(erosion, domain);
        return erosion;
    }

    public static TerrainPopulator makeBorder(@Deprecated Seed seed, Noise ground, TerrainSettings.Terrain plainsSettings, TerrainSettings.Terrain steppeSettings, float verticalScale) {
        return Populators.makePlains(seed, ground, plainsSettings, steppeSettings, verticalScale);
    }
}

