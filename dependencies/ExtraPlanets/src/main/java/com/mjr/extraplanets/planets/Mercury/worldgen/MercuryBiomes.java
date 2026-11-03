package com.mjr.extraplanets.planets.Mercury.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.world.BiomeGenBaseGC;

public class MercuryBiomes extends BiomeGenBaseGC {

	public static final Biome mercury = new BiomeGenMercury(new BiomeProperties("Mercury").setBaseHeight(2.5F).setHeightVariation(0.4F).setRainfall(0.0F).setRainDisabled());

	protected MercuryBiomes(BiomeProperties properties) {
		super(properties, true);
	}
}
