package com.feed_the_beast.ftbquests.gui.tree;

import com.feed_the_beast.ftblib.lib.gui.GuiHelper;
import com.feed_the_beast.ftblib.lib.gui.GuiIcons;
import com.feed_the_beast.ftblib.lib.gui.Panel;
import com.feed_the_beast.ftblib.lib.util.misc.MouseButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class ButtonSharedTeamEditing extends ButtonTab
{
	public ButtonSharedTeamEditing(Panel panel)
	{
		super(panel, I18n.format(((GuiQuestTree) panel.getGui()).file.canEdit() ? "ftbquests.shared_team.editing.off" : "ftbquests.shared_team.editing.on"), GuiIcons.ART);
	}

	@Override
	public void onClicked(MouseButton button)
	{
		GuiHelper.playClickSound();
		Minecraft.getMinecraft().player.sendChatMessage("/ftbquests editing_mode");
	}
}
