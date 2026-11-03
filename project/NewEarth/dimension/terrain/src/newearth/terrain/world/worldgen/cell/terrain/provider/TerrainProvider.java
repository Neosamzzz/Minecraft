/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.terrain.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import newearth.terrain.data.worldgen.preset.PresetNoiseData;
import newearth.terrain.data.worldgen.preset.PresetTerrainTypeNoise;
import newearth.terrain.data.worldgen.preset.settings.TerrainSettings;
import newearth.terrain.world.worldgen.cell.CellPopulator;
import newearth.terrain.world.worldgen.cell.heightmap.Levels;
import newearth.terrain.world.worldgen.cell.heightmap.RegionConfig;
import newearth.terrain.world.worldgen.cell.terrain.Populators;
import newearth.terrain.world.worldgen.cell.terrain.Terrain;
import newearth.terrain.world.worldgen.cell.terrain.TerrainType;
import newearth.terrain.world.worldgen.cell.terrain.populator.TerrainPopulator;
import newearth.terrain.world.worldgen.cell.terrain.populator.VolcanoPopulator;
import newearth.terrain.world.worldgen.noise.module.Noise;
import newearth.terrain.world.worldgen.noise.module.Noises;
import newearth.terrain.world.worldgen.util.Seed;

public class TerrainProvider {
    public static List<CellPopulator> generateTerrain(Seed seed, TerrainSettings settings, RegionConfig config, Levels levels, newearth.terrain.NoiseRegistry noiseLookup) {
        TerrainSettings.General general = settings.general;
        float verticalScale = general.globalVerticalScale;
        boolean fancyMountains = general.fancyMountains;
        boolean legacyMountainScaling = general.legacyMountainScaling;
        Seed terrainSeed = seed.offset(general.terrainSeedOffset);
        Noise ground = PresetNoiseData.getNoise(noiseLookup, PresetTerrainTypeNoise.GROUND);
        List<TerrainPopulator> mixable = new ArrayList<TerrainPopulator>();
        mixable.add(Populators.makeSteppe(terrainSeed, ground, settings.steppe));
        mixable.add(Populators.makePlains(terrainSeed, ground, settings.plains, verticalScale));
        mixable.add(Populators.makeDales(terrainSeed, ground, settings.dales));
        mixable.add(Populators.makeHills1(terrainSeed, ground, settings.hills, verticalScale));
        mixable.add(Populators.makeHills2(terrainSeed, ground, settings.hills, verticalScale));
        mixable.add(Populators.makeTorridonian(terrainSeed, ground, settings.torridonian));
        mixable.add(Populators.makePlateau(terrainSeed, ground, settings.plateau, verticalScale));
        mixable.add(Populators.makeBadlands(terrainSeed, ground, settings.badlands));
        mixable = mixable.stream().filter(populator -> populator.weight() > 0.0f).toList();
        ArrayList<CellPopulator> unmixable = new ArrayList<CellPopulator>();
        unmixable.add(Populators.makeBadlands(terrainSeed, ground, settings.badlands));
        unmixable.add(Populators.makeMountains(terrainSeed, ground, settings.mountains, 1.0f, verticalScale, fancyMountains, legacyMountainScaling));
        unmixable.add(Populators.makeMountains2(terrainSeed, ground, settings.mountains, verticalScale, fancyMountains, legacyMountainScaling));
        unmixable.add(Populators.makeMountains3(terrainSeed, ground, settings.mountains, verticalScale, fancyMountains, legacyMountainScaling));
        unmixable.add(new VolcanoPopulator(terrainSeed, config, levels, settings.volcano.weight));
        List<TerrainPopulator> mixed = TerrainProvider.combine(mixable, (t1, t2) -> TerrainProvider.combine(t1, t2, terrainSeed, levels, config.scale() / 2));
        ArrayList<CellPopulator> result = new ArrayList<CellPopulator>();
        result.addAll(mixed);
        result.addAll(unmixable);
        Collections.shuffle(result, new Random(terrainSeed.next()));
        return result;
    }

    private static TerrainPopulator combine(TerrainPopulator tp1, TerrainPopulator tp2, Seed seed, Levels levels, int scale) {
        Terrain type = TerrainType.registerComposite(tp1.type(), tp2.type());
        Noise selector = Noises.perlin(seed.next(), scale, 1);
        selector = Noises.warpPerlin(selector, seed.next(), scale / 2, 2, (float)scale / 2.0f);
        Noise height = Noises.blend(selector, tp1.height(), tp2.height(), 0.5f, 0.25f);
        height = Noises.max(height, Noises.zero());
        Noise erosion = Noises.blend(selector, tp1.erosion(), tp2.erosion(), 0.5f, 0.25f);
        Noise weirdness = Noises.threshold(selector, tp1.weirdness(), tp2.weirdness(), 0.5f);
        float weight = (tp1.weight() + tp2.weight()) / 2.0f;
        return new TerrainPopulator(type, Noises.constant(levels.ground), height, erosion, weirdness, weight);
    }

    private static <T> List<T> combine(List<T> input, BiFunction<T, T, T> operator) {
        int j;
        int length = input.size();
        for (int i = 1; i < input.size(); ++i) {
            length += input.size() - i;
        }
        ArrayList<T> result = new ArrayList<T>(length);
        for (j = 0; j < length; ++j) {
            result.add(null);
        }
        int k = input.size();
        for (j = 0; j < input.size(); ++j) {
            T t1 = input.get(j);
            result.set(j, t1);
            int l = j + 1;
            while (l < input.size()) {
                T t2 = input.get(l);
                T t3 = operator.apply(t1, t2);
                result.set(k, t3);
                ++l;
                ++k;
            }
        }
        return result;
    }
}

