package com.mjr.extraplanets.moons.Phobos.worldgen;

import net.minecraft.world.biome.Biome;

import micdoodle8.mods.galacticraft.api.world.BiomeGenBaseGC;

public class PhobosBiomes extends BiomeGenBaseGC {

	public static final Biome phobos = new BiomeGenPhobos(new BiomeProperties("Phobos").setBaseHeight(2.5F).setHeightVariation(0.4F).setRainfall(0.0F).setRainDisabled());

	protected PhobosBiomes(BiomeProperties properties) {
		super(properties, true);
	}
}
