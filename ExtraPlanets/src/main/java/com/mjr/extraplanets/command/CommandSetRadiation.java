package com.mjr.extraplanets.command;

import java.util.ArrayList;
import java.util.List;

import com.mjr.extraplanets.handlers.capabilities.CapabilityStatsHandler;
import com.mjr.extraplanets.handlers.capabilities.IStatsCapability;
import com.mjr.mjrlegendslib.util.PlayerUtilties;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import micdoodle8.mods.galacticraft.core.util.EnumColor;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;

public class CommandSetRadiation extends CommandBase {

	private static List<String> numberList = new ArrayList<>(100);

	static {
		for (int i = 0; i < 101; i++)
			numberList.add("" + i);
	}

	@Override
	public String getUsage(ICommandSender var1) {
		return "/" + this.getName() + " <player> <0-100>";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 3;
	}

	@Override
	public String getName() {
		return "epSetRadiation";
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
				IStatsCapability stats = null;
				if (playerToAddFor != null) {
					stats = playerToAddFor.getCapability(CapabilityStatsHandler.EP_STATS_CAPABILITY, null);
				}
				Double amount = Double.parseDouble(args[1]);
				if (amount > 100)
					amount = 100.0;
				else if (amount < 0)
					amount = 0.0;
				stats.setRadiationLevel(amount);
				playerBase.sendMessage(new TextComponentString(EnumColor.AQUA + "Updated Radiation Level for: " + playerToAddFor.getName() + " to " + args[1] + "/100"));
				playerToAddFor.sendMessage(new TextComponentString(EnumColor.AQUA + playerBase.getName() + " has updated your Radiation Level to " + args[1] + "/100"));
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
