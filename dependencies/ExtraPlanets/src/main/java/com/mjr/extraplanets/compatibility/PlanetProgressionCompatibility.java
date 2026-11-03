package com.mjr.extraplanets.compatibility;

import com.mjr.planetprogression.api.research.ResearchHooksSP;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;

@SideOnly(Side.CLIENT)
public class PlanetProgressionCompatibility {

	@SideOnly(Side.CLIENT)
	@Optional.Method(modid = "planetprogression")
	public static boolean isResearched(EntityPlayerSP player, CelestialBody body) {
		return ResearchHooksSP.hasUnlockedCelestialBody(player, body);
	}
}
