package com.mjr.extraplanets.moons.Deimos.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.world.BiomeGenBaseGC;

public class DeimosBiomes extends BiomeGenBaseGC {

	public static final Biome deimos = new BiomeGenDeimos(new BiomeProperties("Deimos").setBaseHeight(2.5F).setHeightVariation(0.4F).setRainfall(0.0F).setRainDisabled());

	protected DeimosBiomes(BiomeProperties properties) {
		super(properties, true);
	}
}
