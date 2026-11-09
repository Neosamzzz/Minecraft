package com.feed_the_beast.ftbquests.quest.task;

import com.feed_the_beast.ftbquests.FTBQuests;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.QuestData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.Loader;

import java.util.UUID;

/** Reads NewEarth's server-owned landing record without depending on its dimension ID. */
public class NewEarthArrivalTask extends Task
{
	private static boolean integrationFailed;

	public NewEarthArrivalTask(Quest quest) { super(quest); }

	@Override
	public TaskType getType() { return FTBQuestsTasks.NEWEARTH_ARRIVAL; }

	@Override
	public String getAltTitle() { return "完成抵达播放并正式降落"; }

	@Override
	public int autoSubmitOnPlayerTick() { return 20; }

	public static boolean hasCompletedArrival(NBTTagCompound saved, UUID player, boolean pending)
	{
		if (pending) return false;
		NBTTagList completed = saved.getTagList("Completed", 8);
		for (int i = 0; i < completed.tagCount(); i++)
		{
			if (player.toString().equals(completed.getStringTagAt(i))) return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static boolean landed(EntityPlayerMP player)
	{
		if (player.isSpectator() || integrationFailed || !Loader.isModLoaded("newearth")) return false;
		try
		{
			Class<?> destination = Class.forName("newearth.dimension.EndDimension");
			if (!Boolean.TRUE.equals(destination.getMethod("isDestination", World.class).invoke(null, player.world))) return false;
			Class<? extends WorldSavedData> type = (Class<? extends WorldSavedData>) Class.forName("newearth.dimension.ArrivalSession");
			WorldSavedData saved = player.getServerWorld().getPerWorldStorage().getOrLoadData(type, "newearth_arrival");
			return saved != null && hasCompletedArrival(saved.writeToNBT(new NBTTagCompound()), player.getUniqueID(),
					player.getEntityData().getBoolean("NewEarthArrivalPending") || player.getEntityData().hasKey("NewEarthArrivalHold"));
		}
		catch (ReflectiveOperationException e)
		{
			integrationFailed = true;
			FTBQuests.LOGGER.error("Unable to read NewEarth's authoritative arrival record", e);
			return false;
		}
	}

	@Override
	public TaskData createData(QuestData data)
	{
		return new BooleanTaskData<NewEarthArrivalTask>(this, data)
		{
			@Override
			public boolean canSubmit(EntityPlayerMP player) { return landed(player); }
		};
	}
}
