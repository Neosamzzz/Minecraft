package com.mjr.extraplanets.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mjr.extraplanets.ExtraPlanets;
import com.mjr.extraplanets.network.PacketSimpleEP;
import com.mjr.extraplanets.network.PacketSimpleEP.EnumSimplePacket;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import micdoodle8.mods.galacticraft.api.recipe.ISchematicPage;
import micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry;
import micdoodle8.mods.galacticraft.core.dimension.SpaceRace;
import micdoodle8.mods.galacticraft.core.dimension.SpaceRaceManager;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;

public class SchematicsUtil {

	public static void lockSchematic(EntityPlayerMP player, ISchematicPage pageToLock) {
		GCPlayerStats stats = GCPlayerStats.get(player);

		if (stats.getUnlockedSchematics().contains(pageToLock)) {
			stats.getUnlockedSchematics().remove(pageToLock);
			Collections.sort(stats.getUnlockedSchematics());

			if (player != null && player.connection != null) {
				Integer[] iArray = new Integer[stats.getUnlockedSchematics().size()];

				for (int i = 0; i < iArray.length; i++) {
					ISchematicPage page = stats.getUnlockedSchematics().get(i);
					iArray[i] = page == null ? -2 : page.getPageID();
				}

				List<Object> objList = new ArrayList<Object>();
				objList.add(iArray);

				ExtraPlanets.packetPipeline.sendTo(new PacketSimpleEP(EnumSimplePacket.C_UPDATE_SCHEMATIC_LIST, GCCoreUtil.getDimensionID(player.world), objList), player);
			}
		}
	}

	public static void teamLockSchematic(EntityPlayerMP player, ItemStack stack) {
		SpaceRace race = SpaceRaceManager.getSpaceRaceFromPlayer(PlayerUtil.getName(player));
		if (race == null)
			return;
		MinecraftServer server = player.mcServer;
		for (String member : race.getPlayerNames()) {
			if (player.getName().equalsIgnoreCase(member))
				continue;

			EntityPlayerMP memberObj = PlayerUtil.getPlayerForUsernameVanilla(server, member);
			if (memberObj != null) {
				SchematicRegistry.unlockNewPage(memberObj, stack);
			} else {
				race.addNewSchematic(member, stack);
			}
		}
	}
}
