package com.mjr.extraplanets.moons.Iapetus.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.world.BiomeGenBaseGC;

public class IapetusBiomes extends BiomeGenBaseGC {

	public static final Biome iapetus = new BiomeGenIapetus(new BiomeProperties("Iapetus").setBaseHeight(2.5F).setHeightVariation(0.4F).setRainfall(0.0F).setRainDisabled());

	protected IapetusBiomes(BiomeProperties properties) {
		super(properties, true);
	}
}
