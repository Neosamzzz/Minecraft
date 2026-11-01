package com.feed_the_beast.ftbquests.util;

import com.feed_the_beast.ftblib.FTBLibCommon;
import com.feed_the_beast.ftblib.FTBLibConfig;
import com.feed_the_beast.ftblib.command.team.CmdTeam;
import com.feed_the_beast.ftblib.events.player.ForgePlayerLoggedInEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamCreatedEvent;
import com.feed_the_beast.ftblib.events.universe.UniverseLoadedEvent;
import com.feed_the_beast.ftblib.lib.EnumTeamStatus;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.TeamAction;
import com.feed_the_beast.ftblib.lib.data.TeamType;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.net.MessageSyncData;
import com.feed_the_beast.ftbquests.FTBQuests;
import com.feed_the_beast.ftbquests.SharedTeamConfig;
import com.feed_the_beast.ftbquests.net.MessageChangedTeam;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FTBQuests.MOD_ID)
public class SharedTeamManager
{
	private static ForgeTeam publicTeam;
	private static boolean oldAutoSP, oldAutoMP, oldDisableTeams;

	public static boolean isActive()
	{
		return SharedTeamConfig.enabled && publicTeam != null && publicTeam.isValid();
	}

	public static void start()
	{
		publicTeam = null;
		oldAutoSP = FTBLibConfig.teams.autocreate_sp;
		oldAutoMP = FTBLibConfig.teams.autocreate_mp;
		oldDisableTeams = FTBLibConfig.teams.disable_teams;
		if (SharedTeamConfig.enabled)
		{
			// Library would otherwise create personal teams during first login.
			FTBLibConfig.teams.autocreate_sp = false;
			FTBLibConfig.teams.autocreate_mp = false;
			FTBLibConfig.teams.disable_teams = false;
		}
	}

	public static void stop()
	{
		publicTeam = null;
		FTBLibConfig.teams.autocreate_sp = oldAutoSP;
		FTBLibConfig.teams.autocreate_mp = oldAutoMP;
		FTBLibConfig.teams.disable_teams = oldDisableTeams;
	}

	@SubscribeEvent
	public static void onLoaded(UniverseLoadedEvent.Finished event)
	{
		if (!SharedTeamConfig.enabled)
		{
			return;
		}
		Universe universe = event.getUniverse();
		String id = SharedTeamConfig.team_id.trim();
		if (id.isEmpty())
		{
			id = FTBQuestsWorldData.INSTANCE.sharedTeamId;
		}
		if (!id.isEmpty())
		{
			final String selectedId = id;
			ForgeTeam team = universe.getTeams().stream().filter(t -> t.getId().equals(selectedId)).findFirst().orElse(null);
			if (team == null || !team.type.save || !team.isValid())
			{
				FTBQuests.LOGGER.error("Shared team '{}' is missing. No team data was changed; check ftbquests-shared-team.cfg.", id);
				stop();
				return;
			}
			publicTeam = team;
		}
		else
		{
			List<ForgeTeam> teams = new ArrayList<>();
			List<ForgeTeam> progressed = new ArrayList<>();
			for (ForgeTeam team : universe.getTeams())
			{
				if (team.isValid() && team.type.save)
				{
					teams.add(team);
					ServerQuestData data = ServerQuestData.get(team);
					if (team.getDataFile("ftbquests").isFile() || data.taskData.values().stream().anyMatch(t -> t.isStarted()) || !data.claimedPlayerRewards.isEmpty() || !data.claimedTeamRewards.isEmpty())
					{
						progressed.add(team);
					}
				}
			}
			if (progressed.size() > 1)
			{
				FTBQuests.LOGGER.error("Multiple teams have quest data: {}. Set team_id in ftbquests-shared-team.cfg; progress is not merged automatically.", progressed);
				stop();
				return;
			}
			if (progressed.size() == 1 || teams.size() == 1)
			{
				publicTeam = progressed.isEmpty() ? teams.get(0) : progressed.get(0);
			}
			else
			{
				publicTeam = teams.stream().filter(t -> t.getId().equals("ftb_public")).findFirst().orElse(null);
				if (publicTeam == null)
				{
					publicTeam = new ForgeTeam(universe, universe.generateTeamUID((short) 0), "ftb_public", TeamType.SERVER);
					publicTeam.setTitle("Public Team");
					universe.addTeam(publicTeam);
					new ForgeTeamCreatedEvent(publicTeam).post();
				}
			}
		}
		publicTeam.markDirty();
		FTBQuestsWorldData.INSTANCE.sharedTeamId = publicTeam.getId();
		universe.markDirty();
		FTBQuests.LOGGER.info("Shared quest team: {}", publicTeam.getId());
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLogin(ForgePlayerLoggedInEvent event)
	{
		ForgePlayer player = event.getPlayer();
		if (!isActive() || player.isFake())
		{
			return;
		}
		assignPlayer(player);
		new MessageSyncData(true, player.getPlayer(), player).sendTo(player.getPlayer());
		new MessageChangedTeam(player.getId(), publicTeam.getUID()).sendToAll();
	}

	static void assignPlayer(ForgePlayer player)
	{
		if (!publicTeam.isMember(player))
		{
			// Do not removeMember(): it deletes the last member's personal team and quest data.
			ForgeTeam oldTeam = player.team;
			oldTeam.players.remove(player);
			oldTeam.markDirty();
			player.team = publicTeam;
			player.markDirty();
		}
		if (!publicTeam.isOwner(player))
		{
			publicTeam.setStatus(player, EnumTeamStatus.MOD);
		}
		publicTeam.universe.clearCache();
		publicTeam.markDirty();
	}

	@SubscribeEvent
	public static void onCommand(CommandEvent event)
	{
		if (isActive() && event.getCommand() instanceof CmdTeam && event.getParameters().length > 0)
		{
			String command = event.getParameters()[0];
			if (!command.equals("info") && !command.equals("get") && !command.equals("list") && !command.equals("settings") && !command.equals("settings_for") && !command.equals("help"))
			{
				event.setCanceled(true);
				event.getSender().sendMessage(new TextComponentTranslation("ftbquests.shared_team.fixed"));
			}
		}
	}

	public static void simplifyTeamActions()
	{
		for (String name : new String[]{"members", "allies", "moderators", "enemies", "leave", "transfer_ownership"})
		{
			ResourceLocation id = new ResourceLocation("ftblib", name);
			TeamAction original = FTBLibCommon.TEAM_GUI_ACTIONS.get(id);
			if (original != null)
			{
				TeamAction wrapped = new TeamAction("ftblib", name, original.getIcon(), original.getOrder())
				{
					@Override
					public Type getType(ForgePlayer player, NBTTagCompound data)
					{
						return isActive() ? Type.INVISIBLE : original.getType(player, data);
					}

					@Override
					public void onAction(ForgePlayer player, NBTTagCompound data)
					{
						if (!isActive())
						{
							original.onAction(player, data);
						}
					}
				}.setTitle(original.getTitle());
				if (original.getRequireConfirm())
				{
					wrapped.setRequiresConfirm();
				}
				FTBLibCommon.TEAM_GUI_ACTIONS.put(id, wrapped);
			}
		}
	}
}
