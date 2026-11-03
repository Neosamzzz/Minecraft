package com.feed_the_beast.ftbquests.command;

import com.feed_the_beast.ftbquests.util.SharedTeamManager;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

/**
 * @author LatvianModder
 */
public abstract class CommandFTBQuestsBase extends CommandBase
{
	@Override
	public String getUsage(ICommandSender sender)
	{
		return "commands.ftbquests." + getName() + ".usage";
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return SharedTeamManager.isActive() ? 0 : 2;
	}
}
