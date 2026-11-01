package com.mjr.extraplanets.command;

import java.util.ArrayList;
import java.util.List;

import com.mjr.extraplanets.items.armor.ExtraPlanets_Armor;
import com.mjr.extraplanets.items.armor.bases.ElectricArmorBase;
import com.mjr.mjrlegendslib.util.PlayerUtilties;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.ItemHandlerHelper;

import micdoodle8.mods.galacticraft.core.util.EnumColor;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;

public class CommandGiveSpaceSuit extends CommandBase {

	private static List<String> numberList = new ArrayList<>(100);

	static {
		for (int i = 0; i < 5; i++)
			numberList.add("" + i);
	}

	@Override
	public String getUsage(ICommandSender var1) {
		return "/" + this.getName() + " <player> <0-4>";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 3;
	}

	@Override
	public String getName() {
		return "epGiveSpaceSuit";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		EntityPlayerMP playerBase = PlayerUtil.getPlayerBaseServerFromPlayerUsername(sender.getName(), true);
		if (playerBase == null) {
			return;
		}
		if (args.length == 2) {
			String username = args[0];
			EntityPlayerMP playerToAddFor;

			if (args[0].startsWith("@") || args[0].contains("-"))
				playerToAddFor = getPlayer(server, sender, args[0]);
			else
				playerToAddFor = PlayerUtilties.getPlayerFromUUID(server.getPlayerProfileCache().getGameProfileForUsername(username).getId());

			try {
				switch (Integer.parseInt(args[1])) {
					case 0:
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_0_SPACE_SUIT_HELMET, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_0_SPACE_SUIT_CHEST, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_0_SPACE_SUIT_LEGINGS, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_0_SPACE_SUIT_BOOTS, 1, ElectricArmorBase.DAMAGE_RANGE));
						break;
					case 1:
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_1_SPACE_SUIT_HELMET, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_1_SPACE_SUIT_CHEST, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_1_SPACE_SUIT_LEGINGS, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_1_SPACE_SUIT_BOOTS, 1, ElectricArmorBase.DAMAGE_RANGE));
						break;
					case 2:
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_2_SPACE_SUIT_HELMET, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_2_SPACE_SUIT_CHEST, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_2_SPACE_SUIT_LEGINGS, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_2_SPACE_SUIT_BOOTS, 1, ElectricArmorBase.DAMAGE_RANGE));
						break;
					case 3:
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_3_SPACE_SUIT_HELMET, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_3_SPACE_SUIT_CHEST, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_3_SPACE_SUIT_LEGINGS, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_3_SPACE_SUIT_BOOTS, 1, ElectricArmorBase.DAMAGE_RANGE));
						break;
					case 4:
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_4_SPACE_SUIT_HELMET, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_4_SPACE_SUIT_CHEST, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_4_SPACE_SUIT_LEGINGS, 1, ElectricArmorBase.DAMAGE_RANGE));
						ItemHandlerHelper.giveItemToPlayer(playerToAddFor, new ItemStack(ExtraPlanets_Armor.TIER_4_SPACE_SUIT_BOOTS, 1, ElectricArmorBase.DAMAGE_RANGE));
						break;
				}

				playerBase.sendMessage(new TextComponentString(EnumColor.AQUA + "Gave : " + playerToAddFor.getName() + " a set of SpaceSuit tier: " + args[1]));
				playerToAddFor.sendMessage(new TextComponentString(EnumColor.AQUA + playerBase.getName() + " give you a set of SpaceSuit tier:" + args[1]));
			} catch (final Exception var6) {
				throw new CommandException(var6.getMessage(), new Object[0]);
			}
		}

	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : (args.length == 2 ? numberList : null);
	}

	@Override
	public boolean isUsernameIndex(String[] par1ArrayOfStr, int par2) {
		return par2 == 0;
	}
}
