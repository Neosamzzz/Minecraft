package com.mjr.extraplanets.moons.Ganymede.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderGanymede extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return GanymedeBiomes.ganymede;
	}

}
