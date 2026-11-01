package com.mjr.extraplanets.moons.Rhea.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderRhea extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return RheaBiomes.rhea;
	}

}
