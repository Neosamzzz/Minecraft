package com.mjr.extraplanets.planets.Ceres.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderCeres extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return CeresBiomes.ceres;
	}

}
