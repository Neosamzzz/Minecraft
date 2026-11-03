package com.mjr.extraplanets.command;

import java.util.ArrayList;
import java.util.List;

import com.mjr.extraplanets.ExtraPlanets;
import com.mjr.extraplanets.network.PacketSimpleEP;
import com.mjr.extraplanets.network.PacketSimpleEP.EnumSimplePacket;
import com.mjr.extraplanets.util.SchematicsUtil;
import com.mjr.mjrlegendslib.util.PlayerUtilties;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import micdoodle8.mods.galacticraft.api.recipe.ISchematicPage;
import micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry;
import micdoodle8.mods.galacticraft.core.util.EnumColor;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;

public class CommandRemoveSchematic extends CommandBase {

	@Override
	public String getUsage(ICommandSender var1) {
		return "/" + this.getName() + " <player> <schematic>";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 3;
	}

	@Override
	public String getName() {
		return "epRemoveSchematic";
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
				for (ItemStack stack : SchematicRegistry.schematicItems) {
					if ((stack.getItem().getRegistryName().toString() + ":" + stack.getItemDamage()).equalsIgnoreCase(args[1])) {
						final ISchematicPage page = SchematicRegistry.getMatchingRecipeForItemStack(stack);
						SchematicsUtil.lockSchematic(playerBase, page);
						// SpaceRaceManager.teamUnlockSchematic(playerBase, stack); TODO this
						ExtraPlanets.packetPipeline.sendTo(new PacketSimpleEP(EnumSimplePacket.C_REMOVE_SCHEMATIC, playerToAddFor.world.provider.getDimension(), new Object[] { page.getPageID() }), playerBase);

						// Output
						String name = stack.getUnlocalizedName() + ":" + stack.getItemDamage();
						String clientName = getClientSchematicName(stack, playerToAddFor);
						if (clientName != null)
							name = clientName;
						playerBase.sendMessage(new TextComponentString(EnumColor.AQUA + "Locked Schematic: " + name + EnumColor.AQUA + " for " + playerToAddFor.getName() + EnumColor.RED + " (Note: Doesnt update if part of Space Race!)"));
						playerToAddFor.sendMessage(new TextComponentString(EnumColor.AQUA + playerBase.getName() + " has taken away Schematic: " + name + EnumColor.RED + " (Note: Doesnt update if part of Space Race!)"));
					}
				}
			} catch (final Exception var6) {
				throw new CommandException(var6.getMessage(), new Object[0]);
			}
		}

	}

	@SideOnly(Side.CLIENT)
	public String getClientSchematicName(ItemStack stack, EntityPlayerMP playerToAddFor) {
		List<String> tooltips = stack.getTooltip(playerToAddFor, ITooltipFlag.TooltipFlags.NORMAL);
		if (tooltips.size() >= 2)
			return tooltips.get(1);
		return null;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		List<String> schematics = new ArrayList<>(100);

		for (ItemStack stack : SchematicRegistry.schematicItems)
			schematics.add(stack.getItem().getRegistryName().toString() + ":" + stack.getItemDamage());

		return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : (args.length == 2 ? schematics : null);
	}

	@Override
	public boolean isUsernameIndex(String[] par1ArrayOfStr, int par2) {
		return par2 == 0;
	}
}
