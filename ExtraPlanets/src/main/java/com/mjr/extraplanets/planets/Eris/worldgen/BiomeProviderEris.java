package com.mjr.extraplanets.planets.Eris.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderEris extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return ErisBiomes.eris;
	}

}
