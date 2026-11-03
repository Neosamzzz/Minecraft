package com.mjr.extraplanets.moons.Phobos.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderPhobos extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return PhobosBiomes.phobos;
	}

}
