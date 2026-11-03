package com.mjr.extraplanets.moons.Titania.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderTitania extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return TitaniaBiomes.titania;
	}

}
