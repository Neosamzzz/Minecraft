package com.mjr.extraplanets.moons.Deimos.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderDeimos extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return DeimosBiomes.deimos;
	}

}
