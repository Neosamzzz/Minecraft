package com.mjr.extraplanets.planets.Pluto.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderPluto extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return PlutoBiomes.pluto;
	}

}
