package com.feed_the_beast.ftbquests.util;

import com.feed_the_beast.ftblib.FTBLibCommon;
import com.feed_the_beast.ftblib.FTBLibConfig;
import com.feed_the_beast.ftblib.events.universe.UniverseLoadedEvent;
import com.feed_the_beast.ftblib.events.universe.UniverseSavedEvent;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.TeamAction;
import com.feed_the_beast.ftblib.lib.data.TeamType;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftbquests.SharedTeamConfig;
import com.feed_the_beast.ftbquests.client.ClientQuestFile;
import com.feed_the_beast.ftbquests.command.CommandEditingMode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.util.UUID;

/** Uses actual Library teams and Quests data; no server, transport or GUI renderer. */
public class SharedTeamProbe
{
	private static int checks;

	private static class TestUniverse extends Universe
	{
		private final File directory;

		TestUniverse() throws Exception
		{
			super(null);
			directory = Files.createTempDirectory("ftb-shared-team-").toFile();
		}

		@Override
		public File getWorldDirectory()
		{
			return directory;
		}

		@Override
		public void clearCache()
		{
			// No fake player or world exists in this isolated probe.
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition) throw new AssertionError(message);
		checks++;
	}

	private static TestUniverse world() throws Exception
	{
		SharedTeamConfig.enabled = true;
		SharedTeamConfig.team_id = "";
		SharedTeamManager.start();
		TestUniverse universe = new TestUniverse();
		Constructor<FTBQuestsWorldData> constructor = FTBQuestsWorldData.class.getDeclaredConstructor(Universe.class);
		constructor.setAccessible(true);
		FTBQuestsWorldData.INSTANCE = constructor.newInstance(universe);
		return universe;
	}

	private static ForgeTeam team(TestUniverse universe, String id) throws Exception
	{
		ForgeTeam team = new ForgeTeam(universe, universe.generateTeamUID((short) 0), id, TeamType.PLAYER);
		team.owner = new ForgePlayer(universe, UUID.randomUUID(), id + "_owner");
		universe.addTeam(team);
		Constructor<ServerQuestData> constructor = ServerQuestData.class.getDeclaredConstructor(ForgeTeam.class);
		constructor.setAccessible(true);
		team.getData().add(constructor.newInstance(team));
		return team;
	}

	public static void main(String[] args) throws Exception
	{
		TestUniverse universe = world();
		ForgeTeam original = team(universe, "friends");
		ServerQuestData progress = ServerQuestData.get(original);
		progress.claimedTeamRewards.add(42);
		SharedTeamManager.onLoaded(new UniverseLoadedEvent.Finished(universe));
		check(SharedTeamManager.isActive(), "single existing team selected");
		check(new CommandEditingMode().checkPermission(null, null), "trusted shared-team players can toggle editing without OP");
		ClientQuestFile clientFile = new ClientQuestFile();
		clientFile.sharedTeam = true;
		check(!clientFile.canEdit(), "shared-team quest book stays in play mode until editing is enabled");
		clientFile.editingMode = true;
		check(clientFile.canEdit(), "shared-team client accepts enabled editing mode");
		clientFile.editingMode = false;
		check(!clientFile.canEdit(), "shared-team client can leave editing mode");
		check(FTBQuestsWorldData.INSTANCE.sharedTeamId.equals("friends"), "selected identity retained for save");
		NBTTagCompound saved = new NBTTagCompound();
		FTBQuestsWorldData.onUniverseSaved(new UniverseSavedEvent(universe, saved));
		check(saved.getCompoundTag("ftbquests").getString("SharedTeam").equals("friends"), "team identity written to universe NBT");
		ForgeTeam oldTeam = team(universe, "old_team");
		ForgePlayer first = new ForgePlayer(universe, UUID.randomUUID(), "first");
		ForgePlayer second = new ForgePlayer(universe, UUID.randomUUID(), "second");
		first.team = oldTeam;
		oldTeam.owner = first;
		universe.players.put(first.getId(), first);
		universe.players.put(second.getId(), second);
		SharedTeamManager.assignPlayer(first);
		SharedTeamManager.assignPlayer(second);
		check(first.team == original && second.team == original, "two players share the original team");
		check(original.isModerator(first) && original.isModerator(second), "both players can manage the team");
		check(ServerQuestData.get(first.team) == progress && progress.claimedTeamRewards.contains(42), "progress and claim records retained");
		check(universe.getTeam("old_team") == oldTeam, "last member migration does not delete old team");
		SharedTeamManager.assignPlayer(first);
		check(original.getMembers().size() == 2, "repeated assignment is idempotent");
		ResourceLocation leave = new ResourceLocation("ftblib", "leave");
		TeamAction action = new TeamAction("ftblib", "leave", null, 0)
		{
			public Type getType(ForgePlayer player, NBTTagCompound data) { return Type.ENABLED; }
			public void onAction(ForgePlayer player, NBTTagCompound data) { player.team.removeMember(player); }
		};
		FTBLibCommon.TEAM_GUI_ACTIONS.put(leave, action);
		SharedTeamManager.simplifyTeamActions();
		TeamAction wrapped = FTBLibCommon.TEAM_GUI_ACTIONS.get(leave);
		check(!wrapped.getType(first, new NBTTagCompound()).isVisible(), "leave hidden in shared-team GUI");
		wrapped.onAction(first, new NBTTagCompound());
		check(first.team == original, "direct team GUI action cannot leave");
		SharedTeamManager.stop();
		check(wrapped.getType(first, new NBTTagCompound()).isEnabled(), "original GUI action restored when inactive");

		universe = world();
		SharedTeamManager.onLoaded(new UniverseLoadedEvent.Finished(universe));
		check(SharedTeamManager.isActive() && universe.getTeams().size() == 1, "fresh world creates exactly one public team");
		check(FTBQuestsWorldData.INSTANCE.sharedTeamId.equals("ftb_public"), "fresh world uses stable public identity");
		SharedTeamManager.stop();

		universe = world();
		team(universe, "one");
		team(universe, "two");
		SharedTeamConfig.team_id = "two";
		SharedTeamManager.onLoaded(new UniverseLoadedEvent.Finished(universe));
		check(FTBQuestsWorldData.INSTANCE.sharedTeamId.equals("two"), "explicit team choice respected");
		SharedTeamManager.stop();

		universe = world();
		team(universe, "one");
		SharedTeamConfig.team_id = "missing";
		SharedTeamManager.onLoaded(new UniverseLoadedEvent.Finished(universe));
		check(!SharedTeamManager.isActive() && universe.getTeams().size() == 1, "missing configured team never replaced by empty team");
		SharedTeamManager.stop();

		universe = world();
		ServerQuestData.get(team(universe, "one")).claimedTeamRewards.add(1);
		ServerQuestData.get(team(universe, "two")).claimedTeamRewards.add(2);
		SharedTeamManager.onLoaded(new UniverseLoadedEvent.Finished(universe));
		check(!SharedTeamManager.isActive() && universe.getTeams().size() == 2, "multiple progressed teams left untouched");
		SharedTeamManager.stop();

		boolean previous = FTBLibConfig.teams.autocreate_sp;
		universe = world();
		check(!FTBLibConfig.teams.autocreate_sp, "Library personal-team autocreation suspended");
		SharedTeamManager.stop();
		check(FTBLibConfig.teams.autocreate_sp == previous, "Library settings restored on stop");
		System.out.println("SharedTeamProbe: " + checks + " checks passed");
	}
}
