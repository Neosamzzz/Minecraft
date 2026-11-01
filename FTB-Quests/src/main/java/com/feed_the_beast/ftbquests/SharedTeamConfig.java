package com.feed_the_beast.ftbquests;

import net.minecraftforge.common.config.Config;

@Config(modid = FTBQuests.MOD_ID, name = "ftbquests-shared-team")
public class SharedTeamConfig
{
	@Config.RequiresMcRestart
	@Config.Comment("Use one shared team and allow everyone to manage quests. For trusted friends only.")
	public static boolean enabled = true;

	@Config.RequiresMcRestart
	@Config.Comment("Existing team ID to reuse. Empty selects the only team with progress, or creates a public team for a fresh world.")
	public static String team_id = "";
}
