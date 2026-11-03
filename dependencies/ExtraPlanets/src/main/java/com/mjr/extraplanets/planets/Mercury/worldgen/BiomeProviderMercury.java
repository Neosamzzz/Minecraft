package com.mjr.extraplanets.planets.Mercury.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderMercury extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return MercuryBiomes.mercury;
	}

}
