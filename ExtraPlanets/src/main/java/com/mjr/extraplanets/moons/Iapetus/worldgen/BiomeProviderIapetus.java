package com.mjr.extraplanets.moons.Iapetus.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.prefab.world.gen.BiomeProviderSpace;

public class BiomeProviderIapetus extends BiomeProviderSpace {

	@Override
	public Biome getBiome() {
		return IapetusBiomes.iapetus;
	}

}
